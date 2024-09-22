package com.sojourners.chess.controller;

import com.sojourners.chess.App;
import com.sojourners.chess.board.BaseBoardRender;
import com.sojourners.chess.board.ChessBoard;
import com.sojourners.chess.board.CustomBoardRender;
import com.sojourners.chess.board.DefaultBoardRender;
import com.sojourners.chess.config.Properties;
import com.sojourners.chess.util.DialogUtils;
import com.sojourners.chess.util.XiangqiUtils;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.control.RadioButton;

public class EditChessBoardController {

    @FXML
    private Canvas canvas;
    @FXML
    private Canvas demoCanvas;

    private char[][] board;
    private char[][] demoBoard;

    private BaseBoardRender boardRender;
    private BaseBoardRender demoBoardRender;

    private ChessBoard.Point remark;
    private ChessBoard.Point demoRemark;

    @FXML
    private RadioButton blackFirstButton;
    @FXML
    private RadioButton redFirstButton;

    private ChessBoard.BoardSize boardSize;

    private String fenCode;

    public String getFenCode() {
        return fenCode;
    }

    @FXML
    public void canvasClick(MouseEvent event) {
        int x = (int) event.getX();
        int y = (int) event.getY();
        int padding = boardRender.getPadding(this.boardSize);
        int piece = boardRender.getPieceSize(this.boardSize);
        int j = (x - padding) / piece;
        int i = (y - padding) / piece;

        if (j < 0 || j > 8 || i < 0 || i > 9) {
            return;
        }

        if (event.getButton() == MouseButton.PRIMARY) {
            if (board[i][j] == ' ') {
                if (remark != null) {
                    board[i][j] = board[remark.getY()][remark.getX()];
                    board[remark.getY()][remark.getX()] = ' ';
                    remark = null;
                } else if (demoRemark != null) {
                    board[i][j] = demoBoard[demoRemark.getY()][demoRemark.getX()];
                }
            } else {
                remark = new ChessBoard.Point(j, i);
                demoRemark = null;
            }
        } else if (event.getButton() == MouseButton.SECONDARY) {
            if (board[i][j] != ' ') {
                board[i][j] = ' ';
                remark = null;
                demoRemark = null;
            }
        }

        paint();
    }

    @FXML
    public void demoCanvasClick(MouseEvent event) {
        int x = (int) event.getX();
        int y = (int) event.getY();
        int padding = demoBoardRender.getPadding(this.boardSize);
        int piece = demoBoardRender.getPieceSize(this.boardSize);
        int j = (x - padding) / piece;
        int i = (y - padding) / piece;

        if (j < 0 || j > 1 || i < 0 || i > 6) {
            return;
        }

        demoRemark = new ChessBoard.Point(j, i);
        remark = null;

        paint();
    }

    @FXML
    void cleanChessBoard(MouseEvent event) {
        for (int i = 0; i < board.length; i++) {
            for (int j = 0; j < board[0].length; j++) {
                board[i][j] = ' ';
            }
        }
        paint();
    }

    @FXML
    void initChessBoard(MouseEvent event) {
        ChessBoard.initChessBoard(board);
        paint();
    }

    @FXML
    void lrReverse(MouseEvent event) {
        for (int i = 0; i < board.length; i++) {
            for (int j = 0; j < board[0].length / 2; j++) {
                char tmp = board[i][j];
                board[i][j] = board[i][board[0].length - 1 - j];
                board[i][board[0].length - 1 - j] = tmp;
            }
        }
        paint();
    }

    @FXML
    void udReverse(MouseEvent event) {
        for (int i = 0; i < board.length / 2; i++) {
            char[] tmp = board[i];
            board[i] = board[board.length - 1 - i];
            board[board.length - 1 - i] = tmp;
        }
        paint();
    }


    @FXML
    void cancelButtonClick(MouseEvent event) {
        fenCode = null;
        App.closeEditChessBoard();
    }

    @FXML
    void okButtonClick(MouseEvent event) {
        if (!XiangqiUtils.validateChessBoard(board) && !DialogUtils.showConfirmDialog("提示", "检测到局面不合法，可能会导致引擎退出或者崩溃，是否继续？")) {
            return;
        }
        fenCode = ChessBoard.fenCode(board, redFirstButton.isSelected());
        App.closeEditChessBoard();
    }

    public void paint() {
        this.boardRender.paint(boardSize, board, null, remark, false, null, null, false, false);
        this.demoBoardRender.paintDemoBoard(boardSize, demoBoard, demoRemark);
    }

    public void setFirstMover(boolean redFirst) {
        if (redFirst) {
            redFirstButton.setSelected(true);
        } else {
            blackFirstButton.setSelected(true);
        }
    }

    public void setBoard(char[][] board, boolean isReverse) {
        int h = board.length;
        int w = board[0].length;
        this.board = new char[h][w];
        for (int i = 0; i < h; i++) {
            for (int j = 0; j < w; j++) {
                this.board[i][j] = isReverse ? board[h - 1 - i][w - 1 - j] : board[i][j];
            }
        }
        demoBoard = new char[][]{
                new char[]{'R', 'r'},
                new char[]{'N', 'n'},
                new char[]{'B', 'b'},
                new char[]{'A', 'a'},
                new char[]{'C', 'c'},
                new char[]{'P', 'p'},
                new char[]{'K', 'k'}
        };

        paint();
    }

    public void initialize() {
        this.boardRender = Properties.getInstance().getBoardStyle() == ChessBoard.BoardStyle.CUSTOM ? new CustomBoardRender(canvas) : new DefaultBoardRender(canvas);
        this.demoBoardRender = Properties.getInstance().getBoardStyle() == ChessBoard.BoardStyle.CUSTOM ? new CustomBoardRender(demoCanvas) : new DefaultBoardRender(demoCanvas);
        this.boardSize = ChessBoard.BoardSize.BIG_BOARD;

        ToggleGroup group = new ToggleGroup();
        redFirstButton.setToggleGroup(group);
        blackFirstButton.setToggleGroup(group);
    }
}