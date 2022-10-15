package com.sojourners.chess.board;

import com.sojourners.chess.media.SoundPlayer;
import com.sojourners.chess.util.MathUtils;
import com.sojourners.chess.util.PathUtils;
import com.sojourners.chess.util.StringUtils;
import com.sojourners.chess.util.XiangqiUtils;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.transform.Rotate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 棋盘
 */
public class ChessBoard {

    private Canvas canvas;
    private GraphicsContext gc;

    private static volatile char[][] board = new char[10][9];

    private static char[][] copyBoard = new char[10][9];

    private BoardStyle style;

    private boolean stepTip;

    private boolean stepSound;

    private static Image bgImage;

    private static SoundPlayer sound;

    private Font font;
    private int fontSize;

    private static Map<Character, String> map = new HashMap<>(32);

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

        bgImage = new Image(ChessBoard.class.getResourceAsStream("/image/BOARD.JPG"));

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

    public class Point {
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

    public enum BoardStyle {
        BIG_BOARD,
        MIDDLE_BOARD,
        SMALL_BOARD;
    }

    public ChessBoard(Canvas canvas, BoardStyle bs, boolean stepTip, boolean stepSound, String fenCode) {
        this.canvas = canvas;
        this.gc = canvas.getGraphicsContext2D();
        this.stepTip = stepTip;
        this.stepSound = stepSound;
        // 设置局面
        setNewBoard(fenCode);
        // 设置棋盘大小
        setStyle(bs);
        // 默认不翻转
        isReverse = false;

        this.paint();
    }

    private void setNewBoard(String fenCode) {
        if (StringUtils.isEmpty(fenCode)) {
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
        } else {
            setBoard(fenCode);
        }
    }

    private void setStyle(BoardStyle bs) {
        this.style = bs;
        /**
         * 棋子字体大小
         */
        switch (style) {
            case BIG_BOARD: { fontSize = 36; break; }
            case MIDDLE_BOARD: { fontSize = 32; break; }
            case SMALL_BOARD: { fontSize = 26; break; }
            default: { fontSize = 32; break; }
        }
        font = Font.loadFont(getClass().getResourceAsStream("/font/chessman.ttf"), fontSize);
    }

    /**
     * 棋盘边距
     * @return
     */
    private int getPadding() {
        switch (style) {
            case BIG_BOARD: {
                return 12;
            }
            case MIDDLE_BOARD: {
                return 10;
            }
            case SMALL_BOARD: {
                return 8;
            }
            default: {
                return 10;
            }
        }
    }

    /**
     * 棋子大小
     * @return
     */
    private int getPieceSize() {
        switch (style) {
            case BIG_BOARD: {
                return 72;
            }
            case MIDDLE_BOARD: {
                return 64;
            }
            case SMALL_BOARD: {
                return 48;
            }
            default: {
                return 64;
            }
        }
    }

    public String mouseClick(int x, int y, boolean canRedGo, boolean canBlackGo) {
        int padding = getPadding();
        int piece = getPieceSize();
        int i = (x - padding) / piece;
        int j = (y - padding) / piece;
        j = getReverseY(j);

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
            if (fenCode.indexOf("k") > fenCode.indexOf("K")) {
                for (int i = 0; i < arr.length / 2; i++) {
                    String tmp = arr[i];
                    arr[i] = arr[arr.length - 1 - i];
                    arr[arr.length - 1 - i] = tmp;
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

    /**
     * 棋盘外矩形线条宽度
     * @return
     */
    private double getOutRectWidth() {
        switch (style) {
            case BIG_BOARD: {
                return 2.4;
            }
            case MIDDLE_BOARD: {
                return 2.2;
            }
            case SMALL_BOARD: {
                return 1.2;
            }
            default: {
                return 2.2;
            }
        }
    }

    /**
     * 棋盘内矩形线条宽度
     * @return
     */
    private double getInnerRectWidth() {
        switch (style) {
            case BIG_BOARD: {
                return 1;
            }
            case MIDDLE_BOARD: {
                return 0.8;
            }
            case SMALL_BOARD: {
                return 0.6;
            }
            default: {
                return 0.8;
            }
        }
    }

    /**
     * 获取线路序号字体大小
     * @return
     */
    private double getNumberSize() {
        switch (style) {
            case BIG_BOARD: {
                return 16;
            }
            case MIDDLE_BOARD: {
                return 14;
            }
            case SMALL_BOARD: {
                return 11;
            }
            default: {
                return 14;
            }
        }
    }

    /**
     * 获取楚河汉界字体大小
     * @return
     */
    private double getCenterTextSize() {
        switch (style) {
            case BIG_BOARD: {
                return 30;
            }
            case MIDDLE_BOARD: {
                return 26;
            }
            case SMALL_BOARD: {
                return 20;
            }
            default: {
                return 26;
            }
        }
    }

    private void paint() {
        int padding = getPadding();
        int piece = getPieceSize();
        int pos = padding + piece / 2;
        canvas.setWidth(2 * padding + piece * 9);
        canvas.setHeight(2 * padding + piece * 10);
        // 绘制背景图片
        gc.drawImage(bgImage, 0, 0, canvas.getWidth(), canvas.getHeight());
        // 棋盘竖线横线
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(getOutRectWidth());
        gc.setGlobalAlpha(0.75);
        gc.strokeRect(pos - padding / 2, pos - padding / 2, piece * 8 + padding, piece * 9 + padding);
        gc.setGlobalAlpha(1);
        gc.setLineWidth(getInnerRectWidth());
        gc.strokeRect(pos, pos, piece * 8, piece * 9);
        for (int i = 1; i < 9; i++) {
            gc.strokeLine(pos, pos + piece * i, pos + piece * 8, pos + piece * i);
        }
        for (int i = 1; i < 8; i++) {
            gc.strokeLine(pos + piece * i, pos, pos + piece * i, pos + piece * 4);
            gc.strokeLine(pos + piece * i, pos + piece * 5, pos + piece * i, pos + piece * 9);
        }
        // 九宫斜线
        gc.strokeLine(pos + piece * 3, pos, pos + piece * 5, pos + piece * 2);
        gc.strokeLine(pos + piece * 3, pos + piece * 2, pos + piece * 5, pos);
        gc.strokeLine(pos + piece * 3, pos + piece * 9, pos + piece * 5, pos + piece * 7);
        gc.strokeLine(pos + piece * 3, pos + piece * 7, pos + piece * 5, pos + piece * 9);
        // 炮兵位置记号
        for (int i = 0; i < 9; i += 2) {
            String style = i == 0 ? "r" : (i == 8 ? "l" : "lr");
            drawStarPos(pos + piece * i, pos + piece * 3, piece, style);
            drawStarPos(pos + piece * i, pos + piece * 6, piece, style);
        }
        for (int i = 1; i < 9; i += 6) {
            drawStarPos(pos + piece * i, pos + piece * 2, piece, "lr");
            drawStarPos(pos + piece * i, pos + piece * 7, piece, "lr");
        }
        // 绘制线路序号
        double numberSize = getNumberSize();
        gc.setFont(Font.font(numberSize));
        gc.setFill(Color.BLACK);
        for (int i = 0; i < 9; i++) {
            // 黑方
            char number = (char) ('１' + i);
            double xTop = pos + i * piece - numberSize / 2, xBottom = pos + (8 - i) * piece - numberSize / 2;
            double yTop = pos - piece / 4, yBottom = pos + 9 * piece + piece / 2.3;
            gc.fillText(String.valueOf(number), isReverse ? xBottom : xTop, isReverse ? yBottom : yTop);
            // 红方
            gc.fillText(map.get(number), isReverse ? xTop : xBottom, isReverse ? yTop : yBottom);
        }
        // 绘制楚河汉界
        double centerTextSize = getCenterTextSize();
        gc.setFont(Font.font(centerTextSize));
        gc.setGlobalAlpha(0.55);
        gc.fillText("楚", pos + 2 * piece - centerTextSize, pos + 4.5 * piece + centerTextSize / 3.6);
        gc.fillText("河", pos + 3 * piece - centerTextSize, pos + 4.5 * piece + centerTextSize / 3.6);
        gc.fillText("汉", pos + 5 * piece, pos + 4.5 * piece + centerTextSize / 3.6);
        gc.fillText("界", pos + 6 * piece, pos + 4.5 * piece + centerTextSize / 3.6);
        gc.setGlobalAlpha(1);
        // 上一步走棋记号
        if (prevStep != null) {
            drawStepRect(pos + piece * prevStep.first.x, pos + piece * getReverseY(prevStep.first.y), piece, Color.web("#bf242a"));
            drawStepRect(pos + piece * prevStep.second.x, pos + piece * getReverseY(prevStep.second.y), piece, Color.web("#bf242a"));
        }
        // 已选择棋子记号
        if (remark != null) {
            drawStepRect(pos + piece * remark.x, pos + piece * getReverseY(remark.y), piece, Color.web("#0000FF"));
        }
        // 绘制棋子
        for (int i = 0; i < board.length; i++) {
            for (int j = 0; j < board[0].length; j++) {
                drawPiece(pos + piece * j, pos + piece * getReverseY(i), piece, board[i][j]);
            }
        }
        // 绘制棋步提示
        if (stepTip && tipFirst != null) {
            drawStepTip(pos + piece * tipFirst.first.x, pos + piece * getReverseY(tipFirst.first.y),
                    pos + piece * tipFirst.second.x, pos + piece * getReverseY(tipFirst.second.y),
                    piece, Color.PURPLE);
        }
        if (stepTip && tipSecond != null) {
            drawStepTip(pos + piece * tipSecond.first.x, pos + piece * getReverseY(tipSecond.first.y),
                    pos + piece * tipSecond.second.x, pos + piece * getReverseY(tipSecond.second.y),
                    piece, Color.GREEN);
        }
    }

    private int getReverseY(int y) {
        return isReverse ? (9 - y) : y;
    }

    private void drawStepTip(int x1, int y1, int x2, int y2, int w, Color color) {
        gc.save();

        double angle = MathUtils.calculateAngle(x1, y1, x2, y2);
        Rotate r = new Rotate(angle, x1, y1);
        gc.setTransform(r.getMxx(), r.getMyx(), r.getMxy(), r.getMyy(), r.getTx(), r.getTy());

        gc.setGlobalAlpha(0.42);
        gc.setFill(color);

        int len = (int) MathUtils.calculateDistance(x1, y1, x2, y2);
        x2 = x1 - len;

        x1 -= w / 4;
        double offY = w / 12.5, offX = w / 4.5, h = w / 6.5;
        gc.fillPolygon(new double[]{x1, x2 + offX, x2 + offX + h / 2, x2, x2 + offX + h / 2, x2 + offX, x1},
                new double[]{y1 - offY, y1 - offY, y1 - offY - h, y1, y1 + offY + h, y1 + offY, y1 + offY},
                7);

        gc.restore();
    }

    /**
     * 棋步标识矩形线条宽度
     * @return
     */
    private double getStepRectWitdh() {

        switch (style) {
            case BIG_BOARD: {
                return 2.8;
            }
            case MIDDLE_BOARD: {
                return 2.5;
            }
            case SMALL_BOARD: {
                return 1.5;
            }
            default: {
                return 2.5;
            }
        }

    }

    private void drawStepRect(int x, int y, int w, Color color) {
        double len = w / 1.08;
        gc.setLineWidth(getStepRectWitdh());
        gc.setStroke(color);
        gc.strokePolyline(new double[]{x - len / 2 + len / 6, x - len / 2, x - len / 2},
                new double[]{y - len / 2, y - len / 2, y - len / 2 + len / 6},
                3);
        gc.strokePolyline(new double[]{x - len / 2 + len / 6, x - len / 2, x - len / 2},
                new double[]{y + len / 2, y + len / 2, y + len / 2 - len / 6},
                3);
        gc.strokePolyline(new double[]{x + len / 2 - len / 6, x + len / 2, x + len / 2},
                new double[]{y - len / 2, y - len / 2, y - len / 2 + len / 6},
                3);
        gc.strokePolyline(new double[]{x + len / 2 - len / 6, x + len / 2, x + len / 2},
                new double[]{y + len / 2, y + len / 2, y + len / 2 - len / 6},
                3);

    }

    /**
     * 棋子外圈线条宽度
     * @return
     */
    private double getPieceBw() {
        switch (style) {
            case BIG_BOARD: {
                return 4.5;
            }
            case MIDDLE_BOARD: {
                return 4;
            }
            case SMALL_BOARD: {
                return 3;
            }
            default: {
                return 4;
            }
        }
    }

    /**
     * 棋子内圈线条宽度
     * @return
     */
    private double getPieceSw() {
        switch (style) {
            case BIG_BOARD: {
                return 1;
            }
            case MIDDLE_BOARD: {
                return 0.8;
            }
            case SMALL_BOARD: {
                return 0.6;
            }
            default: {
                return 0.8;
            }
        }
    }
    private void drawPiece(int x, int y, int w, char code) {
        String word = map.get(code);
        if (word != null) {
            int r = (w - w / 10) / 2;
            double bW = getPieceBw();
            double sW = getPieceSw();

            Color color = Color.web(XiangqiUtils.isRed(code) ? "#AD1A02" : "#167B7F");
            gc.setFill(Color.WHITE);
            gc.fillOval(x - r, y - r, 2 * r, 2 * r);
            gc.setStroke(color);
            gc.setLineWidth(bW);
            gc.strokeOval(x - r, y - r, 2 * r, 2 * r);
            gc.setLineWidth(sW);
            gc.strokeOval(x - r + bW * 1.8, y - r + bW * 1.8, 2 * (r - bW * 1.8), 2 * (r - bW * 1.8));
            gc.setFill(color);
            gc.setFont(font);
            gc.fillText(word, x - fontSize / 2, y + fontSize / 2 - fontSize / 5.5);
        }
    }

    private void drawStarPos(int x, int y, int w, String style) {
        int offset = w / 16;
        int len = w / 6;
        if (style.contains("l")) {
            gc.strokePolyline(new double[]{x - offset - len, x - offset, x - offset},
                    new double[]{y - offset, y - offset, y - offset - len}, 3);

            gc.strokePolyline(new double[]{x - offset - len, x - offset, x - offset},
                    new double[]{y + offset, y + offset, y + offset + len}, 3);


        }
        if (style.contains("r")) {
            gc.strokePolyline(new double[]{x + offset + len, x + offset, x + offset},
                    new double[]{y - offset, y - offset, y - offset - len}, 3);

            gc.strokePolyline(new double[]{x + offset + len, x + offset, x + offset},
                    new double[]{y + offset, y + offset, y + offset + len}, 3);

        }
    }

    /**
     * 设置翻转
     * @param isReverse
     */
    public void reverse(boolean isReverse) {
        if (this.isReverse !=  isReverse) {
            this.isReverse = isReverse;
            paint();
        }
    }

    /**
     * 设置棋盘样式
     * @param bs
     */
    public void setBoardStyle(BoardStyle bs) {
        setStyle(bs);
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
        sb.append(map.get(hasGo ? board[toI][toJ] : board[fromI][fromJ]));
        boolean isRed = XiangqiUtils.isRed(hasGo ? board[toI][toJ] : board[fromI][fromJ]);
        char pos = getPos(fromJ, isRed);
        sb.append(isRed ? map.get(pos) : pos);
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
            return (char) (isReverse ? '０' + j + 1 : '０' + 9 - j);
        } else {
            return (char) (!isReverse ? '０' + j + 1 : '０' + 9 - j);
        }
    }
}
