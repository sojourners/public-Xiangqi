package com.sojourners.chess.controller;

import com.sojourners.chess.App;
import com.sojourners.chess.board.ChessBoard;
import com.sojourners.chess.config.Properties;
import com.sojourners.chess.enginee.Engine;
import com.sojourners.chess.enginee.EngineCallBack;
import com.sojourners.chess.linker.*;
import com.sojourners.chess.lock.SingleLock;
import com.sojourners.chess.lock.WorkerTask;
import com.sojourners.chess.menu.BoardContextMenu;
import com.sojourners.chess.model.BookData;
import com.sojourners.chess.model.EngineConfig;
import com.sojourners.chess.model.ManualRecord;
import com.sojourners.chess.model.ThinkData;
import com.sojourners.chess.openbook.OpenBookManager;
import com.sojourners.chess.util.*;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Side;
import javafx.scene.canvas.Canvas;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.util.Callback;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Controller implements EngineCallBack, LinkerCallBack {

    @FXML
    private Canvas canvas;

    @FXML
    private BorderPane borderPane;
    @FXML
    private Label infoShowLabel;
    @FXML
    private ToolBar statusToolBar;
    @FXML
    private Label timeShowLabel;
    @FXML
    private SplitPane splitPane;
    @FXML
    private SplitPane splitPane2;

    @FXML
    private ListView<ThinkData> listView;

    @FXML
    private ComboBox<String> engineComboBox;

    @FXML
    private ComboBox<String> linkComboBox;

    @FXML
    private ComboBox<String> hashComboBox;

    @FXML
    private ComboBox<String> threadComboBox;

    @FXML
    private RadioMenuItem menuOfLargeBoard;
    @FXML
    private RadioMenuItem menuOfBigBoard;
    @FXML
    private RadioMenuItem menuOfMiddleBoard;
    @FXML
    private RadioMenuItem menuOfSmallBoard;
    @FXML
    private RadioMenuItem menuOfAutoFitBoard;

    @FXML
    private RadioMenuItem menuOfDefaultBoard;
    @FXML
    private RadioMenuItem menuOfCustomBoard;

    @FXML
    private CheckMenuItem menuOfStepTip;
    @FXML
    private CheckMenuItem menuOfStepSound;
    @FXML
    private CheckMenuItem menuOfLinkBackMode;
    @FXML
    private CheckMenuItem menuOfLinkAnimation;
    @FXML
    private CheckMenuItem menuOfShowStatus;
    @FXML
    private CheckMenuItem menuOfShowNumber;

    @FXML
    private CheckMenuItem menuOfTopWindow;

    private Properties prop;

    private Engine engine;

    private ChessBoard board;

    private AbstractGraphLinker graphLinker;

    @FXML
    private Button analysisButton;
    @FXML
    private Button blackButton;
    @FXML
    private Button redButton;
    @FXML
    private Button reverseButton;
    @FXML
    private Button newButton;
    @FXML
    private Button copyButton;
    @FXML
    private Button pasteButton;
    @FXML
    private Button backButton;

    @FXML
    private BorderPane charPane;
    private XYChart.Series lineChartSeries;

    @FXML
    private Button immediateButton;
    @FXML
    private Button bookSwitchButton;
    @FXML
    private Button linkButton;

    private String fenCode;
    private List<String> moveList;
    private int p;

    private SingleLock lock = new SingleLock();

    @FXML
    private TableView<ManualRecord> recordTable;

    @FXML
    private TableView<BookData> bookTable;

    private SimpleObjectProperty<Boolean> robotRed = new SimpleObjectProperty<>(false);
    private SimpleObjectProperty<Boolean> robotBlack = new SimpleObjectProperty<>(false);
    private SimpleObjectProperty<Boolean> robotAnalysis = new SimpleObjectProperty<>(false);
    private SimpleObjectProperty<Boolean> isReverse = new SimpleObjectProperty<>(false);
    private SimpleObjectProperty<Boolean> linkMode = new SimpleObjectProperty<>(false);
    private SimpleObjectProperty<Boolean> useOpenBook = new SimpleObjectProperty<>(false);

    /**
     * 走棋方
     */
    private boolean redGo;

    /**
     * 正在思考（用于连线判断）
     */
    private volatile boolean isThinking;

    @FXML
    public void newButtonClick(ActionEvent event) {
        if (linkMode.getValue()) {
            stopGraphLink();
        }

        newChessBoard(null);
    }

    @FXML
    void boardStyleSelected(ActionEvent event) {
        RadioMenuItem item = (RadioMenuItem) event.getTarget();
        if (item.equals(menuOfDefaultBoard)) {
            prop.setBoardStyle(ChessBoard.BoardStyle.DEFAULT);
        } else {
            prop.setBoardStyle(ChessBoard.BoardStyle.CUSTOM);
        }
        board.setBoardStyle(prop.getBoardStyle(), this.canvas);
    }

    @FXML
    void boardSizeSelected(ActionEvent event) {
        RadioMenuItem item = (RadioMenuItem) event.getTarget();
        if (item.equals(menuOfLargeBoard)) {
            prop.setBoardSize(ChessBoard.BoardSize.LARGE_BOARD);
        } else if (item.equals(menuOfBigBoard)) {
            prop.setBoardSize(ChessBoard.BoardSize.BIG_BOARD);
        } else if (item.equals(menuOfMiddleBoard)) {
            prop.setBoardSize(ChessBoard.BoardSize.MIDDLE_BOARD);
        } else if (item.equals(menuOfAutoFitBoard)) {
            prop.setBoardSize(ChessBoard.BoardSize.AUTOFIT_BOARD);
        } else {
            prop.setBoardSize(ChessBoard.BoardSize.SMALL_BOARD);
        }
        board.setBoardSize(prop.getBoardSize());
        if (prop.getBoardSize() == ChessBoard.BoardSize.AUTOFIT_BOARD) {
            board.autoFitSize(borderPane.getWidth(), borderPane.getHeight(), splitPane.getDividerPositions()[0], prop.isLinkShowInfo());
        }
    }
    @FXML
    void stepTipChecked(ActionEvent event) {
        CheckMenuItem item = (CheckMenuItem) event.getTarget();
        prop.setStepTip(item.isSelected());
        board.setStepTip(prop.isStepTip());
    }

    @FXML
    void showNumberClick(ActionEvent event) {
        CheckMenuItem item = (CheckMenuItem) event.getTarget();
        prop.setShowNumber(item.isSelected());
        board.setShowNumber(prop.isShowNumber());
    }

    @FXML
    void topWindowClick(ActionEvent event) {
        CheckMenuItem item = (CheckMenuItem) event.getTarget();
        prop.setTopWindow(item.isSelected());
        App.topWindow(prop.isTopWindow());
    }

    @FXML
    void linkBackModeChecked(ActionEvent event) {
        CheckMenuItem item = (CheckMenuItem) event.getTarget();
        if (linkMode.getValue()) {
            stopGraphLink();
        }
        prop.setLinkBackMode(item.isSelected());
    }

    @FXML
    void linkAnimationChecked(ActionEvent event) {
        CheckMenuItem item = (CheckMenuItem) event.getTarget();
        prop.setLinkAnimation(item.isSelected());
    }

    @FXML
    void stepSoundClick(ActionEvent event) {
        CheckMenuItem item = (CheckMenuItem) event.getTarget();
        prop.setStepSound(item.isSelected());
        board.setStepSound(prop.isStepSound());
    }

    @FXML
    void showStatusBarClick(ActionEvent event) {
        CheckMenuItem item = (CheckMenuItem) event.getTarget();
        prop.setLinkShowInfo(item.isSelected());
        statusToolBar.setVisible(item.isSelected());
        board.autoFitSize(borderPane.getWidth(), borderPane.getHeight(), splitPane.getDividerPositions()[0], prop.isLinkShowInfo());
    }

    @FXML
    public void analysisButtonClick(ActionEvent event) {
        robotAnalysis.setValue(!robotAnalysis.getValue());
        if (robotAnalysis.getValue()) {
            robotRed.setValue(false);
            robotBlack.setValue(false);
            engineGo();
        } else {
            engineStop();
        }

        redButton.setDisable(robotAnalysis.getValue());
        blackButton.setDisable(robotAnalysis.getValue());
        immediateButton.setDisable(robotAnalysis.getValue());

        if (linkMode.getValue() && !robotAnalysis.getValue()) {
            stopGraphLink();
        }
    }

    private void engineStop() {
        if (engine != null) {
            engine.stop();
        }
    }

    @FXML
    public void immediateButtonClick(ActionEvent event) {
        if (redGo && robotRed.getValue() || !redGo && robotBlack.getValue()) {
            engineStop();
        }
    }

    @FXML
    public void blackButtonClick(ActionEvent event) {
        robotBlack.setValue(!robotBlack.getValue());
        if (robotBlack.getValue() && !redGo) {
            engineGo();
        }
        if (!robotBlack.getValue() && !redGo) {
            engineStop();
        }

        if (linkMode.getValue() && !robotBlack.getValue()) {
            stopGraphLink();
        }
    }

    @FXML
    public void engineManageClick(ActionEvent e) {
        App.openEngineDialog();
        // 重新设置引擎列表
        refreshEngineComboBox();
        // 如果引擎被卸载，则关闭
        if (StringUtils.isEmpty(prop.getEngineName())) {
            // 重置按钮
            robotRed.setValue(false);
            robotBlack.setValue(false);
            robotAnalysis.setValue(false);
            // 关闭引擎
            if (engine != null) {
                engine.close();
                engine = null;
            }
        }
    }

    @FXML
    public void redButtonClick(ActionEvent event) {
        robotRed.setValue(!robotRed.getValue());
        if (robotRed.getValue() && redGo) {
            engineGo();
        }
        if (!robotRed.getValue() && redGo) {
            engineStop();
        }

        if (linkMode.getValue() && !robotRed.getValue()) {
            stopGraphLink();
        }
    }

    private void stopGraphLink() {
        graphLinker.stop();

        engineStop();

        redButton.setDisable(false);
        robotRed.setValue(false);

        blackButton.setDisable(false);
        robotBlack.setValue(false);

        analysisButton.setDisable(false);
        robotAnalysis.setValue(false);

        linkMode.setValue(false);
    }

    private void engineGo() {
        if (engine == null) {
            DialogUtils.showWarningDialog("提示", "引擎未加载");
            return;
        }

        if (robotRed.getValue() && redGo || robotBlack.getValue() && !redGo) {
            this.isThinking = true;
        } else {
            this.isThinking = false;
        }

        engine.setThreadNum(prop.getThreadNum());
        engine.setHashSize(prop.getHashSize());
        engine.setAnalysisModel(robotAnalysis.getValue() ? Engine.AnalysisModel.INFINITE : prop.getAnalysisModel(), prop.getAnalysisValue());
        engine.analysis(fenCode, moveList.subList(0, p), this.board.getBoard(), redGo);
    }

    @FXML
    public void canvasClick(MouseEvent event) {

        if (event.getButton() == MouseButton.PRIMARY) {
            String move = board.mouseClick((int) event.getX(), (int) event.getY(),
                    redGo && !robotRed.getValue(), !redGo && !robotBlack.getValue());

            if (move != null) {
                goCallBack(move);
            }

            BoardContextMenu.getInstance().hide();

        } else if (event.getButton() == MouseButton.SECONDARY) {

            BoardContextMenu.getInstance().show(this.canvas, Side.RIGHT, event.getX() - this.canvas.widthProperty().doubleValue(), event.getY());
        }

    }
    private void goCallBack(String move) {
        // 重新记录棋谱
        if (p == 0) {
            moveList.clear();
            resetTable();
            initLineChart();
        } else if (p < moveList.size()) {
            for (int i = moveList.size() - 1; i >= p; i--) {
                moveList.remove(i);
                recordTable.getItems().remove(i + 1);
                lineChartSeries.getData().remove(i);
            }
        }
        moveList.add(move);
        p++;
        int score = getScore();
        recordTable.getItems().add(new ManualRecord(p, board.translate(move, true), score));
        reLocationTable();
        // 趋势图
        lineChartSeries.getData().add(new XYChart.Data<>(p, score > 1000 ? 1000 : (score < -1000 ? -1000 : score)));
        // 切换行棋方
        redGo = !redGo;
        // 触发引擎走棋
        if (redGo && robotRed.getValue() || !redGo && robotBlack.getValue() || robotAnalysis.getValue()) {
            engineGo();
        }

    }
    private int getScore() {
        if (listView.getItems().size() <= 0)
            return 0;
        if (redGo && robotRed.getValue() || !redGo && robotBlack.getValue() || robotAnalysis.getValue()) {
            int score = listView.getItems().get(0).getScore();
            if (listView.getItems().get(0).getMate() != null) {
                score = (score < 0 ? -30000 : 30000) - score;
            }
            return score;
        } else {
            return recordTable.getItems().get(recordTable.getItems().size() - 1).getScore();
        }
    }
    private void reLocationTable() {
        recordTable.getSelectionModel().select(p);
        recordTable.scrollTo(p);
    }

    private void browseChessRecord() {
        // 棋盘
        board.browseChessRecord(fenCode, moveList, p);
        // 定位table滚动条
        reLocationTable();
        // 设置行棋方
        redGo = fenCode.contains("w");
        if (p % 2 != 0) {
            redGo = !redGo;
        }
        // 引擎走棋
        if (robotRed.getValue() && robotBlack.getValue()) {
            // 如果引擎执红同时执黑，取消状态（否则会有问题）
            robotRed.setValue(false);
            robotBlack.setValue(false);
            engineStop();
        } else if (redGo && robotRed.getValue() || !redGo && robotBlack.getValue() || robotAnalysis.getValue()) {
            // 轮到引擎走棋或者分析模式
            engineGo();
        } else {
            // 其他情况，停止引擎思考
            engineStop();
        }
    }

    @FXML
    void recordTableClick(MouseEvent event) {
        if (linkMode.getValue()) {
            stopGraphLink();
        }
        int index = recordTable.getSelectionModel().getSelectedIndex();
        if (index != p && index >= 0) {
            p = index;
            browseChessRecord();
        }
    }

    @FXML
    public void backButtonClick(ActionEvent event) {
        if (linkMode.getValue()) {
            stopGraphLink();
        }
        if (p > 0) {
            p--;
            browseChessRecord();
        }
    }

    @FXML
    public void regretButtonClick(ActionEvent event) {
        if (linkMode.getValue()) {
            stopGraphLink();
        }
        if (p > 0) {
            if (redGo && robotRed.getValue() || !redGo && robotBlack.getValue()) {
                p -= 1;
            } else {
                p -= 2;
            }
            if (p < 0) p = 0;
            browseChessRecord();
        }
    }

    @FXML
    void forwardButtonClick(ActionEvent event) {
        if (linkMode.getValue()) {
            stopGraphLink();
        }
        if (p < moveList.size()) {
            p++;
            browseChessRecord();
        }
    }

    @FXML
    void finalButtonClick(ActionEvent event) {
        if (linkMode.getValue()) {
            stopGraphLink();
        }
        if (p < moveList.size()) {
            p = moveList.size();
            browseChessRecord();
        }
    }

    @FXML
    void frontButtonClick(ActionEvent event) {
        if (p > 0) {
            p = 0;
            browseChessRecord();
        }
    }

    @FXML
    public void copyButtonClick(ActionEvent e) {
        String fenCode = board.fenCode(redGo);
        ClipboardUtils.setText(fenCode);
    }

    @FXML
    public void pasteButtonClick(ActionEvent e) {
        String fenCode = ClipboardUtils.getText();
        if (StringUtils.isNotEmpty(fenCode) && fenCode.split("/").length == 10) {
            newFromOriginFen(fenCode);
        }
    }

    @FXML
    public void importImageMenuClick(ActionEvent e) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setInitialDirectory(new File(PathUtils.getJarPath()));
        File file = fileChooser.showOpenDialog(App.getMainStage());
        if (file != null) {
            importFromImgFile(file);
        }
    }

    @FXML
    public void exportImageMenuClick(ActionEvent e) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setInitialDirectory(new File(PathUtils.getJarPath()));
        fileChooser.setInitialFileName("tchess_export_" + DateUtils.getDateTimeString(new Date()) + ".png");
        File file = fileChooser.showSaveDialog(App.getMainStage());
        if (file != null) {
            try {
                WritableImage writableImage = new WritableImage((int) this.canvas.getWidth(), (int) this.canvas.getHeight());
                canvas.snapshot(null, writableImage);
                RenderedImage renderedImage = SwingFXUtils.fromFXImage(writableImage, null);
                ImageIO.write(renderedImage, "png", file);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    @FXML
    public void aboutClick(ActionEvent e) {
        DialogUtils.showInfoDialog("关于", "中国象棋界面"
                + System.lineSeparator() + "Built on : " + App.BUILT_ON
                + System.lineSeparator() + "Author : T"
                + System.lineSeparator() + "Version : " + App.VERSION);
    }

    @FXML
    public void homeClick(ActionEvent e) {
        Desktop desktop = Desktop.getDesktop();
        if (Desktop.isDesktopSupported() && desktop.isSupported(Desktop.Action.BROWSE)) {
            try {
                URI uri = new URI("https://github.com/sojourners/public-Xiangqi");
                desktop.browse(uri);
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        }
    }

    @FXML
    void localBookManageButtonClick(ActionEvent e) {
        if (App.openLocalBookDialog()) {
            OpenBookManager.getInstance().setLocalOpenBooks();
        }

    }

    @FXML
    void timeSettingButtonClick(ActionEvent e) {
        App.openTimeSetting();
    }

    @FXML
    void bookSettingButtonClick(ActionEvent e) {
        App.openBookSetting();
    }

    @FXML
    void linkSettingClick(ActionEvent e) {
        App.openLinkSetting();

    }

    @FXML
    public void reverseButtonClick(ActionEvent event) {
        isReverse.setValue(!isReverse.getValue());
        board.reverse(isReverse.getValue());
    }

    @FXML
    private void bookSwitchButtonClick(ActionEvent e) {
        useOpenBook.setValue(!useOpenBook.getValue());
        prop.setBookSwitch(useOpenBook.getValue());
    }

    @FXML
    private void linkButtonClick(ActionEvent e) {
        linkMode.setValue(!linkMode.getValue());
        if (linkMode.getValue()) {
            graphLinker.start();
        } else {
            stopGraphLink();
        }
    }

    private void initLineChart() {
        final NumberAxis xAxis = new NumberAxis();
        final NumberAxis yAxis = new NumberAxis(-1000, 1000, 500);
        xAxis.setTickLabelsVisible(false);
        xAxis.setTickMarkVisible(false);
        xAxis.setMinorTickVisible(false);
        yAxis.setTickMarkVisible(false);
        yAxis.setMinorTickVisible(false);

        LineChart<Number,Number> lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.setMinHeight(100);
        lineChart.setLegendVisible(false);
        lineChart.setCreateSymbols(false);
        lineChart.setVerticalGridLinesVisible(false);
        lineChart.getStylesheets().add(this.getClass().getResource("/style/table.css").toString());

        lineChartSeries = new XYChart.Series();
        lineChart.getData().add(lineChartSeries);

        charPane.setCenter(lineChart);
    }
    public void initialize() {
        // 读取配置
        prop = Properties.getInstance();
        // 思考细节listView
        listView.setCellFactory(new Callback() {
            @Override
            public Object call(Object param) {
                ListCell<ThinkData> cell = new ListCell<ThinkData>() {
                    @Override
                    protected void updateItem(ThinkData item, boolean bln) {
                        super.updateItem(item, bln);
                        if (!bln) {
                            VBox box = new VBox();

                            Label title = new Label();
                            title.setText(item.getTitle());
                            title.setTextFill(item.getScore() >= 0 ? Color.BLUE : Color.RED);
                            box.getChildren().add(title);

                            Label body = new Label();
                            body.setText(item.getBody());
                            body.setTextFill(Color.BLACK);
                            body.setWrapText(true);
                            body.setMaxWidth(listView.getWidth() / 1.124);//bind(listView.widthProperty().divide(1.124));
                            box.getChildren().add(body);

                            setGraphic(box);
                        }
                    }
                };
                return cell;
            }

        });
        // 按钮
        setButtonTips();
        // 棋盘
        initChessBoard();
        // 棋谱
        initRecordTable();
        // 库招表
        initBookTable();
        // 引擎view
        initEngineView();
        // 加载引擎
        loadEngine(prop.getEngineName());
        // 连线器
        initGraphLinker();
        // 按钮监听
        initButtonListener();
        // autofit board size listener
        initAutoFitBoardListener();
        // canvas drag listener
        initCanvasDragListener();

        useOpenBook.setValue(prop.getBookSwitch());
    }

    private void importFromBufferImage(BufferedImage img) {
        char[][] result = graphLinker.findChessBoard(img);
        if (result != null) {
            if (!XiangqiUtils.validateChessBoard(result) && !DialogUtils.showConfirmDialog("提示", "检测到局面不合法，可能会导致引擎退出或者崩溃，是否继续？")) {
                return;
            }
            String fenCode = ChessBoard.fenCode(result, true);
            newFromOriginFen(fenCode);
        }
    }

    private void importFromImgFile(File f) {
        if (f.exists() && PathUtils.isImage(f.getAbsolutePath())) {
            try {
                BufferedImage img = ImageIO.read(f);
                importFromBufferImage(img);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void initCanvasDragListener() {
        this.canvas.setOnDragDropped(event -> {
            File f = event.getDragboard().getFiles().get(0);
            importFromImgFile(f);
        });
        this.canvas.setOnDragOver(event -> {
            event.acceptTransferModes(TransferMode.ANY);
            event.consume();
        });
    }

    private void initAutoFitBoardListener() {
        borderPane.widthProperty().addListener((observableValue, number, t1) -> {
            board.autoFitSize(t1.doubleValue(), borderPane.getHeight(), splitPane.getDividerPositions()[0], prop.isLinkShowInfo());
        });
        borderPane.heightProperty().addListener((observableValue, number, t1) -> {
            board.autoFitSize(borderPane.getWidth(), t1.doubleValue(), splitPane.getDividerPositions()[0], prop.isLinkShowInfo());
        });
        splitPane.getDividers().get(0).positionProperty().addListener((observableValue, number, t1) -> {
            board.autoFitSize(borderPane.getWidth(), borderPane.getHeight(), t1.doubleValue(), prop.isLinkShowInfo());
        });
    }

    private void initBookTable() {
        TableColumn moveCol = bookTable.getColumns().get(0);
        moveCol.setCellValueFactory(new PropertyValueFactory<BookData, String>("word"));
        TableColumn scoreCol = bookTable.getColumns().get(1);
        scoreCol.setCellValueFactory(new PropertyValueFactory<BookData, Integer>("score"));
        TableColumn winRateCol = bookTable.getColumns().get(2);
        winRateCol.setCellValueFactory(new PropertyValueFactory<BookData, Double>("winRate"));
        TableColumn winNumCol = bookTable.getColumns().get(3);
        winNumCol.setCellValueFactory(new PropertyValueFactory<BookData, Integer>("winNum"));
        TableColumn drawNumCol = bookTable.getColumns().get(4);
        drawNumCol.setCellValueFactory(new PropertyValueFactory<BookData, Integer>("drawNum"));
        TableColumn loseNumCol = bookTable.getColumns().get(5);
        loseNumCol.setCellValueFactory(new PropertyValueFactory<BookData, Integer>("loseNum"));
        TableColumn noteCol = bookTable.getColumns().get(6);
        noteCol.setCellValueFactory(new PropertyValueFactory<BookData, String>("note"));
        TableColumn sourceCol = bookTable.getColumns().get(7);
        sourceCol.setCellValueFactory(new PropertyValueFactory<BookData, String>("source"));
    }

    private void initRecordTable() {
        TableColumn idCol = recordTable.getColumns().get(0);
        idCol.setCellValueFactory(new PropertyValueFactory<ManualRecord, String>("id"));
        TableColumn nameCol = recordTable.getColumns().get(1);
        nameCol.setCellValueFactory(new PropertyValueFactory<ManualRecord, String>("name"));
        TableColumn scoreCol = recordTable.getColumns().get(2);
        scoreCol.setCellValueFactory(new PropertyValueFactory<ManualRecord, String>("score"));
    }

    public void initStage() {
        borderPane.setPrefWidth(prop.getStageWidth());
        borderPane.setPrefHeight(prop.getStageHeight());
        splitPane.setDividerPosition(0, prop.getSplitPos());
        splitPane2.setDividerPosition(0, prop.getSplitPos2());

        // 窗口置顶
        menuOfTopWindow.setSelected(prop.isTopWindow());
        App.topWindow(prop.isTopWindow());
    }

    private void setButtonTips() {
        newButton.setTooltip(new Tooltip("新局面"));
        copyButton.setTooltip(new Tooltip("复制局面"));
        pasteButton.setTooltip(new Tooltip("粘贴局面"));
        backButton.setTooltip(new Tooltip("悔棋"));
        reverseButton.setTooltip(new Tooltip("翻转"));
        redButton.setTooltip(new Tooltip("引擎执红"));
        blackButton.setTooltip(new Tooltip("引擎执黑"));
        analysisButton.setTooltip(new Tooltip("分析模式"));
        immediateButton.setTooltip(new Tooltip("立即出招"));
        linkButton.setTooltip(new Tooltip("连线"));
        bookSwitchButton.setTooltip(new Tooltip("启用库招"));

    }

    private void initChessBoard() {
        // 棋步提示
        menuOfStepTip.setSelected(prop.isStepTip());
        // 走棋音效
        menuOfStepSound.setSelected(prop.isStepSound());
        // 连线后台模式
        menuOfLinkBackMode.setSelected(prop.isLinkBackMode());
        // 连线动画确认
        menuOfLinkAnimation.setSelected(prop.isLinkAnimation());
        // show number
        menuOfShowNumber.setSelected(prop.isShowNumber());
        // 显示状态栏
        menuOfShowStatus.setSelected(prop.isLinkShowInfo());
        // 棋盘大小
        if (prop.getBoardSize() == ChessBoard.BoardSize.LARGE_BOARD) {
            menuOfLargeBoard.setSelected(true);
        } else if (prop.getBoardSize() == ChessBoard.BoardSize.BIG_BOARD) {
            menuOfBigBoard.setSelected(true);
        } else if (prop.getBoardSize() == ChessBoard.BoardSize.MIDDLE_BOARD) {
            menuOfMiddleBoard.setSelected(true);
        } else if (prop.getBoardSize() == ChessBoard.BoardSize.AUTOFIT_BOARD) {
            menuOfAutoFitBoard.setSelected(true);
        } else {
            menuOfSmallBoard.setSelected(true);
        }
        // 棋盘样式
        if (prop.getBoardStyle() == ChessBoard.BoardStyle.DEFAULT) {
            menuOfDefaultBoard.setSelected(true);
        } else {
            menuOfCustomBoard.setSelected(true);
        }
        // 右键菜单
        initBoardContextMenu();
        // 状态栏
        this.infoShowLabel.prefWidthProperty().bind(statusToolBar.widthProperty().subtract(120));
        this.timeShowLabel.setText(prop.getAnalysisModel() == Engine.AnalysisModel.FIXED_TIME ? "固定时间" + prop.getAnalysisValue() / 1000d + "s" : "固定深度" + prop.getAnalysisValue() + "层");
        this.statusToolBar.setVisible(prop.isLinkShowInfo());

        newChessBoard(null);
    }

    private void initBoardContextMenu() {
        BoardContextMenu.getInstance().setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                MenuItem item = (MenuItem) event.getTarget();
                if ("复制局面FEN".equals(item.getText())) {
                    copyButtonClick(null);
                } else if ("粘贴局面FEN".equals(item.getText())) {
                    pasteButtonClick(null);
                } else if ("交换行棋方".equals(item.getText())) {
                    switchPlayer(true);
                } else if ("编辑局面".equals(item.getText())) {
                    editChessBoardClick(null);
                } else if ("复制局面图片".equals(item.getText())) {
                    copyImageMenuClick(null);
                } else if ("粘贴局面图片".equals(item.getText())) {
                    pasteImageMenuClick(null);
                }
            }
        });
    }

    @FXML
    public void copyImageMenuClick(ActionEvent event) {
        WritableImage writableImage = new WritableImage((int) canvas.getWidth(), (int) canvas.getHeight());
        canvas.snapshot(null, writableImage);
        BufferedImage bi =SwingFXUtils.fromFXImage(writableImage, null);
        ClipboardUtils.setImage(bi);
    }

    @FXML
    public void pasteImageMenuClick(ActionEvent event) {
        Image img = ClipboardUtils.getImage();
        if (img != null) {
            importFromBufferImage((BufferedImage) img);
        }
    }

    @FXML
    public void editChessBoardClick(ActionEvent e) {
        String fenCode = App.openEditChessBoard(board.getBoard(), redGo, isReverse.getValue());
        newFromOriginFen(fenCode);
    }

    /**
     * new from origin fen that maybe reverse, and stop link mode at the same time
     * @param fenCode
     */
    private void newFromOriginFen(String fenCode) {
        if (StringUtils.isNotEmpty(fenCode)) {
            if (linkMode.getValue()) {
                stopGraphLink();
            }

            newChessBoard(fenCode);
            if (XiangqiUtils.isReverse(fenCode)) {
                reverseButtonClick(null);
            }
        }
    }

    /**
     * 新建局面
     * @param fenCode 传null 新建默认初始局面；传fenCode 则根据fen创建局面
     */
    private void newChessBoard(String fenCode) {
        // 重置按钮
        robotRed.setValue(false);
        redButton.setDisable(false);
        robotBlack.setValue(false);
        blackButton.setDisable(false);
        robotAnalysis.setValue(false);
        immediateButton.setDisable(false);
        isReverse.setValue(false);
        // 引擎停止计算
        engineStop();
        // 绘制棋盘
        board = new ChessBoard(this.canvas, prop.getBoardSize(), prop.getBoardStyle(), prop.isStepTip(), prop.isStepSound(), prop.isShowNumber(), fenCode);
        // 设置局面
        redGo = StringUtils.isEmpty(fenCode) ? true : fenCode.contains("w");
        this.fenCode = board.fenCode(redGo);
        moveList = new ArrayList<>();
        // 设置棋谱
        p = 0;
        resetTable();
        // 库招显示
        this.bookTable.getItems().clear();
        // 重置趋势图
        initLineChart();
        // 重置引擎思考输出
        listView.getItems().clear();
        // 清空思考状态信息
        this.infoShowLabel.setText("");

        System.gc();
    }
    private void resetTable() {
        recordTable.getItems().clear();
        recordTable.getItems().add(new ManualRecord(p, "初始局面", 0));
    }

    private void initEngineView() {
        // 引擎列表 线程数 哈希表大小
        refreshEngineComboBox();
        for (int i = 1; i <= Runtime.getRuntime().availableProcessors(); i++) {
            threadComboBox.getItems().add(String.valueOf(i));
        }
        hashComboBox.getItems().addAll("16", "32", "64", "128", "256", "512", "1024", "2048", "4096");
        // 加载设置
        threadComboBox.setValue(String.valueOf(prop.getThreadNum()));
        hashComboBox.setValue(String.valueOf(prop.getHashSize()));
    }


    private void initGraphLinker() {
        try {
            this.graphLinker = com.sun.jna.Platform.isWindows() ?
                    new WindowsGraphLinker(this) : (com.sun.jna.Platform.isLinux() ?
                    new LinuxGraphLinker(this) : new MacosGraphLinker(this));
        } catch (Exception e) {
            e.printStackTrace();
        }

        linkComboBox.getItems().addAll("自动走棋", "观战模式");
        linkComboBox.setValue("自动走棋");
    }

    private void refreshEngineComboBox() {
        engineComboBox.getItems().clear();
        for (EngineConfig ec : prop.getEngineConfigList()) {
            engineComboBox.getItems().add(ec.getName());
        }
        engineComboBox.setValue(prop.getEngineName());
    }

    private void initButtonListener() {
        addListener(redButton, robotRed);
        addListener(blackButton, robotBlack);
        addListener(analysisButton, robotAnalysis);
        addListener(reverseButton, isReverse);
        addListener(linkButton, linkMode);
        addListener(bookSwitchButton, useOpenBook);

        threadComboBox.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observableValue, String s, String t1) {
                int num = Integer.parseInt(t1);
                if (num != prop.getThreadNum()) {
                    prop.setThreadNum(num);
                }
            }
        });
        hashComboBox.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observableValue, String s, String t1) {
                int size = Integer.parseInt(t1);
                if (size != prop.getHashSize()) {
                    prop.setHashSize(size);
                }
            }
        });
        engineComboBox.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observableValue, String s, String t1) {
                if (StringUtils.isNotEmpty(t1) && !t1.equals(prop.getEngineName())) {
                    // 保存引擎设置
                    prop.setEngineName(t1);
                    // 重置三个按钮
                    robotRed.setValue(false);
                    robotBlack.setValue(false);
                    robotAnalysis.setValue(false);
                    // 停止连线
                    if (linkMode.getValue()) {
                        stopGraphLink();
                    }
                    // 加载新引擎
                    loadEngine(t1);
                }
            }
        });
        linkComboBox.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observableValue, String s, String t1) {
                setLinkMode(t1);
            }
        });
    }

    private void setLinkMode(String t1) {
        if (linkMode.getValue()) {
            if ("自动走棋".equals(t1)) {
                // 观战模式切换自动走棋，先停止引擎
                engineStop();
                // 走黑棋/红棋
                if (isReverse.getValue()) {
                    blackButton.setDisable(false);
                    robotBlack.setValue(true);

                    redButton.setDisable(true);
                    robotRed.setValue(false);

                    analysisButton.setDisable(true);
                    robotAnalysis.setValue(false);

                    if (!redGo) {
                        engineGo();
                    }
                } else {
                    redButton.setDisable(false);
                    robotRed.setValue(true);

                    blackButton.setDisable(true);
                    robotBlack.setValue(false);

                    analysisButton.setDisable(true);
                    robotAnalysis.setValue(false);

                    if (redGo) {
                        engineGo();
                    }
                }
            } else {
                analysisButton.setDisable(false);
                robotAnalysis.setValue(true);

                blackButton.setDisable(true);
                robotBlack.setValue(false);

                redButton.setDisable(true);
                robotRed.setValue(false);

                immediateButton.setDisable(true);

                engineGo();
            }
        }
    }

    private void addListener(Button button, ObjectProperty property) {
        property.addListener((ChangeListener<Boolean>) (observableValue, aBoolean, t1) -> {
            if (t1) {
                button.getStylesheets().add(this.getClass().getResource("/style/selected-button.css").toString());
            } else {
                button.getStylesheets().remove(this.getClass().getResource("/style/selected-button.css").toString());
            }
        });
    }

    private void loadEngine(String name) {
        try {
            if (StringUtils.isNotEmpty(name)) {
                for (EngineConfig ec : prop.getEngineConfigList()) {
                    if (name.equals(ec.getName())) {
                        if (engine != null) {
                            engine.close();
                        }
                        engine = new Engine(ec, this);
                        return;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 连线模式下自动点击走棋
     * @param step
     */
    private void trickAutoClick(ChessBoard.Step step) {
        if (step != null) {
            int x1 = step.getFirst().getX(), y1 = step.getFirst().getY();
            int x2 = step.getSecond().getX(), y2 = step.getSecond().getY();
            if (robotBlack.getValue()) {
                y1 = 9 - y1;
                y2 = 9 - y2;
                x1 = 8 - x1;
                x2 = 8 - x2;
            }
            graphLinker.autoClick(x1, y1, x2, y2);
        }
        this.isThinking = false;
    }

    @Override
    public void bestMove(String first, String second) {
        if (redGo && robotRed.getValue() || !redGo && robotBlack.getValue()) {
            ChessBoard.Step s = board.stepForBoard(first);

            Platform.runLater(() -> {
                board.move(s.getFirst().getX(), s.getFirst().getY(), s.getSecond().getX(), s.getSecond().getY());
                board.setTip(second, null);

                goCallBack(first);
            });

            if (linkMode.getValue()) {
                trickAutoClick(s);
            }
        }
    }

    @Override
    public void thinkDetail(ThinkData td) {
        if (redGo && robotRed.getValue() || !redGo && robotBlack.getValue() || robotAnalysis.getValue()) {
            td.generate(redGo, isReverse.getValue(), board);
            if (td.getValid()) {
                Platform.runLater(() -> {
                    listView.getItems().add(0, td);
                    if (listView.getItems().size() > 128) {
                        listView.getItems().remove(listView.getItems().size() - 1);
                    }

                    if (prop.isLinkShowInfo()) {
                        infoShowLabel.setText(td.getTitle() + " | " + td.getBody());
                        infoShowLabel.setTextFill(td.getScore() >= 0 ? Color.BLUE : Color.RED);
                        timeShowLabel.setText(prop.getAnalysisModel() == Engine.AnalysisModel.FIXED_TIME ? "固定时间" + prop.getAnalysisValue() / 1000d + "s" : "固定深度" + prop.getAnalysisValue() + "层");
                    }

                    board.setTip(td.getDetail().get(0), td.getDetail().size() > 1 ? td.getDetail().get(1) : null);
                });
            }
        }
    }

    @Override
    public void showBookResults(List<BookData> list) {
        this.bookTable.getItems().clear();
        for (BookData bd : list) {
            String move = bd.getMove();
            bd.setWord(board.translate(move, false));
            this.bookTable.getItems().add(bd);
        }
    }

    private void callWorker(WorkerTask task) {
        lock.lock();
        Platform.runLater(() -> {
            task.call();
            lock.unlock();
        });
    }

    @FXML
    public void exit() {
        if (engine != null) {
            engine.close();
        }

        OpenBookManager.getInstance().close();
        ExecutorsUtils.getInstance().close();

        graphLinker.stop();

        prop.setStageWidth(borderPane.getWidth());
        prop.setStageHeight(borderPane.getHeight());
        prop.setSplitPos(splitPane.getDividerPositions()[0]);
        prop.setSplitPos2(splitPane2.getDividerPositions()[0]);

        prop.save();

        Platform.exit();
    }

    /**
     * 图形连线初始化棋盘
     * @param fenCode
     * @param isReverse
     */
    @Override
    public void linkerInitChessBoard(String fenCode, boolean isReverse) {
        Platform.runLater(() -> {
            newChessBoard(fenCode);
            if (isReverse) {
                reverseButtonClick(null);
            }
            setLinkMode(linkComboBox.getValue());
        });
    }

    @Override
    public char[][] getEngineBoard() {
        return board.getBoard();
    }

    @Override
    public boolean isThinking() {
        return this.isThinking;
    }

    @Override
    public boolean isWatchMode() {
        return "观战模式".equals(linkComboBox.getValue());
    }

    @Override
    public void linkerMove(int x1, int y1, int x2, int y2) {
        Platform.runLater(() -> {
            String move = board.move(x1, y1, x2, y2);
            if (move != null) {
                boolean red = XiangqiUtils.isRed(board.getBoard()[y2][x2]);
                if (isWatchMode() && (!redGo && red || redGo && !red)) {
                    System.out.println(move + "," + red + ", " + redGo);
                    // 连线识别行棋方错误，自动切换行棋方
                    switchPlayer(false);
                } else {
                    goCallBack(move);
                }
            }
        });
    }

    private void switchPlayer(boolean f) {
        engineStop();

        graphLinker.pause();

        boolean tmpRed = robotRed.getValue(), tmpBlack = robotBlack.getValue(), tmpAnalysis = robotAnalysis.getValue(), tmpLink = linkMode.getValue(), tmpReverse = isReverse.getValue();

        String fenCode = board.fenCode(f ? !redGo : redGo);
        newChessBoard(fenCode);

        isReverse.setValue(tmpReverse);
        board.reverse(tmpReverse);
        robotRed.setValue(tmpRed);
        robotBlack.setValue(tmpBlack);
        robotAnalysis.setValue(tmpAnalysis);
        linkMode.setValue(tmpLink);

        graphLinker.resume();
        if (robotRed.getValue() && redGo || robotBlack.getValue() && !redGo || robotAnalysis.getValue()) {
            engineGo();
        }
    }
}
