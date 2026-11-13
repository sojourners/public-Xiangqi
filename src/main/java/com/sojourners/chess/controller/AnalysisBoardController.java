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
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** 独立于主棋盘的单线推演窗口。 */
public class AnalysisBoardController {

    private static Stage window;
    private static AnalysisBoardController controller;
    private static final ChessBoard.BoardSize BOARD_SIZE = ChessBoard.BoardSize.MIDDLE_BOARD;

    @FXML
    private Canvas canvas;
    @FXML
    private ListView<String> recordList;
    @FXML
    private Button firstButton;
    @FXML
    private Button previousButton;
    @FXML
    private Button nextButton;
    @FXML
    private Button lastButton;
    @FXML
    private Label statusLabel;

    // ChessBoard 的棋盘及绘图器是静态共享的，此处只复用绘图器类型和规则工具。
    private BaseBoardRender boardRender;
    private char[][] board;
    private final List<char[][]> positions = new ArrayList<>();
    private final List<ChessBoard.Step> steps = new ArrayList<>();
    private boolean initialRedGo;
    private boolean redGo;
    private boolean reverse;
    private int cursor;
    private ChessBoard.Point remark;

    public static void open(Window owner, char[][] source, boolean redGo, boolean reverse) {
        try {
            if (window == null) {
                FXMLLoader loader = new FXMLLoader(App.class.getResource("/fxml/analysisBoard.fxml"));
                Parent root = loader.load();
                Stage stage = new Stage();
                stage.setTitle("推演局面");
                stage.initOwner(owner);
                stage.initModality(Modality.NONE);
                stage.setResizable(false);
                stage.setScene(new Scene(root));
                stage.setOnHidden(event -> {
                    window = null;
                    controller = null;
                });
                controller = loader.getController();
                window = stage;
            }
            controller.reset(source, redGo, reverse);
            App.applyTheme(window.getScene());
            window.show();
            window.setIconified(false);
            window.toFront();
            window.requestFocus();
        } catch (IOException e) {
            e.printStackTrace();
            DialogUtils.showErrorDialog("推演局面", "无法打开推演窗口：" + e.getMessage());
        }
    }

    @FXML
    private void initialize() {
        recordList.getSelectionModel().selectedIndexProperty().addListener((observable, oldValue, newValue) -> {
            int index = newValue.intValue();
            if (index >= 0 && index != cursor) {
                browse(index);
            }
        });
    }

    public void reset(char[][] source, boolean redGo, boolean reverse) {
        Properties prop = Properties.getInstance();
        boolean customSkin = prop.getBoardStyle() == ChessBoard.BoardStyle.CUSTOM
                && CustomBoardRender.isSkinAvailable(prop.getCustomSkin());
        boardRender = customSkin ? new CustomBoardRender(canvas, prop.getCustomSkin()) : new DefaultBoardRender(canvas);
        board = copy(source);
        initialRedGo = redGo;
        this.redGo = redGo;
        this.reverse = reverse;
        cursor = 0;
        remark = null;
        positions.clear();
        positions.add(copy(board));
        steps.clear();
        recordList.getItems().setAll("开始局面");
        refresh();
    }

    @FXML
    public void canvasClick(MouseEvent event) {
        if (event.getButton() != MouseButton.PRIMARY || board == null) {
            return;
        }
        int padding = boardRender.getPadding(BOARD_SIZE);
        int piece = boardRender.getPieceSize(BOARD_SIZE);
        double x = event.getX() - padding;
        double y = event.getY() - padding;
        if (x < 0 || x >= piece * 9 || y < 0 || y >= piece * 10) {
            return;
        }
        int col = (int) (x / piece);
        int row = (int) (y / piece);
        if (reverse) {
            col = 8 - col;
            row = 9 - row;
        }
        char target = board[row][col];
        if (target != ' ' && XiangqiUtils.isRed(target) == redGo) {
            remark = new ChessBoard.Point(col, row);
            paint();
        } else if (remark != null) {
            move(remark.getX(), remark.getY(), col, row);
        }
    }

    private void move(int fromCol, int fromRow, int toCol, int toRow) {
        if (!XiangqiUtils.canGo(board, fromRow, fromCol, toRow, toCol)) {
            return;
        }
        char[][] next = copy(board);
        next[toRow][toCol] = next[fromRow][fromCol];
        next[fromRow][fromCol] = ' ';
        if (XiangqiUtils.isJiang(next, redGo)) {
            return;
        }

        StringBuilder notation = new StringBuilder();
        ChessBoard.Step step = XiangqiUtils.translate(board, notation,
                ChessBoard.stepForEngine(fromCol, fromRow, toCol, toRow), false);

        // 浏览到中途再走棋时，用新着法替换后续记录，不创建分支。
        positions.subList(cursor + 1, positions.size()).clear();
        steps.subList(cursor, steps.size()).clear();
        recordList.getItems().subList(cursor + 1, recordList.getItems().size()).clear();
        positions.add(copy(next));
        steps.add(step);
        board = next;
        cursor++;
        recordList.getItems().add(cursor + ". " + (redGo ? "红方  " : "黑方  ") + notation);
        redGo = !redGo;
        remark = null;
        refresh();
    }

    @FXML
    private void first() {
        browse(0);
    }

    @FXML
    private void previous() {
        browse(cursor - 1);
    }

    @FXML
    private void next() {
        browse(cursor + 1);
    }

    @FXML
    private void last() {
        browse(steps.size());
    }

    private void browse(int index) {
        if (index < 0 || index >= positions.size()) {
            return;
        }
        cursor = index;
        board = copy(positions.get(index));
        redGo = initialRedGo != (index % 2 == 1);
        remark = null;
        refresh();
    }

    private void refresh() {
        firstButton.setDisable(cursor == 0);
        previousButton.setDisable(cursor == 0);
        nextButton.setDisable(cursor == steps.size());
        lastButton.setDisable(cursor == steps.size());
        statusLabel.setText((redGo ? "红方走棋" : "黑方走棋") + "  ·  " + cursor + " / " + steps.size() + " 手");
        recordList.getSelectionModel().select(cursor);
        recordList.scrollTo(cursor);
        paint();
    }

    private void paint() {
        boardRender.paint(BOARD_SIZE, board, cursor == 0 ? null : steps.get(cursor - 1), remark,
                false, false, null, reverse, Properties.getInstance().isShowNumber(), false, null);
    }

    private static char[][] copy(char[][] source) {
        char[][] result = new char[source.length][];
        for (int i = 0; i < source.length; i++) {
            result[i] = source[i].clone();
        }
        return result;
    }
}
