package com.sojourners.chess.openbook.reader;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Read-only indexed reader for Binghe's unsigned-key SQLite OBK variant.
 * No JDBC or native library is required. Keys, including serial-type-7 NaNs,
 * are compared as raw unsigned 64-bit values, never as floating-point numbers.
 *
 * <p>The source must be a closed, stable database. Nonempty WAL/journal sidecars
 * and observable source changes are rejected. This is not a general SQLite
 * connection and does not participate in SQLite's live-writer locking protocol.
 * Each record is limited to 64 MiB, b-tree depth to 64, and the page cache to
 * 256 pages (1 MiB for the usual 4096-byte pages). Database schemas must expose
 * the ten bhobk columns, an INTEGER PRIMARY KEY id, and an ordinary ascending
 * index on vkey. Numeric move/statistic NULL fields are returned as zero.</p>
 */
public final class ObkReader implements AutoCloseable {
    private static final int CACHE_PAGES = 256;
    private static final int MAX_DEPTH = 64;
    private static final int MAX_PAYLOAD = 64 * 1024 * 1024;
    private static final byte[] MAGIC = "SQLite format 3\0".getBytes(StandardCharsets.US_ASCII);
    private static final String[] COLUMNS = {
            "id", "vkey", "vmove", "vscore", "vwin", "vdraw", "vlost", "vvalid", "vmemo", "vindex"
    };
    private static final String NAME = "(?:[a-zA-Z_][a-zA-Z_0-9]*|\"[^\"]+\"|`[^`]+`|\\[[^\\]]+\\])";
    private static final Pattern INDEX = Pattern.compile(
            "(?is)^\\s*CREATE\\s+(?:UNIQUE\\s+)?INDEX\\s+(?:IF\\s+NOT\\s+EXISTS\\s+)?"
                    + NAME + "\\s+ON\\s+" + NAME + "\\s*\\(\\s*(" + NAME
                    + ")(?:\\s+ASC)?\\s*\\)\\s*;?\\s*$");
    private final Path path;
    private final FileChannel channel;
    private final BasicFileAttributes initialAttributes;
    private final byte[] initialHeader;
    private final int pageSize, usableSize;
    private final long pageCount;
    private final Charset textCharset;
    private final Map<Long, byte[]> cache = new LinkedHashMap<Long, byte[]>(32, 0.75f, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<Long, byte[]> entry) {
            return size() > CACHE_PAGES;
        }
    };
    private final Map<String, Integer> columnNumbers = new HashMap<String, Integer>();
    private long tableRoot, indexRoot;
    private boolean closed;

    public static ObkReader open(Path path) throws IOException { return new ObkReader(path); }

    public ObkReader(Path source) throws IOException {
        path = source.toAbsolutePath().normalize();
        checkSidecars();
        initialAttributes = Files.readAttributes(path, BasicFileAttributes.class);
        if (!initialAttributes.isRegularFile()) throw error("source is not a regular file");
        channel = FileChannel.open(path, StandardOpenOption.READ);
        try {
            initialHeader = readAt(0, 100);
            if (!Arrays.equals(MAGIC, Arrays.copyOf(initialHeader, MAGIC.length))) {
                throw error("not a SQLite-format OBK database");
            }
            int ps = u16(initialHeader, 16);
            pageSize = ps == 1 ? 65536 : ps;
            if (pageSize < 512 || pageSize > 65536 || (pageSize & (pageSize - 1)) != 0) {
                throw error("invalid page size");
            }
            usableSize = pageSize - (initialHeader[20] & 255);
            int readVersion = initialHeader[19] & 255;
            if (usableSize < 480 || readVersion < 1 || readVersion > 2
                    || initialHeader[21] != 64 || initialHeader[22] != 32 || initialHeader[23] != 32) {
                throw error("unsupported SQLite header");
            }
            long length = channel.size();
            if (length < pageSize || length % pageSize != 0) throw error("truncated database");
            pageCount = length / pageSize;
            if (pageCount > 0xfffffffeL) throw error("invalid page count");
            long encoding = u32(initialHeader, 56);
            if (encoding == 1) textCharset = StandardCharsets.UTF_8;
            else if (encoding == 2) textCharset = StandardCharsets.UTF_16LE;
            else if (encoding == 3) textCharset = StandardCharsets.UTF_16BE;
            else throw error("unsupported text encoding: " + encoding);
            List<Value[]> schema = new ArrayList<Value[]>();
            scanSchema(1, 0, schema);
            for (Value[] row : schema) {
                if (row.length < 5) throw error("invalid sqlite_schema record");
                String type = text(row[0]), name = text(row[1]), table = text(row[2]);
                if ("table".equalsIgnoreCase(type) && "bhobk".equalsIgnoreCase(name)) {
                    if (tableRoot != 0) throw error("duplicate bhobk table");
                    tableRoot = integer(row[3]);
                    parseColumns(text(row[4]));
                }
                if ("index".equalsIgnoreCase(type) && "bhobk".equalsIgnoreCase(table)) {
                    Matcher m = INDEX.matcher(text(row[4]));
                    if (m.matches() && "vkey".equalsIgnoreCase(unquote(m.group(1)))) {
                        indexRoot = integer(row[3]);
                    }
                }
            }
            if (tableRoot == 0 || indexRoot == 0) throw error("bhobk or a plain ascending vkey index is missing");
            checkPageNumber(tableRoot);
            checkPageNumber(indexRoot);
            int tableType = page(tableRoot).type;
            int indexType = page(indexRoot).type;
            if (tableType != 5 && tableType != 13) throw error("unsupported bhobk table b-tree");
            if (indexType != 2 && indexType != 10) throw error("unsupported vkey index b-tree");
            checkUnchanged();
        } catch (IOException | RuntimeException failure) {
            channel.close();
            throw failure;
        }
    }

    /** Returns all rows for the exact key, including vvalid != 1 rows. */
    public synchronized List<ObkEntry> find(long key) throws IOException {
        if (closed) throw error("reader is closed");
        checkUnchanged();
        List<Long> ids = new ArrayList<Long>();
        findIndex(indexRoot, key, ids, 0);
        List<ObkEntry> entries = new ArrayList<ObkEntry>(ids.size());
        for (long id : ids) {
            Value[] row = findTable(tableRoot, id, 0);
            if (row == null) throw error("index references missing rowid " + id);
            if (key(row[column("vkey")]) != key) throw error("index/table vkey mismatch at rowid " + id);
            Value memo = row[column("vmemo")];
            if (memo.serial != 0 && memo.serial < 12) throw error("invalid memo type at rowid " + id);
            entries.add(new ObkEntry(id, key, intColumn(row, "vmove"), intColumn(row, "vscore"),
                    intColumn(row, "vwin"), intColumn(row, "vdraw"), intColumn(row, "vlost"),
                    intColumn(row, "vvalid"), memo.serial == 0 ? null : memo.bytes,
                    intColumn(row, "vindex"), (memo.serial & 1) != 0 ? textCharset : StandardCharsets.UTF_8));
        }
        checkUnchanged();
        return entries;
    }

    @Override public synchronized void close() throws IOException {
        closed = true;
        cache.clear();
        channel.close();
    }

    private void findIndex(long pg, long target, List<Long> ids, int depth) throws IOException {
        depth(depth);
        Page page = page(pg);
        if (page.type != 2 && page.type != 10) throw error("unexpected index page type");
        int lo = 0, hi = page.count;
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            Value[] entry = indexEntry(page, mid);
            if (Long.compareUnsigned(key(entry[0]), target) < 0) lo = mid + 1;
            else hi = mid;
        }
        if (page.type == 2) findIndex(page.child(lo), target, ids, depth + 1);
        for (int i = lo; i < page.count; i++) {
            Value[] entry = indexEntry(page, i);
            if (key(entry[0]) != target) break;
            ids.add(integer(entry[1]));
            if (page.type == 2) findIndex(page.child(i + 1), target, ids, depth + 1);
        }
    }

    private Value[] indexEntry(Page page, int i) throws IOException {
        Value[] values = record(payload(page, page.cell(i) + (page.type == 2 ? 4 : 0), true).bytes);
        if (values.length != 2) throw error("vkey index must contain exactly key and rowid");
        return values;
    }

    private Value[] findTable(long pg, long rowid, int depth) throws IOException {
        depth(depth);
        Page page = page(pg);
        if (page.type != 5 && page.type != 13) throw error("unexpected table page type");
        int lo = 0, hi = page.count;
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            int cell = page.cell(mid);
            Cursor c = new Cursor(page.bytes, cell + (page.type == 5 ? 4 : 0), usableSize);
            if (page.type == 13) c.varint(); // payload size
            long id = c.varint();
            if (id < rowid) lo = mid + 1;
            else hi = mid;
        }
        if (page.type == 5) return findTable(page.child(lo), rowid, depth + 1);
        if (lo == page.count) return null;
        Payload payload = payload(page, page.cell(lo), false);
        if (payload.rowid != rowid) return null;
        Value[] result = record(payload.bytes);
        if (result.length < columnNumbers.size()) throw error("bhobk row has missing fields");
        return result;
    }

    private void scanSchema(long pg, int depth, List<Value[]> rows) throws IOException {
        depth(depth);
        Page page = page(pg);
        if (page.type != 5 && page.type != 13) throw error("unexpected schema page type");
        for (int i = 0; i < page.count; i++) {
            if (page.type == 5) scanSchema(page.child(i), depth + 1, rows);
            else {
                if (rows.size() >= 100000) throw error("schema is too large");
                rows.add(record(payload(page, page.cell(i), false).bytes));
            }
        }
        if (page.type == 5) scanSchema(page.child(page.count), depth + 1, rows);
    }

    private Payload payload(Page page, int offset, boolean index) throws IOException {
        Cursor c = new Cursor(page.bytes, offset, usableSize);
        long size = c.varint();
        if (size < 0 || size > MAX_PAYLOAD) throw error("record exceeds 64 MiB limit");
        long rowid = index ? 0 : c.varint();
        int n = (int) size;
        int maxLocal = index ? ((usableSize - 12) * 64 / 255) - 23 : usableSize - 35;
        int minLocal = ((usableSize - 12) * 32 / 255) - 23;
        int local = n;
        if (n > maxLocal) {
            local = minLocal + (n - minLocal) % (usableSize - 4);
            if (local > maxLocal) local = minLocal;
        }
        c.require(local + (local < n ? 4 : 0));
        byte[] data = new byte[n];
        System.arraycopy(page.bytes, c.offset, data, 0, local);
        if (local < n) {
            long overflow = u32(page.bytes, c.offset + local);
            Set<Long> visited = new HashSet<Long>();
            visited.add(page.number);
            int written = local;
            while (written < n) {
                if (!visited.add(overflow)) throw error("overflow page cycle");
                byte[] next = readPage(overflow);
                int chunk = Math.min(n - written, usableSize - 4);
                System.arraycopy(next, 4, data, written, chunk);
                written += chunk;
                overflow = u32(next, 0);
            }
            if (overflow != 0) throw error("overflow chain exceeds record size");
        }
        return new Payload(rowid, data);
    }

    private Value[] record(byte[] bytes) throws IOException {
        Cursor header = new Cursor(bytes, 0, bytes.length);
        long headerSize = header.varint();
        if (headerSize < header.offset || headerSize > bytes.length) throw error("invalid record header size");
        header.limit = (int) headerSize;
        List<Long> types = new ArrayList<Long>();
        while (header.offset < header.limit) {
            if (types.size() >= 32768) throw error("too many record fields");
            long type = header.varint();
            if (type < 0 || type == 10 || type == 11) throw error("invalid record serial type");
            types.add(type);
        }
        int offset = (int) headerSize;
        Value[] values = new Value[types.size()];
        for (int i = 0; i < values.length; i++) {
            long type = types.get(i);
            long length;
            if (type == 0 || type == 8 || type == 9) length = 0;
            else if (type < 5) length = type;
            else if (type == 5) length = 6;
            else if (type < 8) length = 8;
            else length = (type - 12) / 2;
            if (length > bytes.length - offset) throw error("truncated record field");
            byte[] data = Arrays.copyOfRange(bytes, offset, offset + (int) length);
            values[i] = new Value(type, data);
            offset += (int) length;
        }
        if (offset != bytes.length) throw error("record has trailing bytes");
        return values;
    }

    private long key(Value value) throws IOException {
        if (value.serial == 7) return rawInteger(value.bytes); // includes every NaN payload bit
        if (value.serial >= 1 && value.serial <= 6 || value.serial == 8 || value.serial == 9) {
            long integer = integer(value);
            if (integer < 0) throw error("signed integer vkey is not a Binghe unsigned-key OBK");
            return integer;
        }
        throw error("unsupported vkey storage type " + value.serial);
    }

    private long integer(Value value) throws IOException {
        if (value.serial == 8) return 0;
        if (value.serial == 9) return 1;
        if (value.serial < 1 || value.serial > 6) throw error("expected integer field");
        long number = rawInteger(value.bytes);
        int shift = 64 - value.bytes.length * 8;
        return (number << shift) >> shift;
    }

    private static long rawInteger(byte[] bytes) {
        long value = 0;
        for (byte b : bytes) value = (value << 8) | (b & 255);
        return value;
    }

    private int intColumn(Value[] row, String name) throws IOException {
        Value field = row[column(name)];
        // OBK producers commonly leave optional statistics/vindex NULL.
        long value = field.serial == 0 ? 0 : integer(field);
        if (value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) throw error(name + " exceeds signed 32-bit range");
        return (int) value;
    }

    private int column(String name) { return columnNumbers.get(name); }

    private String text(Value value) throws IOException {
        if (value.serial == 0) return "";
        if (value.serial < 13 || (value.serial & 1) == 0) throw error("expected schema text");
        return new String(value.bytes, textCharset);
    }

    private void parseColumns(String sql) throws IOException {
        int start = sql.indexOf('('), end = sql.lastIndexOf(')');
        if (start < 0 || end < start || sql.substring(end + 1).toUpperCase(Locale.ROOT).contains("WITHOUT")) {
            throw error("unsupported bhobk schema");
        }
        List<String> definitions = splitDefinitions(sql.substring(start + 1, end));
        Pattern firstName = Pattern.compile("^\\s*(" + NAME + ")(.*)$", Pattern.DOTALL);
        int number = 0;
        for (String definition : definitions) {
            Matcher m = firstName.matcher(definition);
            if (!m.matches()) throw error("invalid bhobk column declaration");
            String name = unquote(m.group(1)).toLowerCase(Locale.ROOT);
            if ("constraint".equals(name) || "primary".equals(name) || "unique".equals(name)
                    || "check".equals(name) || "foreign".equals(name)) continue;
            if (columnNumbers.put(name, number++) != null) throw error("duplicate bhobk column");
            if ("id".equals(name) && !m.group(2).matches("(?is)\\s+INTEGER\\s+PRIMARY\\s+KEY(?:\\s+AUTOINCREMENT)?\\s*")) {
                throw error("bhobk.id must be an INTEGER PRIMARY KEY rowid alias");
            }
        }
        for (String required : COLUMNS) if (!columnNumbers.containsKey(required)) throw error("missing bhobk column " + required);
    }

    private List<String> splitDefinitions(String body) throws IOException {
        List<String> parts = new ArrayList<String>();
        int start = 0, depth = 0;
        char quote = 0;
        for (int i = 0; i < body.length(); i++) {
            char ch = body.charAt(i);
            if (quote != 0) {
                if (ch == quote) {
                    if (quote != ']' && i + 1 < body.length() && body.charAt(i + 1) == ch) i++;
                    else quote = 0;
                }
            } else if (ch == '[') quote = ']';
            else if (ch == '\'' || ch == '"' || ch == '`') quote = ch;
            else if (ch == '(') depth++;
            else if (ch == ')') depth--;
            else if (ch == ',' && depth == 0) { parts.add(body.substring(start, i)); start = i + 1; }
            if (depth < 0) throw error("invalid schema parentheses");
        }
        if (quote != 0 || depth != 0) throw error("invalid schema quoting");
        parts.add(body.substring(start));
        return parts;
    }

    private static String unquote(String name) {
        char first = name.charAt(0);
        return first == '[' || first == '"' || first == '`' ? name.substring(1, name.length() - 1) : name;
    }

    private Page page(long number) throws IOException { return new Page(number, readPage(number)); }

    private byte[] readPage(long number) throws IOException {
        checkPageNumber(number);
        byte[] bytes = cache.get(number);
        if (bytes == null) {
            bytes = readAt((number - 1) * pageSize, pageSize);
            cache.put(number, bytes);
        }
        return bytes;
    }

    private void checkPageNumber(long number) throws IOException {
        if (number < 1 || number > pageCount) throw error("page number out of range: " + number);
    }

    private byte[] readAt(long position, int length) throws IOException {
        byte[] bytes = new byte[length];
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        while (buffer.hasRemaining()) {
            int n = channel.read(buffer, position + buffer.position());
            if (n < 0) throw new EOFException("Truncated OBK: " + path);
            if (n == 0) throw error("could not read database bytes");
        }
        return bytes;
    }

    private void checkSidecars() throws IOException {
        for (String suffix : new String[] {"-wal", "-journal"}) {
            Path sidecar = path.resolveSibling(path.getFileName().toString() + suffix);
            if (Files.exists(sidecar) && Files.size(sidecar) != 0) {
                throw error("nonempty " + suffix + " sidecar; close/checkpoint the writing application first");
            }
        }
    }

    private void checkUnchanged() throws IOException {
        checkSidecars();
        BasicFileAttributes current = Files.readAttributes(path, BasicFileAttributes.class);
        Object originalKey = initialAttributes.fileKey(), currentKey = current.fileKey();
        if (current.size() != initialAttributes.size()
                || !current.lastModifiedTime().equals(initialAttributes.lastModifiedTime())
                || originalKey != null && !originalKey.equals(currentKey)
                || !Arrays.equals(initialHeader, readAt(0, 100))) {
            throw error("source database changed; close and reopen the reader");
        }
    }

    private void depth(int value) throws IOException {
        if (value >= MAX_DEPTH) throw error("b-tree depth exceeds limit (possible page cycle)");
    }

    private IOException error(String message) { return new IOException("OBK " + path + ": " + message); }
    private static int u16(byte[] bytes, int at) { return ((bytes[at] & 255) << 8) | (bytes[at + 1] & 255); }
    private static long u32(byte[] bytes, int at) {
        return ((long) u16(bytes, at) << 16) | u16(bytes, at + 2);
    }

    private final class Page {
        final long number;
        final byte[] bytes;
        final int type, count, pointers;
        Page(long number, byte[] bytes) throws IOException {
            this.number = number;
            this.bytes = bytes;
            int offset = number == 1 ? 100 : 0;
            type = bytes[offset] & 255;
            if (type != 2 && type != 5 && type != 10 && type != 13) throw error("invalid b-tree page type " + type);
            count = u16(bytes, offset + 3);
            pointers = offset + (type == 2 || type == 5 ? 12 : 8);
            if (count > (usableSize - pointers) / 2) throw error("invalid cell pointer count");
        }
        int cell(int i) throws IOException {
            if (i < 0 || i >= count) throw error("cell number out of range");
            int offset = u16(bytes, pointers + i * 2);
            if (offset < pointers + count * 2 || offset >= usableSize) throw error("cell offset out of range");
            return offset;
        }
        long child(int i) throws IOException {
            if (type != 2 && type != 5 || i < 0 || i > count) throw error("invalid child pointer");
            if (i == count) return u32(bytes, (number == 1 ? 100 : 0) + 8);
            int offset = cell(i);
            if (offset > usableSize - 4) throw error("truncated child pointer");
            return u32(bytes, offset);
        }
    }

    private final class Cursor {
        final byte[] bytes;
        int offset, limit;
        Cursor(byte[] bytes, int offset, int limit) throws IOException {
            this.bytes = bytes; this.offset = offset; this.limit = limit;
            require(0);
        }
        void require(int n) throws IOException {
            if (n < 0 || offset < 0 || offset > limit - n || limit > bytes.length) throw error("truncated cell/record");
        }
        long varint() throws IOException {
            long value = 0;
            for (int i = 0; i < 9; i++) {
                require(1);
                int b = bytes[offset++] & 255;
                if (i == 8) return (value << 8) | b;
                value = (value << 7) | (b & 127);
                if ((b & 128) == 0) return value;
            }
            throw error("invalid varint");
        }
    }

    private static final class Payload {
        final long rowid;
        final byte[] bytes;
        Payload(long rowid, byte[] bytes) { this.rowid = rowid; this.bytes = bytes; }
    }
    private static final class Value {
        final long serial;
        final byte[] bytes;
        Value(long serial, byte[] bytes) { this.serial = serial; this.bytes = bytes; }
    }
}
