package com.sojourners.chess.linker;

import com.sojourners.chess.board.ChessBoard;
import com.sojourners.chess.config.Properties;
import com.sojourners.chess.util.XiangqiUtils;
import com.sojourners.chess.yolo.OnnxModel;
import com.sojourners.chess.yolo.VinYolo5Model;
import com.sojourners.chess.yolo.Yolo11Model;

import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;


public abstract class AbstractGraphLinker implements GraphLinker, Runnable {

    /**
     * 扫描线程
     */
    private Thread thread;
    /**
     * 棋盘区域
     */
    private Rectangle boardPos;
    /**
     * 识别棋盘 暂存
     */
    private char[][] board2 = new char[10][9];

    private char[][] board1 = new char[10][9];

    private OnnxModel aiModel;

    private OnnxModel vinModel;

    private LinkerCallBack callBack;

    private Robot robot;

    private int count;

    private int failCount;

    /**
     * 待确认的新局面（连续两次识别相同才接受，过滤动画/残影等不稳定帧）
     */
    private char[][] stableBoard;

    /**
     * 是否已完成开局初始化（初始化阶段引擎局面未同步，不启用少子数量检查）
     */
    private boolean linkedInited = false;

    private int stableCount;

    private volatile boolean pause;

    private Properties prop;

    public AbstractGraphLinker(LinkerCallBack callBack) throws AWTException {
        this.callBack = callBack;
        robot = new Robot();
        this.count = 0;
        this.aiModel = new Yolo11Model();
        this.vinModel = new VinYolo5Model();
        this.prop = Properties.getInstance();
        this.pause = false;
    }

    /**
     * 开始连线
     */
    @Override
    public void start() {
        getTargetWindowId();
    }

    void scan() {
        this.thread = Thread.ofVirtual().unstarted(this);
        this.thread.start();
    }

    private boolean isSame(char[][] board1, char[][] board2) {
        if (board1 == null || board2 == null) {
            return false;
        }
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 9; j++) {
                if (board1[i][j] != board2[i][j]) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean isStable(char[][] board) {
        if (isSame(board, stableBoard)) {
            stableCount++;
        } else {
            stableBoard = new char[10][9];
            for (int i = 0; i < 10; i++) {
                System.arraycopy(board[i], 0, stableBoard[i], 0, 9);
            }
            stableCount = 1;
        }
        return stableCount >= 2;
    }

    public void pause() {
        this.pause = true;
    }
    public void resume() {
        this.pause = false;
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            if (!findBoardPosition()) {
                sleep(1000);
                continue;
            }
            if (!initChessBoard()) {
                sleep(1000);
                continue;
            }
            while (!Thread.currentThread().isInterrupted()) {
                sleep(prop.getLinkScanTime());
                if (!callBack.isThinking() && !pause) {

                    if (!findChessBoard(board2)) {
                        failCount++;
                        if (failCount > 10) {
                            // 连续识别失败，重新在整屏上定位棋盘并初始化局面
                            boardPos = null;
                            failCount = 0;
                            break;
                        }
                        continue;
                    }
                    failCount = 0;

                    boolean isReverse;
                    try {
                        isReverse = reverse(board2);
                    } catch (Exception e) {
                        e.printStackTrace();
                        continue;
                    }

                    if (isSame(board2, callBack.getEngineBoard())) {
                        stableCount = 0;
                        stableBoard = null;
                        continue;
                    }

                    Action action = compareBoard(board2, callBack.getEngineBoard(), isReverse, callBack.isWatchMode());

                    // 少子补棋：识别局面拼不成一步合法棋、且棋子数比引擎局面少时，
                    // 很可能是中局/残局漏识别（漏子与"被吃一子"数量相同，无法靠数量区分），
                    // 先用低置信度把漏掉的棋子补回来再判断
                    if (action == null && linkedInited
                            && countPieces(board2) < countPieces(callBack.getEngineBoard())) {
                        BufferedImage img2 = screenshot(false);
                        char[][] before = new char[10][9];
                        for (int i = 0; i < 10; i++) {
                            System.arraycopy(board2[i], 0, before[i], 0, 9);
                        }
                        boolean recovered = this.vinModel.completeChessBoard(img2, board2)
                                || this.aiModel.completeChessBoard(img2, board2)
                                || completeWithEngine(board2);
                        if (!recovered || !validateLinkBoard(board2)) {
                            for (int i = 0; i < 10; i++) {
                                System.arraycopy(before[i], 0, board2[i], 0, 9);
                            }
                        } else {
                            try {
                                isReverse = reverse(board2);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            action = compareBoard(board2, callBack.getEngineBoard(), isReverse, callBack.isWatchMode());
                        }
                    }

                    // 能明确配对成一步合法棋（对方走子/己方走子）时立即接受，保证响应速度；
                    // 变化不明确（动画帧/残影/漏子等）时，需要连续两次识别相同才接受
                    if (action == null || action.flag == 3 || action.flag == 4) {
                        if (!isStable(board2)) {
                            continue;
                        }
                        stableCount = 0;
                        stableBoard = null;
                    }

                    if (prop.isLinkAnimation() && needConfirm(board2, callBack.getEngineBoard(), action)) {
                        boolean f = false;
                        do {
                            char[][] tmp = board1;
                            board1 = board2;
                            board2 = tmp;

                            if (!findChessBoard(board2)) {
                                f = true;
                                break;
                            }

                            try {
                                isReverse = reverse(board2);
                            } catch (Exception e) {
                                e.printStackTrace();
                                f = true;
                                break;
                            }
                        } while (!isSame(board1, board2));

                        if (f) continue;

                        action = compareBoard(board2, callBack.getEngineBoard(), isReverse, callBack.isWatchMode());
                    }

                    if (action != null) {
                        System.out.println("action " + action);
                        if (action.flag == 1) {
                            callBack.linkerMove(action.x1, action.y1, action.x2, action.y2);

                        } else if (action.flag == 2) {
                            if (isReverse) {
                                action.y1 = 9 - action.y1;
                                action.y2 = 9 - action.y2;
                                action.x1 = 8 - action.x1;
                                action.x2 = 8 - action.x2;
                            }
                            autoClick(action.x1, action.y1, action.x2, action.y2);

                        } else if (action.flag == 3) {
                            break;
                        }
                        if (action.flag == 4) {
                            count++;
                            if (count > 9) {
                                break;
                            }
                        } else {
                            count = 0;
                        }
                    }

                }
            }
        }
    }

    class Action {
        int flag;
        int x1;
        int y1;
        int x2;
        int y2;
        public Action(int flag) {
            this.flag = flag;
        }
        public Action(int flag, int x1, int y1, int x2, int y2) {
            this.flag = flag;
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
        }

        @Override
        public String toString() {
            return "Action{" +
                    "flag=" + flag +
                    ", x1=" + x1 +
                    ", y1=" + y1 +
                    ", x2=" + x2 +
                    ", y2=" + y2 +
                    '}';
        }
    }

    private boolean needConfirm(char[][] linkBoard, char[][] engineBoard, Action action) {
        if (action == null) {
            return false;
        }
        if (action.flag == 3) {
            return true;
        }
        if (action.flag != 1 || !(linkBoard[action.y2][action.x2] == 'r' || linkBoard[action.y2][action.x2] == 'R' || linkBoard[action.y2][action.x2] == 'c' || linkBoard[action.y2][action.x2] == 'C') || !(engineBoard[action.y2][action.x2] == ' ')) {
            return false;
        }
        if (linkBoard[action.y2][action.x2] == 'r' || linkBoard[action.y2][action.x2] == 'R') {
            int x = -1, y = -1;
            if (action.x1 == action.x2) {
                x = action.x1;
                if (action.y2 > action.y1) {
                    y = action.y2 + 1;
                } else {
                    y = action.y2 - 1;
                }
            }
            if (action.y1 == action.y2) {
                y = action.y1;
                if (action.x2 > action.x1) {
                    x = action.x2 + 1;
                } else {
                    x = action.x2 - 1;
                }
            }
            if (x < 0 || x > 8 || y < 0 || y > 9 || engineBoard[y][x] != ' ' && XiangqiUtils.isRed(engineBoard[action.y1][action.x1]) == XiangqiUtils.isRed(engineBoard[y][x])) {
                return false;
            }
        }
        if (linkBoard[action.y2][action.x2] == 'c' || linkBoard[action.y2][action.x2] == 'C') {
            if (action.x1 == action.x2) {
                int x = action.x1, y;
                int p;
                if (action.y2 > action.y1) {
                    y = action.y2 + 1;
                    p = 1;
                } else {
                    y = action.y2 - 1;
                    p = -1;
                }
                if (y < 0 || y > 9) {
                    return false;
                }
                if (engineBoard[y][x] != ' ') {
                    for (int i = y + p; i >= 0 && i <= 9; i += p) {
                        if (engineBoard[i][x] != ' ' && XiangqiUtils.isRed(engineBoard[i][x]) == XiangqiUtils.isRed(engineBoard[action.y1][action.x1])) {
                            return false;
                        } else if (engineBoard[i][x] != ' ' && XiangqiUtils.isRed(engineBoard[i][x]) != XiangqiUtils.isRed(engineBoard[action.y1][action.x1])) {
                            return true;
                        }
                    }
                    return false;
                }
            }
            if (action.y1 == action.y2) {
                int x, y = action.y1;
                int p;
                if (action.x2 > action.x1) {
                    x = action.x2 + 1;
                    p = 1;
                } else {
                    x = action.x2 - 1;
                    p = -1;
                }
                if (x < 0 || x > 8 || y < 0 || y > 9) {
                    return false;
                }
                if (engineBoard[y][x] != ' ') {
                    for (int j = x + p; j >= 0 && j <= 8; j += p) {
                        if (engineBoard[y][j] != ' ' && XiangqiUtils.isRed(engineBoard[y][j]) == XiangqiUtils.isRed(engineBoard[action.y1][action.x1])) {
                            return false;
                        } else if (engineBoard[y][j] != ' ' && XiangqiUtils.isRed(engineBoard[y][j]) != XiangqiUtils.isRed(engineBoard[action.y1][action.x1])) {
                            return true;
                        }
                    }
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * 对比棋盘，计算出当前操作
     * flag： 1对方已走棋，需要同步到引擎
     *      2引擎已走棋，需要同步到目标平台
     *      3识别到新棋局
     *      4可能识别到新棋局
     * @param linkBoard
     * @param engineBoard
     * @param robotBlack
     * @return
     */
    private Action compareBoard(char[][] linkBoard, char[][] engineBoard, boolean robotBlack, boolean analysisMode) {
        int diff1 = 0, diff2 = 0, diff3 = 0;

        List<Point> diffList = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 9; j++) {
                if (linkBoard[i][j] != engineBoard[i][j]) {
                    diffList.add(new Point(i, j));
                    if (linkBoard[i][j] != ' ' && engineBoard[i][j] != ' ') {
                        diff1++;
                    } else if (linkBoard[i][j] != ' ' && engineBoard[i][j] == ' ') {
                        diff2++;
                    } else {
                        diff3++;
                    }
                }
            }
        }

        if (diff1 > 2 || diff2 >= 2 && diff3 > 2) {
            return new Action(3);
        }

        Action action = null;
        int flag = 0, sum = 0;
        Point from = null, to = null;
        for (int i = 0; i < diffList.size(); i++) {
            for (int j = i + 1; j < diffList.size(); j++) {
                Point p1 = diffList.get(i), p2 = diffList.get(j);
                boolean f = false;
                if (linkBoard[p1.x][p1.y] == engineBoard[p2.x][p2.y] && linkBoard[p1.x][p1.y] != ' ') {
                    if (linkBoard[p2.x][p2.y] == ' ' && engineBoard[p1.x][p1.y] == ' ') {
                        if (analysisMode || robotBlack && XiangqiUtils.isRed(linkBoard[p1.x][p1.y]) || !robotBlack && !XiangqiUtils.isRed(linkBoard[p1.x][p1.y])) {
                            flag = 1;
                            from = p2;
                            to = p1;
                            f = true;
                        } else if (robotBlack && !XiangqiUtils.isRed(linkBoard[p1.x][p1.y]) || !robotBlack && XiangqiUtils.isRed(linkBoard[p1.x][p1.y])) {
                            flag = 2;
                            from = p1;
                            to = p2;
                            f = true;
                        }
                    }
                    if (linkBoard[p2.x][p2.y] == ' ' && engineBoard[p1.x][p1.y] != ' ' && XiangqiUtils.isRed(linkBoard[p1.x][p1.y]) != XiangqiUtils.isRed(engineBoard[p1.x][p1.y])) {
                        flag = 1;
                        from = p2;
                        to = p1;
                        f = true;
                    }
                    if (!analysisMode && engineBoard[p1.x][p1.y] == ' ' && linkBoard[p2.x][p2.y] != ' ' && XiangqiUtils.isRed(engineBoard[p2.x][p2.y]) != XiangqiUtils.isRed(linkBoard[p2.x][p2.y])) {
                        flag = 2;
                        from = p1;
                        to = p2;
                        f = true;
                    }
                }
                if (linkBoard[p2.x][p2.y] == engineBoard[p1.x][p1.y] && linkBoard[p2.x][p2.y] != ' ') {
                    if (linkBoard[p1.x][p1.y] == ' ' && engineBoard[p2.x][p2.y] == ' ') {
                        if (analysisMode || robotBlack && XiangqiUtils.isRed(linkBoard[p2.x][p2.y]) || !robotBlack && !XiangqiUtils.isRed(linkBoard[p2.x][p2.y])) {
                            flag = 1;
                            from = p1;
                            to = p2;
                            f = true;
                        } else if (robotBlack && !XiangqiUtils.isRed(linkBoard[p2.x][p2.y]) || !robotBlack && XiangqiUtils.isRed(linkBoard[p2.x][p2.y])) {
                            flag = 2;
                            from = p2;
                            to = p1;
                            f = true;
                        }
                    }
                    if (linkBoard[p1.x][p1.y] == ' ' && engineBoard[p2.x][p2.y] != ' ' && XiangqiUtils.isRed(linkBoard[p2.x][p2.y]) != XiangqiUtils.isRed(engineBoard[p2.x][p2.y])) {
                        flag = 1;
                        from = p1;
                        to = p2;
                        f = true;
                    }
                    if (!analysisMode && engineBoard[p2.x][p2.y] == ' ' && linkBoard[p1.x][p1.y] != ' ' && XiangqiUtils.isRed(engineBoard[p1.x][p1.y]) != XiangqiUtils.isRed(linkBoard[p1.x][p1.y])) {
                        flag = 2;
                        from = p2;
                        to = p1;
                        f = true;
                    }
                }
                if (f && (flag == 1 && XiangqiUtils.canGo(engineBoard, from.x, from.y, to.x, to.y) || flag == 2 && XiangqiUtils.canGo(linkBoard, from.x, from.y, to.x, to.y))) {
                    sum++;
                    action = new Action(flag, from.y, from.x, to.y, to.x);
                }
            }
        }

        if (sum == 1) {
            return action;
        }

//        if (diff1 + diff2 + diff3 == 1) {
//            return new Action(3);
//        }

        if (diff1 + diff2 + diff3 > 2) {
            return new Action(4);
        }

        return null;
    }

    void sleep(long time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 前台截图
     * @param windowPos
     * @return
     */
    public BufferedImage screenshotByFront(Rectangle windowPos) {
        if (windowPos.width == 0 || windowPos.height == 0) {
            return null;
        }
        return robot.createScreenCapture(windowPos);
    }

    /**
     * 前台点击
     * @param windowPos
     * @param p1
     * @param p2
     */
    @Override
    public void mouseClickByFront(Rectangle windowPos, Point p1, Point p2) {

        Point mouse = MouseInfo.getPointerInfo().getLocation();

        robot.mouseMove(windowPos.x + p1.x, windowPos.y+ p1.y);

        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        if (prop.getMouseClickDelay() > 0) {
            robot.delay(prop.getMouseClickDelay());
        }
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);

        if (prop.getMouseMoveDelay() > 0) {
            robot.delay(prop.getMouseMoveDelay());
        }
        robot.mouseMove(windowPos.x + p2.x, windowPos.y + p2.y);

        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        if (prop.getMouseClickDelay() > 0) {
            robot.delay(prop.getMouseClickDelay());
        }
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);

        robot.mouseMove((int) mouse.getX(), (int) mouse.getY());

    }

    /**
     * 寻找棋盘区域
     * @return
     */
    boolean findBoardPosition() {
        BufferedImage img = screenshot(true);
        this.boardPos = this.aiModel.findBoardPosition(img);
        if (this.boardPos == null) {
            // 主模型识别不到棋盘时（如JJ象棋深色棋盘），尝试VinXiangQi模型
            this.boardPos = this.vinModel.findBoardPosition(img);
        }
        return this.boardPos != null;
    }

    /**
     * 截图
     * @param fullScreen
     * @return
     */
    BufferedImage screenshot(boolean fullScreen) {
        if (prop.isLinkBackMode()) {
            // 后台模式：统一截全窗口，再用 boardPos 裁剪（与 findBoardPosition 一致），
            // 避免 capture 内部 DPI 缩放导致裁剪区域错位/黑图
            BufferedImage img = screenshotByBack(null);
            if (!fullScreen && img != null && boardPos != null) {
                int x = Math.max(0, boardPos.x);
                int y = Math.max(0, boardPos.y);
                int w = Math.min(boardPos.width, img.getWidth() - x);
                int h = Math.min(boardPos.height, img.getHeight() - y);
                if (w > 0 && h > 0) {
                    img = img.getSubimage(x, y, w, h);
                }
            }
            return img;

        } else {
            Rectangle pos = getTargetWindowPosition();
            if (!fullScreen) {
                pos.setLocation(pos.x + boardPos.x, pos.y + boardPos.y);
                pos.setSize(boardPos.width, boardPos.height);
            }
            BufferedImage img = screenshotByFront(pos);
            return img;
        }
    }


    private boolean findChessBoard(char[][] board) {
        // 截图
        BufferedImage img = screenshot(false);
        // ai识别棋盘棋子
        if (this.aiModel.findChessBoard(img, board) && validateLinkBoard(board) && pieceCountOK(board)) {
            return true;
        }
        // 主模型识别失败时，尝试VinXiangQi模型（支持JJ象棋等深色棋盘）
        if (this.vinModel.findChessBoard(img, board) && validateLinkBoard(board) && pieceCountOK(board)) {
            return true;
        }
        // 少子补棋：识别出合法局面但棋子数明显偏少时（相对引擎局面少2子以上），
        // 用更低置信度把漏掉的棋子补进空格，避免"少子局面"被当作正确局面接受
        if (validateLinkBoard(board)) {
            BufferedImage img2 = screenshot(false);
            if (this.vinModel.completeChessBoard(img2, board) && validateLinkBoard(board) && pieceCountOK(board)) {
                return true;
            }
            if (this.aiModel.completeChessBoard(img2, board) && validateLinkBoard(board) && pieceCountOK(board)) {
                return true;
            }
            // 引擎局面补棋：模型补不上时，直接用引擎局面补缺失棋子（残局漏识别主方案）
            if (completeWithEngine(board) && validateLinkBoard(board) && pieceCountOK(board)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 棋子数量连续性检查：一轮走棋最多只会少一子（被吃），
     * 识别结果比引擎局面少2子以上说明存在漏识别（少子）。
     */
    private boolean pieceCountOK(char[][] board) {
        if (!linkedInited) {
            return true;
        }
        char[][] engineBoard = callBack.getEngineBoard();
        if (engineBoard == null) {
            return true;
        }
        int cur = countPieces(board);
        int eng = countPieces(engineBoard);
        // 深色棋盘中局/残局偶发漏识别 1-2 子，放宽到 eng-2 容错；
        // 少 2 子以上的情况会在少子补棋流程用引擎局面补回
        return cur >= eng - 2;
    }

    /**
     * 引擎局面补棋：识别结果比引擎局面少子时，把引擎局面中有而识别结果
     * 缺失的棋子补回空格（前提：补回后局面合法且棋子数增加）。
     * 引擎局面由连线过程维护，中局/残局时它是当前最可信的局面参照。
     */
    private boolean completeWithEngine(char[][] board) {
        try {
            char[][] engineBoard = callBack.getEngineBoard();
            if (engineBoard == null) {
                return false;
            }
            char[][] merged = new char[10][9];
            for (int i = 0; i < 10; i++) {
                System.arraycopy(board[i], 0, merged[i], 0, 9);
            }
            int added = 0;
            for (int i = 0; i < 10; i++) {
                for (int j = 0; j < 9; j++) {
                    if (engineBoard[i][j] != ' ' && merged[i][j] == ' ') {
                        merged[i][j] = engineBoard[i][j];
                        added++;
                    }
                }
            }
            if (added > 0 && countPieces(merged) > countPieces(board)
                    && XiangqiUtils.validateChessBoard(merged)) {
                for (int i = 0; i < 10; i++) {
                    System.arraycopy(merged[i], 0, board[i], 0, 9);
                }
                return true;
            }
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private int countPieces(char[][] board) {
        int n = 0;
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 9; j++) {
                if (board[i][j] != ' ') {
                    n++;
                }
            }
        }
        return n;
    }

    /**
     * 连线专用的局面校验：在基础校验之外，按红黑分侧检查象/兵是否越过河界，
     * 过滤 JJ象棋 等平台的"飞子残影"被误识别为真实棋子的情况
     */
    private boolean validateLinkBoard(char[][] board) {
        if (!XiangqiUtils.validateChessBoard(board)) {
            return false;
        }
        int redKingRow = -1, blackKingRow = -1;
        for (int i = 0; i < 10; i++) {
            for (int j = 3; j < 6; j++) {
                if (board[i][j] == 'K') {
                    redKingRow = i;
                } else if (board[i][j] == 'k') {
                    blackKingRow = i;
                }
            }
        }
        if (redKingRow == -1 || blackKingRow == -1) {
            return false;
        }
        boolean redTop = redKingRow <= 4;
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 9; j++) {
                char c = board[i][j];
                if (c == 'B' || c == 'b') {
                    boolean redSide = c == 'B';
                    boolean onTopHalf = i <= 4;
                    boolean ok = redSide ? (redTop ? onTopHalf : !onTopHalf) : (redTop ? !onTopHalf : onTopHalf);
                    if (!ok) {
                        return false;
                    }
                } else if (c == 'P') {
                    boolean ok = redTop ? i >= 3 : i <= 6;
                    if (!ok) {
                        return false;
                    }
                } else if (c == 'p') {
                    boolean ok = redTop ? i <= 6 : i >= 3;
                    if (!ok) {
                        return false;
                    }
                }
            }
        }
        return true;
    }
    private boolean reverse(char[][] board) throws Exception {
        // 是否翻转
        int rowRedKing = -1, rowBlackKing = -1;
        for (int i = 0; i < 10; i++) {
            for (int j = 3; j < 6; j++) {
                if (board[i][j] == 'k') {
                    rowBlackKing = i;
                } else if (board[i][j] == 'K') {
                    rowRedKing = i;
                }
            }
        }
        if (rowBlackKing == -1 && rowRedKing == -1) {
            throw new Exception("find king failed.");
        }
        boolean isReverse = rowRedKing >= 0 && rowRedKing <= 2 || rowBlackKing >= 7 && rowBlackKing <= 9;
        if (isReverse) {
            for (int i = 0; i < 5; i++) {
                for (int j = 0; j < 9; j++) {
                    char tmp = board[i][j];
                    board[i][j] = board[9 - i][8 - j];
                    board[9 - i][8 - j] = tmp;
                }
            }
        }
        return isReverse;
    }

    /**
     * 初始化棋盘局面
     * @return
     */
    private boolean initChessBoard() {
        linkedInited = false;
        // 连续两次识别到相同且合法的开局局面，避免单帧错误进入引擎
        char[][] first = new char[10][9];
        if (!findChessBoard(first)) {
            return false;
        }
        char[][] second = new char[10][9];
        if (!findChessBoard(second)) {
            return false;
        }
        if (!isSame(first, second)) {
            return false;
        }
        for (int i = 0; i < 10; i++) {
            System.arraycopy(second[i], 0, board2[i], 0, 9);
        }

        boolean isReverse = false;
        try {
            isReverse = reverse(board2);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        // 是否红走：开局局面（双方将帅在位、棋子数充足）一律红先，
        // 不再依赖 FEN 精确匹配标准开局（深色棋盘识别偶发漏子/误识别会破坏 FEN）
        boolean redGo = isOpeningBoard(board2) ? true : !isReverse;
        String fenCode = ChessBoard.fenCode(board2, null);
        fenCode = ChessBoard.fenCode(board2, redGo);
        // 回调，初始化棋盘
        callBack.linkerInitChessBoard(fenCode, isReverse);
        linkedInited = true;
        return true;
    }

    /**
     * 判断是否为开局局面：双方将帅各一，且总棋子数 >= 26（容错漏识别）
     */
    private boolean isOpeningBoard(char[][] board) {
        int redKing = 0, blackKing = 0, pieces = 0;
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 9; j++) {
                char c = board[i][j];
                if (c == 'K') {
                    redKing++;
                } else if (c == 'k') {
                    blackKing++;
                }
                if (c != ' ') {
                    pieces++;
                }
            }
        }
        return redKing == 1 && blackKing == 1 && pieces >= 26;
    }

    /**
     * 自动点击走棋
     * @param x1
     * @param y1
     * @param x2
     * @param y2
     */
    public void autoClick(int x1, int y1, int x2, int y2) {

        Point p1 = getPosition(x1, y1);
        Point p2 = getPosition(x2, y2);
        if (prop.isLinkBackMode()) {
            mouseClickByBack(p1, p2);
        } else {
            Rectangle windowPos = getTargetWindowPosition();
            mouseClickByFront(windowPos, p1, p2);
        }
    }
    private Point getPosition(int x, int y) {
        double pieceWith = boardPos.width / (8 + OnnxModel.PADDING * 2);
        double pieceHeight = boardPos.height / (9 + OnnxModel.PADDING * 2);
        Point p = new Point((int) (boardPos.x + pieceWith * OnnxModel.PADDING + (x * pieceWith)),
                (int) (boardPos.y + pieceHeight * OnnxModel.PADDING + (y * pieceHeight)));
        if (x == 0) {
            p.x += 0.2 * pieceWith;
        } else if (x == 8) {
            p.x -= 0.2 * pieceWith;
        }
        if (y == 0) {
            p.y += 0.2 * pieceHeight;
        } else if (y == 9) {
            p.y -= 0.2 * pieceHeight;
        }
        return p;
    }

    /**
     * 停止连线
     */
    @Override
    public void stop() {
        if (thread != null && thread.isAlive()) {
            thread.interrupt();
        }
    }

    // find chess board from image
    public char[][] findChessBoard(BufferedImage img) {
        char[][] tmp = new char[10][9];
        if (this.aiModel.findChessBoard(img, tmp)) {
            return tmp;
        }
        if (this.vinModel.findChessBoard(img, tmp)) {
            return tmp;
        }
        return null;
    }
}
