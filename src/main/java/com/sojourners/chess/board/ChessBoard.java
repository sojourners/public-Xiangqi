package com.sojourners.chess.board;

import com.sojourners.chess.config.Properties;
import com.sojourners.chess.media.SoundPlayer;
import com.sojourners.chess.util.PathUtils;
import com.sojourners.chess.util.StringUtils;
import com.sojourners.chess.util.XiangqiUtils;
import javafx.scene.canvas.Canvas;

import java.util.ArrayList;
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

    private boolean manualTip;
    private List<Step> manualList = new ArrayList<>();

    private static SoundPlayer sound;

    static {
        sound = new SoundPlayer(PathUtils.getJarPath() + "sound/click.wav",
                PathUtils.getJarPath() + "sound/move.wav",
                PathUtils.getJarPath() + "sound/capture.wav",
                PathUtils.getJarPath() + "sound/check.wav",
                PathUtils.getJarPath() + "sound/win.wav");
    }

    private Point remark;

    private Step prevStep;

    private boolean showMultiPV;

    private List<MoveTip> moveTips = new ArrayList<>();

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
    public static class Step {
        Point start;
        Point end;
        public Step(Point start, Point end) {
            this.start = start;
            this.end = end;
        }

        public Point getStart() {
            return start;
        }

        public void setStart(Point start) {
            this.start = start;
        }

        public Point getEnd() {
            return end;
        }

        public void setEnd(Point end) {
            this.end = end;
        }
    }

    public class MoveTip {
        Step first;
        Step second;

        public MoveTip(Step first, Step second) {
            this.first = first;
            this.second = second;
        }

        public Step getFirst() {
            return first;
        }

        public void setFirst(Step first) {
            this.first = first;
        }

        public Step getSecond() {
            return second;
        }

        public void setSecond(Step second) {
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

    public ChessBoard(Canvas canvas, BoardSize bs, BoardStyle style, String customSkin, boolean stepTip, boolean manualTip,
                      boolean showMultiPV, boolean stepSound, boolean showNumber, String fenCode) {
        if (this.boardRender == null) {
            this.boardRender = style == BoardStyle.CUSTOM && CustomBoardRender.isSkinAvailable(customSkin)
                    ? new CustomBoardRender(canvas, customSkin) : new DefaultBoardRender(canvas);
        }

        this.stepTip = stepTip;
        this.manualTip = manualTip;
        this.stepSound = stepSound;
        this.showNumber = showNumber;
        this.showMultiPV = showMultiPV;
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

    public void showMultiPV(boolean showMultiPV) {
        this.showMultiPV = showMultiPV;
    }

    private void setNewBoard(String fenCode) {
        if (StringUtils.isEmpty(fenCode)) {
            initChessBoard(board);
        } else {
            setBoard(fenCode);
        }
    }

    public void setBoardStyle(BoardStyle style, String customSkin, Canvas canvas) {
        this.boardRender = style == BoardStyle.CUSTOM && CustomBoardRender.isSkinAvailable(customSkin)
                ? new CustomBoardRender(canvas, customSkin) : new DefaultBoardRender(canvas);
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
        XiangqiUtils.fenToBoard(this.board, fenCode);
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
     */
    public void browseChessRecord(String fenCode, List<String> moveList) {
        setBoard(fenCode);
        if (moveList == null || moveList.isEmpty()) {
            // 开始局面
            prevStep = null;
            moveTips.clear();
            remark = null;
            manualList.clear();
            paint();
        } else {
            for (int i = 0; i < moveList.size() - 1; i++) {
                Step s = stepForBoard(moveList.get(i));
                board[s.getEnd().y][s.getEnd().x] = board[s.getStart().y][s.getStart().x];
                board[s.getStart().y][s.getStart().x] = ' ';
            }
            Step s = stepForBoard(moveList.get(moveList.size() - 1));
            move(s.getStart().x, s.getStart().y, s.getEnd().x, s.getEnd().y);
        }
    }

    public void setTip(String firstMove, String secondMove, int pv) {
        if (pv < moveTips.size()) {
            moveTips.clear();
        }
        if (pv > moveTips.size()) {
            moveTips.add(new MoveTip(stepForBoard(firstMove), stepForBoard(secondMove)));
        } else {
            moveTips.set(pv - 1, new MoveTip(stepForBoard(firstMove), stepForBoard(secondMove)));
        }
        if (stepTip) {
            paint();
        }
    }

    public void setManualList(List<String> list) {
        manualList.clear();
        for (String move : list) {
            manualList.add(stepForBoard(move));
        }
        if (manualTip)
            paint();
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
        move(s.getStart().x, s.getStart().y, s.getEnd().x, s.getEnd().y);
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
        moveTips.clear();
        remark = null;
        manualList.clear();
        paint();
        return stepForEngine(x1, y1, x2, y2);
    }

    public List<String> getTacticList(boolean redGo) {
        List<String> list = new ArrayList<>();
        for (int i = 0; i < board.length; i++) {
            for (int j = 0; j < board[0].length; j++) {
                if (board[i][j] != ' ' && XiangqiUtils.isRed(board[i][j]) == redGo) {
                    for (int i2 = 0; i2 < board.length; i2++) {
                        for (int j2 = 0; j2 < board[0].length; j2++) {
                            if ((i != i2 || j != j2) && XiangqiUtils.canGo(board, i, j, i2, j2)) {
                                list.add(stepForEngine(j, i, j2, i2));
                            }
                        }
                    }
                }
            }
        }
        return list;
    }

    public static String stepForEngine(int x1, int y1, int x2, int y2) {
        StringBuffer sb = new StringBuffer();
        sb.append((char)('a' + x1));
        sb.append(9 - y1);
        sb.append((char)('a' + x2));
        sb.append(9 - y2);
        return sb.toString();
    }

    private void paint() {
        this.boardRender.paint(boardSize, this.board, prevStep, remark, stepTip,
                showMultiPV, moveTips, isReverse, showNumber, manualTip, manualList);
    }

    public void refresh() {
        paint();
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

    public void setManualTip(boolean f) {
        this.manualTip = f;
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
        XiangqiUtils.translate(this.board, sb, move, hasGo);
        return sb.toString();
    }

    /**
     * 翻译引擎着法(思考细节)
     * @param moveList
     * @return
     */
    public String translate(List<String> moveList) {
        for (int i = 0; i < board.length; i++) {
            System.arraycopy(board[i], 0, copyBoard[i], 0, copyBoard[i].length);
        }
        StringBuilder sb = new StringBuilder();
        for (String move : moveList) {
            char a = move.charAt(0), b = move.charAt(1), c = move.charAt(2), d = move.charAt(3);
            int fromJ = a - 'a', toJ = c - 'a';
            int fromI = 9 - Integer.parseInt(String.valueOf(b)), toI = 9 - Integer.parseInt(String.valueOf(d));
            XiangqiUtils.translate(copyBoard, sb, move, false);
            sb.append("  ");
            copyBoard[toI][toJ] = copyBoard[fromI][fromJ];
            copyBoard[fromI][fromJ] = ' ';
        }
        sb.delete(sb.length() - 2, sb.length());
        return sb.toString();
    }

    public char[][] getBoard() {
        return this.board;
    }

    public void autoFitSize(double width, double height, double position) {
        if (boardSize == BoardSize.AUTOFIT_BOARD) {
            if (Properties.getInstance().isShowChessNotation()) {
                width = width - 256;
            }
            position = Math.abs(position);
            width = width * position;
            height = height - 56;
            if (Properties.getInstance().isLinkShowInfo()) {
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
