package com.sojourners.chess.manual;

import com.sojourners.chess.model.ManualRecord;
import com.sojourners.chess.util.StringUtils;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * XQF (象棋演播室) 棋谱读写实现类
 */
public class XqfChessManualImpl implements ChessManualService {

    // 32个棋子的FEN表示顺序: 红方车马相士帅士相马车炮炮兵兵兵兵兵 + 黑方车马象士将士象马车炮炮卒卒卒卒卒
    private static final String FEN_PIECES = "RNBAKABNRCCPPPPPrnbakabnrccppppp";

    private static final String DEFAULT_FEN = "rnbakabnr/9/1c5c1/p1p1p1p1p/9/9/P1P1P1P1P/1C5C1/9/RNBAKABNR w - - 0 1";

    private static final int HEADER_SIZE = 1024;
    private static final int COMMENT_LENGTH_KEY = 767;

    // GBK编码用于解析中文文本
    private static final Charset GBK_CHARSET = Charset.forName("GBK");

    @Override
    public ChessManual openChessManual(File file) {
        try (FileInputStream fis = new FileInputStream(file);
             BufferedInputStream bis = new BufferedInputStream(fis)) {

            byte[] buffer = new byte[(int) file.length()];
            int read = bis.read(buffer);
            if (read != buffer.length) {
                throw new IOException("Failed to read complete file");
            }

            return parseXqfFile(buffer);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 解析XQF文件内容
     */
    private ChessManual parseXqfFile(byte[] buffer) {
        // 验证文件头标记 'XQ'
        if (buffer.length < 2 || buffer[0] != 'X' || buffer[1] != 'Q') {
            throw new IllegalArgumentException("Invalid XQF file header");
        }

        ChessManual cm = new ChessManual();

        // 解析文件头
        XqfHeader header = parseHeader(buffer);

        // 设置棋局基本信息
        cm.setName(header.title);
        cm.setDate(header.matchTime);
        cm.setCity(header.matchAddr);
        cm.setRed(header.redPlayer);
        cm.setBlack(header.blkPlayer);

        // 计算密钥
        XqfKey key = calculateKey(header);

        // 计算开局FEN串
        String fen = calculateFen(header, key);
        cm.setFenCode(fen);

        // 解析棋谱记录
        ManualRecord head = parseMoves(buffer, header, key);
        cm.setHead(head);

        // 翻译招法为中文
        translate(fen, head);

        return cm;
    }

    /**
     * XQF文件头结构
     */
    private static class XqfHeader {
        int version;        // 版本号
        int keyMask;        // 加密掩码
        byte[] keyOr;       // Or密钥 (4字节)
        int keySum;         // 加密的钥匙和
        int keyXYp;         // 棋子布局位置钥匙
        int keyXYf;         // 棋谱起点钥匙
        int keyXYt;         // 棋谱终点钥匙
        byte[] qiziXY;      // 32个棋子的原始位置 (32字节)
        int playStepNo;     // 棋谱文件的开始步数
        int whoPlay;        // 该谁下 (0=红方, 1=黑方)
        int playResult;     // 最终结果 (0=未知, 1=红胜, 2=黑胜, 3=和棋)
        int type;           // 对局类型
        String title;       // 标题
        String matchName;   // 比赛名称
        String matchTime;   // 比赛时间
        String matchAddr;   // 比赛地点
        String redPlayer;   // 红方姓名
        String blkPlayer;   // 黑方姓名
        String timeRule;    // 用时规则
        String redTime;     // 红方用时
        String blkTime;     // 黑方用时
        String rmkWriter;   // 棋谱评论员
        String author;      // 文件作者
    }

    /**
     * XQF密钥结构
     */
    private static class XqfKey {
        int[] f32 = new int[32];  // 32字节密钥数组
        int xYp;                  // 棋子位置密钥
        int xYf;                  // 起点密钥
        int xYt;                  // 终点密钥
        int rmk;                  // 注释长度密钥
    }

    /**
     * 解析文件头
     */
    private XqfHeader parseHeader(byte[] buffer) {
        XqfHeader header = new XqfHeader();

        header.version = buffer[2] & 0xFF;
        header.keyMask = buffer[3] & 0xFF;
        header.keyOr = new byte[]{buffer[8], buffer[9], buffer[10], buffer[11]};
        header.keySum = buffer[12] & 0xFF;
        header.keyXYp = buffer[13] & 0xFF;
        header.keyXYf = buffer[14] & 0xFF;
        header.keyXYt = buffer[15] & 0xFF;

        // 32个棋子位置
        header.qiziXY = new byte[32];
        System.arraycopy(buffer, 16, header.qiziXY, 0, 32);

        // 开始步数 (2字节, 小端序)
        header.playStepNo = (buffer[49] & 0xFF) * 256 + (buffer[48] & 0xFF);

        header.whoPlay = buffer[50] & 0xFF;
        header.playResult = buffer[51] & 0xFF;
        header.type = buffer[64] & 0xFF;

        // 解析字符串字段
        header.title = readString(buffer, 80, 128);
        header.matchName = readString(buffer, 208, 64);
        header.matchTime = readString(buffer, 272, 16);
        header.matchAddr = readString(buffer, 288, 16);
        header.redPlayer = readString(buffer, 304, 16);
        header.blkPlayer = readString(buffer, 320, 16);
        header.timeRule = readString(buffer, 336, 64);
        header.redTime = readString(buffer, 400, 16);
        header.blkTime = readString(buffer, 416, 16);
        header.rmkWriter = readString(buffer, 464, 16);
        header.author = readString(buffer, 480, 16);

        return header;
    }

    /**
     * 从指定位置读取字符串 (GBK编码)
     * 第一个字节是长度，后面是内容
     */
    private String readString(byte[] buffer, int offset, int maxLength) {
        int length = buffer[offset] & 0xFF;
        if (length == 0 || length > maxLength - 1) {
            return "";
        }

        // 找到实际内容长度(去除末尾的0)
        int actualLength = 0;
        for (int i = 0; i < length && i < maxLength - 1; i++) {
            if (buffer[offset + 1 + i] != 0) {
                actualLength++;
            } else {
                break;
            }
        }

        if (actualLength == 0) {
            return "";
        }

        byte[] strBytes = new byte[actualLength];
        System.arraycopy(buffer, offset + 1, strBytes, 0, actualLength);
        return new String(strBytes, GBK_CHARSET);
    }

    /**
     * 计算解密密钥
     */
    private XqfKey calculateKey(XqfHeader header) {
        XqfKey key = new XqfKey();

        // XQF 1.0 版本 (version <= 10) 没有加密
        if (header.version <= 10) {
            return key;
        }

        // 计算密钥
        key.xYp = ((header.keyXYp * header.keyXYp * 54 + 221) * header.keyXYp) & 0xFF;
        key.xYf = ((header.keyXYf * header.keyXYf * 54 + 221) * key.xYp) & 0xFF;
        key.xYt = ((header.keyXYt * header.keyXYt * 54 + 221) * key.xYf) & 0xFF;
        key.rmk = (((header.keySum * 256 + header.keyXYp) % 32000) + 767) & 0xFFFF;

        // 计算F32密钥数组
        int[] fKey = new int[]{
                (header.keySum & header.keyMask) | (header.keyOr[0] & 0xFF),
                (header.keyXYp & header.keyMask) | (header.keyOr[1] & 0xFF),
                (header.keyXYf & header.keyMask) | (header.keyOr[2] & 0xFF),
                (header.keyXYt & header.keyMask) | (header.keyOr[3] & 0xFF)
        };

        String seed = "[(C) Copyright Mr. Dong Shiwei.]";
        for (int i = 0; i < 32; i++) {
            key.f32[i] = fKey[i % 4] & seed.charAt(i);
        }

        return key;
    }

    /**
     * 计算开局FEN串
     */
    private String calculateFen(XqfHeader header, XqfKey key) {
        // 创建10x9的棋盘数组，初始填充为'*'
        char[] fenArray = new char[90];
        for (int i = 0; i < 90; i++) {
            fenArray[i] = '*';
        }

        // 放置32个棋子
        for (int i = 0; i < 32; i++) {
            int pieceKey;
            int piecePos;

            if (header.version > 10) {
                pieceKey = (key.xYp + i + 1) & 31;
                piecePos = ((header.qiziXY[i] & 0xFF) - key.xYp) & 0xFF;
            } else {
                pieceKey = i;
                piecePos = header.qiziXY[i] & 0xFF;
            }

            if (piecePos < 90) {
                // X坐标 = piecePos / 10, Y坐标 = piecePos % 10
                // FEN数组索引 = (9 - Y) * 9 + X
                int x = piecePos / 10;
                int y = 9 - (piecePos % 10);
                int index = y * 9 + x;
                if (index >= 0 && index < 90) {
                    fenArray[index] = FEN_PIECES.charAt(pieceKey);
                }
            }
        }

        // 转换为FEN格式
        StringBuilder fen = new StringBuilder();
        for (int row = 0; row < 10; row++) {
            int emptyCount = 0;
            for (int col = 0; col < 9; col++) {
                char c = fenArray[row * 9 + col];
                if (c == '*') {
                    emptyCount++;
                } else {
                    if (emptyCount > 0) {
                        fen.append(emptyCount);
                        emptyCount = 0;
                    }
                    fen.append(c);
                }
            }
            if (emptyCount > 0) {
                fen.append(emptyCount);
            }
            if (row < 9) {
                fen.append('/');
            }
        }

        // 添加轮次和其他信息
        fen.append(header.whoPlay == 1 ? " b - - 0 " : " w - - 0 ");
        fen.append(header.playStepNo > 0 ? (header.playStepNo >> 1) : 1);

        return fen.toString();
    }

    /**
     * 解析棋谱着法记录
     */
    private ManualRecord parseMoves(byte[] buffer, XqfHeader header, XqfKey key) {
        ManualRecord head = new ManualRecord(0, "开始局面", 0);

        // 棋谱记录从0x400 (1024)开始
        if (buffer.length <= 1024) {
            return head;
        }

        // 解密数据
        int dataLength = buffer.length - 1024;
        byte[] decode = new byte[dataLength];

        for (int i = 0; i < dataLength; i++) {
            if (header.version > 10) {
                decode[i] = (byte) (((buffer[1024 + i] & 0xFF) - key.f32[i % 32]) & 0xFF);
            } else {
                decode[i] = buffer[1024 + i];
            }
        }

        // 解析棋步
        List<ManualRecord> changeNodeStack = new ArrayList<>();
        ManualRecord parent = head;
        int pos = 0;
        while (pos < decode.length) {
            // 新版无注释的节点只有4字节，旧版始终包含4字节注释长度。
            if (decode.length - pos < (header.version > 10 ? 4 : 8)) {
                break;
            }

            int commentLen = 0;
            int nextOffset = 4;

            // 判断是否有后续着法和变着
            boolean hasNext;
            boolean hasChange;

            if (header.version > 10) {
                hasNext = (decode[pos + 2] & 0x80) != 0;
                hasChange = (decode[pos + 2] & 0x40) != 0;

                // 检查是否有注释
                if ((decode[pos + 2] & 0x20) != 0) {
                    if (decode.length - pos < 8) {
                        break;
                    }
                    commentLen = readInt32(decode, pos + 4) - key.rmk;
                    if (commentLen < 0) commentLen = 0;
                    nextOffset = commentLen + 8;
                }
            } else {
                hasNext = (decode[pos + 2] & 0xF0) != 0;
                hasChange = (decode[pos + 2] & 0x0F) != 0;
                commentLen = readInt32(decode, pos + 4);
                nextOffset = commentLen + 8;
            }

            // 读取注释
            String comment = "";
            if (commentLen > 0 && pos + 8 + commentLen <= decode.length) {
                byte[] commentBytes = new byte[commentLen];
                System.arraycopy(decode, pos + 8, commentBytes, 0, commentLen);
                comment = new String(commentBytes, GBK_CHARSET);
            }

            // 第0步是空着法，只包含注释
            if (pos == 0) {
                if (StringUtils.isNotEmpty(comment)) {
                    head.setRemark(comment);
                }
                pos += hasNext ? nextOffset : decode.length;
                continue;
            }

            // 解析着法坐标
            int pf = ((decode[pos] & 0xFF) - 24 - key.xYf) & 0xFF;
            int pt = ((decode[pos + 1] & 0xFF) - 32 - key.xYt) & 0xFF;

            // 转换为ICCS格式 (如 a0a1)
            String move = convertToIccs(pf, pt);

            // 创建新节点
            ManualRecord step = new ManualRecord(parent.getId() + 1, move, "");
            if (StringUtils.isNotEmpty(comment)) {
                step.setRemark(comment);
            }

            parent.getList().add(step);

            if (hasNext) {
                if (hasChange) {
                    changeNodeStack.add(parent);
                }
                parent = step;
            } else {
                if (!hasChange) {
                    if (!changeNodeStack.isEmpty()) {
                        parent = changeNodeStack.remove(changeNodeStack.size() - 1);
                    }
                }
            }

            pos += nextOffset;

            // 安全检查：如果parent为null，退出循环
            if (parent == null) {
                break;
            }
        }

        return head;
    }

    /**
     * 读取32位整数 (小端序)
     */
    private int readInt32(byte[] data, int offset) {
        if (offset + 4 > data.length) {
            return 0;
        }
        return (data[offset] & 0xFF) |
                ((data[offset + 1] & 0xFF) << 8) |
                ((data[offset + 2] & 0xFF) << 16) |
                ((data[offset + 3] & 0xFF) << 24);
    }

    /**
     * 将XQF坐标转换为ICCS格式
     * XQF坐标: X * 10 + Y, 其中X(0-8), Y(0-9), 原点(0,0)在左下角
     * ICCS格式: 起始列(a-i) + 起始行(0-9) + 目标列(a-i) + 目标行(0-9)
     */
    private String convertToIccs(int fromPos, int toPos) {
        int fromX = fromPos / 10;
        int fromY = fromPos % 10;
        int toX = toPos / 10;
        int toY = toPos % 10;

        // 限制在有效范围内
        fromX = Math.max(0, Math.min(8, fromX));
        fromY = Math.max(0, Math.min(9, fromY));
        toX = Math.max(0, Math.min(8, toX));
        toY = Math.max(0, Math.min(9, toY));

        char fromFile = (char) ('a' + fromX);
        char toFile = (char) ('a' + toX);

        return "" + fromFile + fromY + toFile + toY;
    }

    @Override
    public void saveChessManual(ChessManual chessManual, File file) {
        // 先完整编码，避免局面或着法无效时截断已有棋谱。
        byte[] buffer = createXqfFile(chessManual);
        try (OutputStream output = new BufferedOutputStream(new FileOutputStream(file))) {
            output.write(buffer);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to save XQF file: " + file, e);
        }
    }

    private byte[] createXqfFile(ChessManual cm) {
        byte[] header = new byte[HEADER_SIZE];
        header[0] = 'X';
        header[1] = 'Q';
        header[2] = 18;

        writeString(header, 80, cm.getName(), 128);
        writeString(header, 272, cm.getDate(), 16);
        writeString(header, 288, cm.getCity(), 16);
        writeString(header, 304, cm.getRed(), 16);
        writeString(header, 320, cm.getBlack(), 16);

        String fen = cm.getFenCode();
        if (fen == null || fen.isBlank()) {
            fen = DEFAULT_FEN;
        }
        String[] fields = fen.trim().split("\\s+");
        fillInitialPosition(header, fields[0]);
        header[50] = (byte) (fields.length > 1 && "b".equals(fields[1]) ? 1 : 0);

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        output.writeBytes(header);
        ManualRecord head = cm.getHead();
        if (head == null) {
            head = new ManualRecord(0, "开始局面", 0);
        }
        writeNode(output, head, true, false);
        return output.toByteArray();
    }

    /** 固定字段包含一个长度字节，GBK编码溢出时不截断双字节字符。 */
    private void writeString(byte[] buffer, int offset, String text, int fieldSize) {
        if (text == null || text.isEmpty()) {
            return;
        }
        ByteBuffer bytes = ByteBuffer.wrap(buffer, offset + 1, fieldSize - 1);
        GBK_CHARSET.newEncoder()
                .onMalformedInput(CodingErrorAction.REPLACE)
                .onUnmappableCharacter(CodingErrorAction.REPLACE)
                .encode(CharBuffer.wrap(text), bytes, true);
        buffer[offset] = (byte) (bytes.position() - offset - 1);
    }

    /** 直接展开FEN，保留原始棋盘方向及缺子信息。 */
    private void fillInitialPosition(byte[] header, String placement) {
        String[] rows = placement.split("/", -1);
        if (rows.length != 10) {
            throw new IllegalArgumentException("Invalid XQF starting position: " + placement);
        }
        char[] pieces = FEN_PIECES.toCharArray();
        int[] positions = new int[32];
        Arrays.fill(positions, 90);
        for (int row = 0; row < rows.length; row++) {
            int col = 0;
            for (char piece : rows[row].toCharArray()) {
                if (piece >= '1' && piece <= '9') {
                    col += piece - '0';
                } else {
                    piece = switch (piece) {
                        case 'H' -> 'N';
                        case 'h' -> 'n';
                        case 'E' -> 'B';
                        case 'e' -> 'b';
                        default -> piece;
                    };
                    int slot = -1;
                    for (int i = 0; i < pieces.length; i++) {
                        if (pieces[i] == piece) {
                            slot = i;
                            break;
                        }
                    }
                    if (slot < 0 || col >= 9) {
                        throw new IllegalArgumentException("Invalid XQF starting position: " + placement);
                    }
                    positions[slot] = col * 10 + 9 - row;
                    pieces[slot] = '\0';
                    col++;
                }
            }
            if (col != 9) {
                throw new IllegalArgumentException("Invalid XQF starting position: " + placement);
            }
        }
        // 新版的布局槽位相对于棋子顺序偏移 XYp+1，此处 XYp=0。
        for (int i = 0; i < positions.length; i++) {
            header[16 + i] = (byte) positions[(i + 1) & 31];
        }
    }

    /** 按深度优先顺序保存所有变着，标记后续着法、同级变着和注释。 */
    private void writeNode(ByteArrayOutputStream output, ManualRecord record, boolean root, boolean hasSibling) {
        if (root) {
            output.write('X');
            output.write('Q');
        } else {
            String move = record.getMove();
            if (move == null || !move.matches("[a-i][0-9][a-i][0-9]")) {
                throw new IllegalArgumentException("Invalid ICCS move: " + move);
            }
            output.write((move.charAt(0) - 'a') * 10 + move.charAt(1) - '0' + 24);
            output.write((move.charAt(2) - 'a') * 10 + move.charAt(3) - '0' + 32);
        }

        List<ManualRecord> children = record.getList();
        String remark = record.getRemark();
        byte[] comment = remark == null ? new byte[0] : remark.getBytes(GBK_CHARSET);
        int flags = (children.isEmpty() ? 0 : 0x80) | (hasSibling ? 0x40 : 0)
                | (comment.length > 0 ? 0x20 : 0);
        output.write(flags);
        output.write(0);
        if (comment.length > 0) {
            int length = comment.length + COMMENT_LENGTH_KEY;
            for (int i = 0; i < 4; i++) {
                output.write(length >>> (i * 8));
            }
            output.writeBytes(comment);
        }
        for (int i = 0; i < children.size(); i++) {
            writeNode(output, children.get(i), false, i < children.size() - 1);
        }
    }

}
