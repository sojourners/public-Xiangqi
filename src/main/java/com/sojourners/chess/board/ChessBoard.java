package com.sojourners.chess.board;

import com.sojourners.chess.media.SoundPlayer;
import com.sojourners.chess.util.PathUtils;
import com.sojourners.chess.util.StringUtils;
import com.sojourners.chess.util.XiangqiUtils;
import javafx.scene.canvas.Canvas;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 棋盘
 */
public class ChessBoard {

    private static BaseBoardRender boardRender;

    private static volatile char[][] board = new char[10][9];

    private static char[][] copyBoard = new char[10][9];

    private BoardSize boardSize;

    private boolean stepTip;

    private boolean showNumber;

    private boolean stepSound;

    private static SoundPlayer sound;

    public static Map<Character, String> map = new HashMap<>(32);

    static {
        map.put('r', "车");
        map.put('n', "马");
        map.put('b', "象");
        map.put('a', "士");
        map.put('k', "将");
        map.put('c', "炮");
        map.put('p', "卒");

        map.put('R', "车");
        map.put('N', "马");
        map.put('B', "相");
        map.put('A', "仕");
        map.put('K', "帅");
        map.put('C', "炮");
        map.put('P', "兵");

        map.put('１', "一");
        map.put('２', "二");
        map.put('３', "三");
        map.put('４', "四");
        map.put('５', "五");
        map.put('６', "六");
        map.put('７', "七");
        map.put('８', "八");
        map.put('９', "九");

        sound = new SoundPlayer(PathUtils.getJarPath() + "sound/click.wav",
                PathUtils.getJarPath() + "sound/move.wav",
                PathUtils.getJarPath() + "sound/capture.wav",
                PathUtils.getJarPath() + "sound/check.wav",
                PathUtils.getJarPath() + "sound/win.wav");
    }

    private Point remark;

    private Step prevStep;

    private Step tipFirst, tipSecond;

    private boolean isReverse;

    public static class Point {
        int x;
        int y;
        public Point(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public int getX() {
            return x;
        }

        public void setX(int x) {
            this.x = x;
        }

        public int getY() {
            return y;
        }

        public void setY(int y) {
            this.y = y;
        }
    }
    public class Step {
        Point first;
        Point second;
        public Step(Point first, Point second) {
            this.first = first;
            this.second = second;
        }

        public Point getFirst() {
            return first;
        }

        public void setFirst(Point first) {
            this.first = first;
        }

        public Point getSecond() {
            return second;
        }

        public void setSecond(Point second) {
            this.second = second;
        }
    }

    public enum BoardSize {
        LARGE_BOARD,
        BIG_BOARD,
        MIDDLE_BOARD,
        SMALL_BOARD,
        AUTOFIT_BOARD
    }
    public enum BoardStyle {
        DEFAULT,
        CUSTOM;
    }

    public ChessBoard(Canvas canvas, BoardSize bs, BoardStyle style, boolean stepTip, boolean stepSound, boolean showNumber, String fenCode) {
        if (this.boardRender == null) {
            this.boardRender = style == BoardStyle.CUSTOM ? new CustomBoardRender(canvas) : new DefaultBoardRender(canvas);
        }

        this.stepTip = stepTip;
        this.stepSound = stepSound;
        this.showNumber = showNumber;
        // 设置局面
        setNewBoard(fenCode);
        // 设置棋盘大小
        this.boardSize = bs;
        // 默认不翻转
        isReverse = false;

        this.paint();
    }

    public static void initChessBoard(char[][] board) {
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 9; j++) {
                if (i == 0 && (j == 0 || j == 8)) {
                    board[i][j] = 'r';
                } else if (i == 0 && (j == 1 || j == 7)) {
                    board[i][j] = 'n';
                } else if (i == 0 && (j == 2 || j == 6)) {
                    board[i][j] = 'b';
                } else if (i == 0 && (j == 3 || j == 5)) {
                    board[i][j] = 'a';
                } else if (i == 0 && j == 4) {
                    board[i][j] = 'k';
                } else if (i == 2 && (j == 1 || j == 7)) {
                    board[i][j] = 'c';
                } else if (i == 3 && (j == 0 || j == 2 || j == 4 || j == 6 || j == 8)) {
                    board[i][j] = 'p';
                } else if (i == 9 && (j == 0 || j == 8)) {
                    board[i][j] = 'R';
                } else if (i == 9 && (j == 1 || j == 7)) {
                    board[i][j] = 'N';
                } else if (i == 9 && (j == 2 || j == 6)) {
                    board[i][j] = 'B';
                } else if (i == 9 && (j == 3 || j == 5)) {
                    board[i][j] = 'A';
                } else if (i == 9 && j == 4) {
                    board[i][j] = 'K';
                } else if (i == 7 && (j == 1 || j == 7)) {
                    board[i][j] = 'C';
                } else if (i == 6 && (j == 0 || j == 2 || j == 4 || j == 6 || j == 8)) {
                    board[i][j] = 'P';
                } else {
                    board[i][j] = ' ';
                }
            }
        }
    }

    private void setNewBoard(String fenCode) {
        if (StringUtils.isEmpty(fenCode)) {
            initChessBoard(board);
        } else {
            setBoard(fenCode);
        }
    }

    public void setBoardStyle(BoardStyle style, Canvas canvas) {
        this.boardRender = style == BoardStyle.CUSTOM ? new CustomBoardRender(canvas) : new DefaultBoardRender(canvas);
        this.paint();
    }

    public String mouseClick(int x, int y, boolean canRedGo, boolean canBlackGo) {
        int padding = boardRender.getPadding(this.boardSize);
        int piece = boardRender.getPieceSize(this.boardSize);
        int i = (x - padding) / piece;
        int j = (y - padding) / piece;
        i = boardRender.getReverseX(i, isReverse);
        j = boardRender.getReverseY(j, isReverse);

        if (i < 0 || i > 8 || j < 0 || j > 9) {
            return null;
        }

        if (remark != null) {
            boolean isRed = XiangqiUtils.isRed(board[remark.y][remark.x]);
            if (isRed && !canRedGo || !isRed && !canBlackGo) {
                return null;
            } else if (board[j][i] != ' ' && XiangqiUtils.isRed(board[j][i]) == isRed) {
                if (stepSound) sound.pick();
                remark = new Point(i, j);
                paint();
                return null;
            } else if (!XiangqiUtils.canGo(board, remark.y, remark.x, j, i)) {
                return null;
            } else {
                return move(remark.x, remark.y, i, j);
            }
        } else {
            if (board[j][i] != ' ') {
                boolean isRed = XiangqiUtils.isRed(board[j][i]);
                if (!(isRed && !canRedGo || !isRed && !canBlackGo)) {
                    if (stepSound) sound.pick();
                    remark = new Point(i, j);
                    paint();
                }
            }
            return null;
        }

    }

    private void setBoard(String fenCode) {
        try {
            String[] arr = fenCode.split(" ")[0].split("/");
            if (XiangqiUtils.isReverse(fenCode)) {
                for (int i = 0; i < arr.length / 2; i++) {
                    String tmp = arr[i];
                    arr[i] = new StringBuffer(arr[arr.length - 1 - i]).reverse().toString();
                    arr[arr.length - 1 - i] = new StringBuffer(tmp).reverse().toString();
                }
            }
            for (int i = 0; i < 10; i++) {
                int p = 0;
                for (char c : arr[i].toCharArray()) {
                    if (c >= '1' && c <= '9') {
                        int count = c - '0';
                        while (count-- > 0) {
                            board[i][p++] = ' ';
                        }
                    } else {
                        board[i][p++] = c;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String fenCode(boolean redGo) {
        return fenCode(this.board, redGo);
    }

    public static String fenCode(char[][] board, Boolean redGo) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < board.length; i++) {
            int count = 0;
            for (int j = 0; j < board[0].length; j++) {
                if (board[i][j] != ' ') {
                    if (count != 0) {
                        sb.append(count);
                        count = 0;
                    }
                    sb.append(board[i][j]);
                } else {
                    count++;
                }
            }
            if (count != 0) {
                sb.append(count);
            }
            if (i != board.length - 1) {
                sb.append("/");
            }
        }
        if (redGo != null) {
            if (redGo) {
                sb.append(" w - - 0 1");
            } else {
                sb.append(" b - - 0 1");
            }
        }
        return sb.toString();
    }

    /**
     * 浏览棋谱
     * @param fenCode
     * @param moveList
     * @param p
     */
    public void browseChessRecord(String fenCode, List<String> moveList, int p) {
        setBoard(fenCode);
        if (p == 0) {
            // 开始局面
            prevStep = null;
            tipFirst = null; tipSecond = null;
            remark = null;
            paint();
        } else {
            for (int i = 0; i < p - 1; i++) {
                Step s = stepForBoard(moveList.get(i));
                board[s.second.y][s.second.x] = board[s.first.y][s.first.x];
                board[s.first.y][s.first.x] = ' ';
            }
            Step s = stepForBoard(moveList.get(p - 1));
            move(s.first.x, s.first.y, s.second.x, s.second.y);
        }
    }

    public void setTip(String firstMove, String secondMove) {
        this.tipFirst = stepForBoard(firstMove);
        this.tipSecond = stepForBoard(secondMove);
        if (stepTip) {
            paint();
        }
    }

    public Step stepForBoard(String step) {
        if (step == null) {
            return null;
        }
        char c = step.charAt(0);
        int x1 = c - 'a';
        c = step.charAt(1);
        int y1 = 9 - Integer.parseInt(String.valueOf(c));
        c = step.charAt(2);
        int x2 = c - 'a';
        c = step.charAt(3);
        int y2 = 9 - Integer.parseInt(String.valueOf(c));
        return new Step(new Point(x1, y1), new Point(x2, y2));
    }

    public Step move(String step) {
        if (step == null || step.length() != 4) {
            return null;
        }
        Step s = stepForBoard(step);
        move(s.first.x, s.first.y, s.second.x, s.second.y);
        return s;
    }

    public String move(int x1, int y1, int x2, int y2) {
        char tmp = board[y2][x2];
        boolean isRed = XiangqiUtils.isRed(board[y1][x1]);
        board[y2][x2] = board[y1][x1];
        board[y1][x1] = ' ';
        if (XiangqiUtils.isJiang(board, isRed)) {
            // 不可送将
            if (stepSound) {
                sound.check();
            }
            board[y1][x1] = board[y2][x2];
            board[y2][x2] = tmp;
            return null;
        }
        if (stepSound) {
            if (XiangqiUtils.isSha(board, !isRed)) {
                // 绝杀
                sound.over();
            } else if (XiangqiUtils.isJiang(board, !isRed)) {
                // 将军
                sound.check();
            } else {
                // 是否吃子
                if (tmp == ' ') {
                    sound.move();
                } else {
                    sound.eat();
                }
            }
        }

        prevStep = new Step(new Point(x1, y1), new Point(x2, y2));
        tipFirst = null; tipSecond = null;
        remark = null;
        paint();
        return stepForEngine(x1, y1, x2, y2);
    }

    private String stepForEngine(int x1, int y1, int x2, int y2) {
        StringBuffer sb = new StringBuffer();
        sb.append((char)('a' + x1));
        sb.append(9 - y1);
        sb.append((char)('a' + x2));
        sb.append(9 - y2);
        return sb.toString();
    }

    private void paint() {
        this.boardRender.paint(boardSize, this.board, prevStep, remark, stepTip, tipFirst, tipSecond, isReverse, showNumber);
    }

    /**
     * 设置翻转
     * @param isReverse
     */
    public void reverse(boolean isReverse) {
        if (this.isReverse != isReverse) {
            this.isReverse = isReverse;
            paint();
        }
    }

    /**
     * 设置棋盘样式
     * @param bs
     */
    public void setBoardSize(BoardSize bs) {
        this.boardSize = bs;
        paint();
    }

    /**
     * 设置棋步提示
     * @param f
     */
    public void setStepTip(boolean f) {
        this.stepTip = f;
        paint();
    }

    public void setShowNumber(boolean showNumber) {
        this.showNumber = showNumber;
        paint();
    }

    /**
     * 设置走棋音效
     * @param f
     */
    public void setStepSound(boolean f) {
        this.stepSound = f;
    }

    /**
     * 翻译着法(记录棋谱)
     * @param move
     * @return
     */
    public String translate(String move, boolean hasGo) {
        StringBuilder sb = new StringBuilder();
        translateStep(this.board, sb, move, hasGo);
        return sb.toString();
    }

    /**
     * 翻译引擎着法(思考细节)
     * @param moveList
     * @return
     */
    public String translate(List<String> moveList) {
        for (int i = 0; i < board.length; i++) {
            for (int j = 0; j < board[0].length; j++) {
                copyBoard[i][j] = board[i][j];
            }
        }
        StringBuilder sb = new StringBuilder();
        for (String move : moveList) {
            translateStep(copyBoard, sb, move, false);
        }
        sb.delete(sb.length() - 2, sb.length());
        return sb.toString();
    }

    public char[][] getBoard() {
        return this.board;
    }

    private void translateStep(char[][] board, StringBuilder sb, String move, boolean hasGo) {
        if (StringUtils.isEmpty(move) || move.length() < 4) {
            sb.append(move);
            return;
        }
        char a = move.charAt(0), b = move.charAt(1), c = move.charAt(2), d = move.charAt(3);
        int fromI = 9 - Integer.parseInt(String.valueOf(b)), toI = 9 - Integer.parseInt(String.valueOf(d));
        int fromJ = a - 'a', toJ = c - 'a';
        boolean isRed = XiangqiUtils.isRed(hasGo ? board[toI][toJ] : board[fromI][fromJ]);
        //针对棋盘上同时存在前后的情况进行处理 马八进九 h0g2
        char piece = hasGo?board[toI][toJ] : board[fromI][fromJ];
        String oneTwo = "";
        for(int i =0;i<9;i++){
            if((!hasGo &&i == fromI)||(hasGo&&fromJ == toJ&&i == toI)){
                continue;
            }
            if(piece == board[i][fromJ]){
                //说明有重复情况
                if((isRed&&i>fromI) || (!isRed&&i<fromI)){
                    oneTwo = oneTwo+"前"+ map.get(piece);
                }else{
                    oneTwo = oneTwo+"后"+ map.get(piece);
                }
                break;
            }
        }

        char pos = getPos(fromJ, isRed);
        if(StringUtils.isNotEmpty(oneTwo)){
            sb.append(oneTwo);
        }else{
            sb.append(map.get(hasGo ? board[toI][toJ] : board[fromI][fromJ]));
            sb.append(isRed ? map.get(pos) : pos);
        }

        if (fromI == toI && fromJ != toJ) {
            sb.append("平");
            pos = getPos(toJ, isRed);
            sb.append(isRed ? map.get(pos) : pos);
        } else if (fromI != toI && fromJ == toJ) {
            if (isRed) {
                sb.append(fromI > toI ? "进" : "退");
            } else {
                sb.append(fromI < toI ? "进" : "退");
            }
            pos = (char) ('０' + (Math.abs(fromI - toI)));
            sb.append(isRed ? map.get(pos) : pos);
        } else {
            if (isRed) {
                sb.append(fromI > toI ? "进" : "退");
            } else {
                sb.append(fromI < toI ? "进" : "退");
            }
            pos = getPos(toJ, isRed);
            sb.append(isRed ? map.get(pos) : pos);
        }
        sb.append("  ");
        copyBoard[toI][toJ] = copyBoard[fromI][fromJ];
        copyBoard[fromI][fromJ] = ' ';
    }

    private char getPos(int j, boolean isRed) {
        if (isRed) {
            return (char) ('０' + 9 - j);
        } else {
            return (char) ('０' + j + 1);
        }
    }

    public void autoFitSize(double width, double height, double position, boolean showStatusBar) {
        if (boardSize == BoardSize.AUTOFIT_BOARD) {
            position = Math.abs(position);
            width = width * position;
            height = height - 56;
            if (showStatusBar) {
                height = height - 27;
            }
            int pieceSize;
            if (width / height > 1120 / 1240d) {
                pieceSize = (int) (height / (10 + 1/3d));
            } else {
                pieceSize = (int) (width / (9 + 1/3d));
            }
            if (pieceSize < 42) {
                pieceSize = 42;
            }
            boardRender.setAutoPieceSize(pieceSize);

            paint();
        }
    }
}
