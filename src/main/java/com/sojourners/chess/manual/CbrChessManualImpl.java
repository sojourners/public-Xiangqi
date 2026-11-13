package com.sojourners.chess.manual;

import com.sojourners.chess.model.ManualRecord;
import com.sojourners.chess.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

/** 象棋桥CBR棋谱读写 */
public class CbrChessManualImpl implements ChessManualService {

    private static final String DEFAULT_FEN = "rnbakabnr/9/1c5c1/p1p1p1p1p/9/9/P1P1P1P1P/1C5C1/9/RNBAKABNR w - - 0 1";

    private static final int POSITION_OFFSET = 2112;
    private static final int NODE_OFFSET = POSITION_OFFSET + 102;

    // CBR 文件头部标识: "CCBridge Record"
    private static final byte[] CBR_HEADER = {0x43, 0x43, 0x42, 0x72, 0x69, 0x64, 0x67, 0x65, 0x20, 0x52, 0x65, 0x63, 0x6F, 0x72, 0x64};

    // 棋子标识转换为 Fen 字符 (n2f)
    private static final char[] N2F = {
            '*', '*', '*', '*', '*', '*', '*', '*', '*', '*',
            '*', '*', '*', '*', '*', '*', '*', 'R', 'N', 'B',
            'A', 'K', 'C', 'P', '*', '*', '*', '*', '*', '*',
            '*', '*', '*', 'r', 'n', 'b', 'a', 'k', 'c', 'p'
    };

    // Fen 字符转换为棋子标识 (f2n)
    private static final Map<Character, Integer> F2N = new HashMap<>();
    static {
        F2N.put('R', 17); F2N.put('N', 18); F2N.put('H', 18); F2N.put('B', 19);
        F2N.put('E', 19); F2N.put('A', 20); F2N.put('K', 21); F2N.put('C', 22);
        F2N.put('P', 23); F2N.put('r', 33); F2N.put('n', 34); F2N.put('h', 34);
        F2N.put('b', 35); F2N.put('e', 35); F2N.put('a', 36); F2N.put('k', 37);
        F2N.put('c', 38); F2N.put('p', 39); F2N.put('*', 1);
    }

    // 棋盘坐标转换为 ICCS (b2i) - CBR版本1使用
    // 棋盘索引 0-89 对应 a9-i9, a8-i8, ..., a0-i0
    private static final String[] B2I = {
            "a9", "b9", "c9", "d9", "e9", "f9", "g9", "h9", "i9",
            "a8", "b8", "c8", "d8", "e8", "f8", "g8", "h8", "i8",
            "a7", "b7", "c7", "d7", "e7", "f7", "g7", "h7", "i7",
            "a6", "b6", "c6", "d6", "e6", "f6", "g6", "h6", "i6",
            "a5", "b5", "c5", "d5", "e5", "f5", "g5", "h5", "i5",
            "a4", "b4", "c4", "d4", "e4", "f4", "g4", "h4", "i4",
            "a3", "b3", "c3", "d3", "e3", "f3", "g3", "h3", "i3",
            "a2", "b2", "c2", "d2", "e2", "f2", "g2", "h2", "i2",
            "a1", "b1", "c1", "d1", "e1", "f1", "g1", "h1", "i1",
            "a0", "b0", "c0", "d0", "e0", "f0", "g0", "h0", "i0"
    };

    // ICCS 转换为棋盘坐标 (i2b) - CBR版本2使用
    private static final Map<String, Integer> I2B = new HashMap<>();
    static {
        I2B.put("a9", 0);  I2B.put("b9", 1);  I2B.put("c9", 2);  I2B.put("d9", 3);  I2B.put("e9", 4);
        I2B.put("f9", 5);  I2B.put("g9", 6);  I2B.put("h9", 7);  I2B.put("i9", 8);
        I2B.put("a8", 9);  I2B.put("b8", 10); I2B.put("c8", 11); I2B.put("d8", 12); I2B.put("e8", 13);
        I2B.put("f8", 14); I2B.put("g8", 15); I2B.put("h8", 16); I2B.put("i8", 17);
        I2B.put("a7", 18); I2B.put("b7", 19); I2B.put("c7", 20); I2B.put("d7", 21); I2B.put("e7", 22);
        I2B.put("f7", 23); I2B.put("g7", 24); I2B.put("h7", 25); I2B.put("i7", 26);
        I2B.put("a6", 27); I2B.put("b6", 28); I2B.put("c6", 29); I2B.put("d6", 30); I2B.put("e6", 31);
        I2B.put("f6", 32); I2B.put("g6", 33); I2B.put("h6", 34); I2B.put("i6", 35);
        I2B.put("a5", 36); I2B.put("b5", 37); I2B.put("c5", 38); I2B.put("d5", 39); I2B.put("e5", 40);
        I2B.put("f5", 41); I2B.put("g5", 42); I2B.put("h5", 43); I2B.put("i5", 44);
        I2B.put("a4", 45); I2B.put("b4", 46); I2B.put("c4", 47); I2B.put("d4", 48); I2B.put("e4", 49);
        I2B.put("f4", 50); I2B.put("g4", 51); I2B.put("h4", 52); I2B.put("i4", 53);
        I2B.put("a3", 54); I2B.put("b3", 55); I2B.put("c3", 56); I2B.put("d3", 57); I2B.put("e3", 58);
        I2B.put("f3", 59); I2B.put("g3", 60); I2B.put("h3", 61); I2B.put("i3", 62);
        I2B.put("a2", 63); I2B.put("b2", 64); I2B.put("c2", 65); I2B.put("d2", 66); I2B.put("e2", 67);
        I2B.put("f2", 68); I2B.put("g2", 69); I2B.put("h2", 70); I2B.put("i2", 71);
        I2B.put("a1", 72); I2B.put("b1", 73); I2B.put("c1", 74); I2B.put("d1", 75); I2B.put("e1", 76);
        I2B.put("f1", 77); I2B.put("g1", 78); I2B.put("h1", 79); I2B.put("i1", 80);
        I2B.put("a0", 81); I2B.put("b0", 82); I2B.put("c0", 83); I2B.put("d0", 84); I2B.put("e0", 85);
        I2B.put("f0", 86); I2B.put("g0", 87); I2B.put("h0", 88); I2B.put("i0", 89);
    }

    @Override
    public ChessManual openChessManual(File file) {
        try {
            byte[] buffer = Files.readAllBytes(file.toPath());

            // 检查文件头
            if (!isValidCbrFile(buffer)) {
                return null;
            }

            ChessManual cm = new ChessManual();

            // 解析版本
            int ver = buffer[19] & 0xFF;
            if (ver < 1 || ver > 2) {
                // 不支持的版本，使用默认FEN
                cm.setFenCode(DEFAULT_FEN);
                ManualRecord head = new ManualRecord(0, "开始局面", 0);
                cm.setHead(head);
                return cm;
            }

            // 解析棋局信息
            parseChessInfo(cm, buffer, ver);

            // 解析棋谱节点树
            ManualRecord head = parseNodeTree(buffer, ver);
            cm.setHead(head);

            // 从根节点获取FEN并设置到ChessManual
            if (head != null && StringUtils.isNotEmpty(head.getMove())) {
                cm.setFenCode(head.getMove());
                // 清空head的move，因为FEN已经设置到ChessManual
                head.setMove("");
            } else {
                cm.setFenCode(DEFAULT_FEN);
            }

            translate(cm.getFenCode(), cm.getHead());

            return cm;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private boolean isValidCbrFile(byte[] buffer) {
        if (buffer.length < 20) {
            return false;
        }
        for (int i = 0; i < CBR_HEADER.length; i++) {
            if (buffer[i] != CBR_HEADER[i]) {
                return false;
            }
        }
        return true;
    }

    private void parseChessInfo(ChessManual cm, byte[] buffer, int ver) {
        if (ver == 1) {
            cm.setName(readStrCbr(buffer, 52, 128));
            cm.setDate(readStrCbr(buffer, 756, 64));
            cm.setCity(readStrCbr(buffer, 820, 64));
            cm.setRed(readStrCbr(buffer, 948, 64));
            cm.setBlack(readStrCbr(buffer, 1172, 64));
        } else {
            cm.setName(readStrCbr(buffer, 180, 128));
            cm.setDate(readStrCbr(buffer, 884, 64));
            cm.setCity(readStrCbr(buffer, 948, 64));
            cm.setRed(readStrCbr(buffer, 1076, 64));
            cm.setBlack(readStrCbr(buffer, 1300, 64));
        }
    }

    private String readStrCbr(byte[] buffer, int start, int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i += 2) {
            int low = buffer[start + i] & 0xFF;
            int high = buffer[start + i + 1] & 0xFF;
            if (low == 0 && high == 0) {
                break;
            }
            char c = (char) ((high << 8) | low);
            sb.append(c);
        }
        return sb.toString();
    }

    private ManualRecord parseNodeTree(byte[] buffer, int ver) {
        int offset = ver == 1 ? 1856 : 2112;

        // 读取棋盘局面 (90个格子)
        char[] board = new char[90];
        for (int i = 0; i < 90; i++) {
            int pieceIdx = buffer[offset + 8 + i] & 0xFF;
            board[i] = pieceIdx < N2F.length ? N2F[pieceIdx] : '*';
        }

        // 构建FEN字符串
        String fen = buildFen(board, buffer, offset);

        // 创建根节点
        ManualRecord head = new ManualRecord(0, "开始局面", 0);
        head.setMove("");
        head.setRemark("");

        // 解析着法数据
        int pos = offset + 102;

        // 使用栈来处理变着 (对应JS中的changeNode数组)
        Stack<ManualRecord> changeNodeStack = new Stack<>();
        ManualRecord parent = head;
        boolean hasRootComment = false;

        while (pos < buffer.length - 3) {
            int sig = buffer[pos] & 0xFF;
            int src = buffer[pos + 2] & 0xFF;
            int dst = buffer[pos + 3] & 0xFF;

            // 结束条件检查
            if (sig > 7 || (src == dst && hasRootComment)) {
                break;
            }

            // sig 位含义: bit0=1表示无后续, bit1=1表示有变着, bit2=1表示有注解
            boolean hasNext = (sig & 1) == 0;   // bit 0: 0=有后续着法, 1=无后续着法
            boolean hasChange = (sig & 2) != 0; // bit 1: 1=有变着
            boolean hasComment = (sig & 4) != 0; // bit 2: 1=有注解

            int nextOffset = 4;
            String comment = "";

            // 读取注解
            if (hasComment) {
                int commentLen = 0;
                for (int i = 0; i < 4; i++) {
                    commentLen += (buffer[pos + 4 + i] & 0xFF) << (8 * i);
                }
                comment = readStrCbr(buffer, pos + 8, commentLen);
                nextOffset = commentLen + 8;
            }

            // 根节点注解 (src == dst 表示根节点注释)
            if (src == dst) {
                head.setRemark(comment);
                hasRootComment = true;
                if (!hasNext) {
                    break;
                }
                pos += nextOffset;
                continue;
            }

            // 转换为ICCS格式着法
            String move;
            if (ver == 1) {
                // 版本1: src/16 + 97 得到字符, src%16 得到数字
                char fromFile = (char) (src / 16 + 97);
                int fromRank = src % 16;
                char toFile = (char) (dst / 16 + 97);
                int toRank = dst % 16;
                String rawMove = "" + fromFile + fromRank + toFile + toRank;
                move = flipMove(rawMove);
            } else {
                // 版本2: 直接使用B2I映射
                move = B2I[src] + B2I[dst];
            }

            // 创建新节点
            ManualRecord step = new ManualRecord(parent.getId() + 1, move, "");
            step.setRemark(comment);
            step.setNext(0);

            // 添加到父节点的列表
            parent.getList().add(step);

            // 处理变着逻辑 (对应JS逻辑)
            if (hasNext) {
                // 有后续着法
                if (hasChange) {
                    changeNodeStack.push(parent);
                }
                parent = step;
            } else {
                // 无后续着法
                if (!hasChange && !changeNodeStack.isEmpty()) {
                    parent = changeNodeStack.pop();
                }
            }

            // 部分棋谱存在冗余错误数据，直接退出
            if (parent == null) {
                break;
            }

            pos += nextOffset;
        }

        // 增强兼容性：根据第一步推断当前走棋方
        if (!head.getList().isEmpty()) {
            String firstMove = head.getList().get(0).getMove();
            if (StringUtils.isNotEmpty(firstMove) && firstMove.length() >= 2) {
                String fromPos = firstMove.substring(0, 2);
                int position = I2B.getOrDefault(fromPos, 0);
                if (position >= 0 && position < 90) {
                    char piece = board[position];
                    // 根据棋子判断走棋方 (大写为红方w，小写为黑方b)
                    String side = (piece >= 'A' && piece <= 'Z') ? "w" : "b";
                    // 更新FEN中的走棋方
                    String[] fenParts = fen.split(" ");
                    if (fenParts.length >= 2) {
                        fenParts[1] = side;
                        fen = String.join(" ", fenParts);
                    }
                }
            }
        }

        head.setMove(fen);
        return head;
    }

    private String buildFen(char[] board, byte[] buffer, int offset) {
        StringBuilder fen = new StringBuilder();

        // 将90个格子转换为FEN格式 (10行 x 9列)
        for (int row = 0; row < 10; row++) {
            int emptyCount = 0;
            for (int col = 0; col < 9; col++) {
                int idx = row * 9 + col;
                char piece = board[idx];
                if (piece == '*' || piece == ' ') {
                    emptyCount++;
                } else {
                    if (emptyCount > 0) {
                        fen.append(emptyCount);
                        emptyCount = 0;
                    }
                    fen.append(piece);
                }
            }
            if (emptyCount > 0) {
                fen.append(emptyCount);
            }
            if (row < 9) {
                fen.append('/');
            }
        }

        // 添加走棋方和其他FEN字段
        int side = buffer[offset] & 0xFF;
        String activeSide = (side == 2) ? "b" : "w";
        int stepCount = ((buffer[offset + 5] & 0xFF) << 8) | (buffer[offset + 4] & 0xFF);

        fen.append(" ").append(activeSide).append(" - - 0 ").append(stepCount);

        return fen.toString();
    }

    private String flipMove(String move) {
        // 翻转着法坐标 (用于版本1)
        if (move == null || move.length() != 4) {
            return move;
        }
        char fromFile = move.charAt(0);
        char fromRank = move.charAt(1);
        char toFile = move.charAt(2);
        char toRank = move.charAt(3);

        // 翻转文件和等级
        fromFile = (char) ('i' - (fromFile - 'a'));
        fromRank = (char) ('9' - (fromRank - '0'));
        toFile = (char) ('i' - (toFile - 'a'));
        toRank = (char) ('9' - (toRank - '0'));

        return "" + fromFile + fromRank + toFile + toRank;
    }

    @Override
    public void saveChessManual(ChessManual chessManual, File file) {
        // 完整编码后再打开文件，避免无效局面或着法截断已有棋谱。
        byte[] buffer = createCbrFile(chessManual);
        try {
            Files.write(file.toPath(), buffer);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to save CBR file: " + file, e);
        }
    }

    private byte[] createCbrFile(ChessManual cm) {
        byte[] header = new byte[NODE_OFFSET];
        System.arraycopy(CBR_HEADER, 0, header, 0, CBR_HEADER.length);
        header[19] = 2;
        Arrays.fill(header, 2210, NODE_OFFSET, (byte) 0xFF);

        writeString(header, 180, cm.getName(), 128);
        writeString(header, 884, cm.getDate(), 64);
        writeString(header, 948, cm.getCity(), 64);
        writeString(header, 1076, cm.getRed(), 64);
        writeString(header, 1300, cm.getBlack(), 64);

        String fen = cm.getFenCode();
        if (fen == null || fen.isBlank()) {
            fen = DEFAULT_FEN;
        }
        String[] fields = fen.trim().split("\\s+");
        fillInitialPosition(header, fields[0]);
        header[POSITION_OFFSET] = (byte) (fields.length > 1 && "b".equals(fields[1]) ? 2 : 1);
        int fullMove = fields.length > 5 ? Integer.parseInt(fields[5]) : 1;
        if (fullMove < 0 || fullMove > 0xFFFF) {
            throw new IllegalArgumentException("CBR full move number must be between 0 and 65535");
        }
        header[POSITION_OFFSET + 4] = (byte) fullMove;
        header[POSITION_OFFSET + 5] = (byte) (fullMove >>> 8);

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        output.writeBytes(header);
        ManualRecord head = cm.getHead();
        if (head == null) {
            head = new ManualRecord(0, "开始局面", 0);
        }
        writeNode(output, head, true, false);
        return output.toByteArray();
    }

    /** 固定字段使用UTF-16LE，可填满字段，但不截断代理对或覆盖相邻字段。 */
    private void writeString(byte[] buffer, int offset, String text, int fieldSize) {
        if (text == null || text.isEmpty()) {
            return;
        }
        int length = Math.min(text.length(), fieldSize / 2);
        if (length < text.length() && length > 0
                && Character.isHighSurrogate(text.charAt(length - 1))
                && Character.isLowSurrogate(text.charAt(length))) {
            length--;
        }
        byte[] bytes = text.substring(0, length).getBytes(StandardCharsets.UTF_16LE);
        System.arraycopy(bytes, 0, buffer, offset, bytes.length);
    }

    /** 按FEN原始方向展开，CBR使用从a9到i0的90格棋盘，空格编码为0。 */
    private void fillInitialPosition(byte[] header, String placement) {
        String[] rows = placement.split("/", -1);
        if (rows.length != 10) {
            throw new IllegalArgumentException("Invalid CBR starting position: " + placement);
        }
        for (int row = 0; row < rows.length; row++) {
            int col = 0;
            for (char piece : rows[row].toCharArray()) {
                if (piece >= '1' && piece <= '9') {
                    col += piece - '0';
                } else {
                    Integer code = F2N.get(piece);
                    if (code == null || code <= 1 || col >= 9) {
                        throw new IllegalArgumentException("Invalid CBR starting position: " + placement);
                    }
                    header[POSITION_OFFSET + 8 + row * 9 + col] = code.byteValue();
                    col++;
                }
            }
            if (col != 9) {
                throw new IllegalArgumentException("Invalid CBR starting position: " + placement);
            }
        }
    }

    /** 深度优先保存全部变着；位0表示无后续，位1表示有同级变着，位2表示有注释。 */
    private void writeNode(ByteArrayOutputStream output, ManualRecord record, boolean root, boolean hasSibling) {
        int from = 0;
        int to = 0;
        if (!root) {
            String move = record.getMove();
            if (move == null || !move.matches("[a-i][0-9][a-i][0-9]")) {
                throw new IllegalArgumentException("Invalid ICCS move: " + move);
            }
            from = I2B.get(move.substring(0, 2));
            to = I2B.get(move.substring(2, 4));
            if (from == to) {
                throw new IllegalArgumentException("CBR move must have different source and destination: " + move);
            }
        }
        List<ManualRecord> children = record.getList();
        String remark = record.getRemark();
        byte[] comment = remark == null ? new byte[0] : remark.getBytes(StandardCharsets.UTF_16LE);
        int flags = (children.isEmpty() ? 1 : 0) | (hasSibling ? 2 : 0)
                | (comment.length > 0 ? 4 : 0);
        output.write(flags);
        output.write(0);
        output.write(from);
        output.write(to);
        if (comment.length > 0) {
            for (int i = 0; i < 4; i++) {
                output.write(comment.length >>> (i * 8));
            }
            output.writeBytes(comment);
        }
        for (int i = 0; i < children.size(); i++) {
            writeNode(output, children.get(i), false, i < children.size() - 1);
        }
    }
}
