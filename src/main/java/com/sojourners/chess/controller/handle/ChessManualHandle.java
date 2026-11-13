package com.sojourners.chess.controller.handle;

import com.sojourners.chess.App;
import com.sojourners.chess.config.Properties;
import com.sojourners.chess.manual.*;
import com.sojourners.chess.model.ManualRecord;
import com.sojourners.chess.util.ClipboardUtils;
import com.sojourners.chess.util.DialogUtils;
import com.sojourners.chess.util.PathUtils;
import com.sojourners.chess.util.ScoreScale;
import com.sojourners.chess.util.StringUtils;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.skin.VirtualFlow;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.text.Font;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.util.Duration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;

public class ChessManualHandle {

    private ChessManualCallBack cb;

    /**
     * 棋谱数据结构
     */
    private String fenCode;
    private ManualRecord manualHead;
    private int p;
    private File manualFile;

    /**
     * 控件
     */
    private BorderPane chessManualPane;
    private CheckMenuItem menuOfChessNotation;
    private CheckMenuItem menuOfShowTactic;
    private TreeView<File> notationTree;
    private Label manualTitleLabel;
    private TableView<ManualRecord> recordTable;
    private ListView<ManualRecord> subRecordTable;
    private TextArea remarkText;

    private Button manualBackButton;
    private Button manualDeleteButton;
    private Button manualDownButton;
    private Button manualFinalButton;
    private Button manualForwardButton;
    private Button manualFrontButton;
    private Button manualPlayButton;
    private Button manualUpButton;
    private Button openManualButton;
    private Button saveManualButton;
    private Button manualScoreButton;
    private TextField competitionNameText;
    private TextField competitionCityText;
    private TextField competitionDateText;
    private TextField competitionRedText;
    private TextField competitionBlackText;

    private Properties prop;

    private Timeline manualPlayTimeline;

    public static Map<String, ChessManualService> manualServices;

    static {
        manualServices = new HashMap<>();
        manualServices.put("txq", new TxqChessManualImpl());
        manualServices.put("pgn", new PgnChessManualImpl());
        manualServices.put("xqf", new XqfChessManualImpl());
        manualServices.put("cbr", new CbrChessManualImpl());
    }

    public ChessManualHandle(BorderPane chessManualPane, CheckMenuItem menuOfChessNotation, CheckMenuItem menuOfShowTactic, TreeView notationTree,
                             Label manualTitleLabel, TableView recordTable, ListView subRecordTable, TextArea remarkText,
                             Button manualBackButton, Button manualDeleteButton, Button manualDownButton, Button manualFinalButton,
                             Button manualForwardButton, Button manualFrontButton, Button manualPlayButton, Button manualUpButton,
                             Button openManualButton, Button saveManualButton, Button manualScoreButton,
                             TextField competitionNameText, TextField competitionCityText, TextField competitionDateText,
                             TextField competitionRedText, TextField competitionBlackText,
                             ChessManualCallBack cb) {
        this.chessManualPane = chessManualPane;
        this.menuOfChessNotation = menuOfChessNotation;
        this.menuOfShowTactic = menuOfShowTactic;
        this.notationTree = notationTree;
        this.notationTree.getStyleClass().add("notation-tree");
        this.manualTitleLabel = manualTitleLabel;
        this.recordTable = recordTable;
        this.subRecordTable = subRecordTable;
        this.remarkText = remarkText;
        this.manualBackButton = manualBackButton;
        this.manualDeleteButton = manualDeleteButton;
        this.manualDownButton = manualDownButton;
        this.manualFinalButton = manualFinalButton;
        this.manualForwardButton = manualForwardButton;
        this.manualFrontButton = manualFrontButton;
        this.manualPlayButton = manualPlayButton;
        this.manualUpButton = manualUpButton;
        this.openManualButton = openManualButton;
        this.saveManualButton = saveManualButton;
        this.manualScoreButton = manualScoreButton;
        this.competitionNameText = competitionNameText;
        this.competitionCityText = competitionCityText;
        this.competitionDateText = competitionDateText;
        this.competitionRedText = competitionRedText;
        this.competitionBlackText = competitionBlackText;

        this.cb = cb;

        prop = Properties.getInstance();

        initTreeView();
        initRecordTable();
        initRemarkText();
        initMenu();
        initButton();

        refreshManualTree();
    }

    private void refreshManualTree() {
        if (!StringUtils.isEmpty(prop.getChessManualPath())) {
            openChessNotationFolder(prop.getChessManualPath());
        }
    }

    private void updateManualTree(File savedFile) {
        if (!savedFile.isFile()) return;
        if (notationTree.getRoot() instanceof ManualDirectoryTreeItem root) {
            root.savedFile(savedFile);
        } else {
            refreshManualTree();
        }
    }

    private void initButton() {
        manualDownButton.setTooltip(new Tooltip("下变"));
        manualFrontButton.setTooltip(new Tooltip("开局"));
        manualPlayButton.setTooltip(new Tooltip("播放棋谱"));
        manualUpButton.setTooltip(new Tooltip("上变"));
        manualDeleteButton.setTooltip(new Tooltip("删除棋谱"));
        manualForwardButton.setTooltip(new Tooltip("前进"));
        manualBackButton.setTooltip(new Tooltip("后退"));
        manualFinalButton.setTooltip(new Tooltip("终局"));
        openManualButton.setTooltip(new Tooltip("打开棋谱"));
        saveManualButton.setTooltip(new Tooltip("保存棋谱"));
        manualScoreButton.setTooltip(new Tooltip("棋谱打分"));
    }

    private void initMenu() {
        menuOfChessNotation.setSelected(prop.isShowChessNotation());
        showChessManualPane(prop.isShowChessNotation());
        menuOfChessNotation.setOnAction(e -> {
            CheckMenuItem item = (CheckMenuItem) e.getTarget();
            prop.setShowChessNotation(item.isSelected());
            showChessManualPane(item.isSelected());
        });

        menuOfShowTactic.setSelected(prop.isManualTip());
    }

    private void initRemarkText() {
        applyCommentFontSize();
        remarkText.textProperty().addListener((obs, oldV, newV) -> {
            if (!remarkText.isFocused()) return;

            recordTable.getItems().get(p).setRemark(newV);
            if (StringUtils.isEmpty(oldV) != StringUtils.isEmpty(newV)) {
                refreshRecordView(recordTable.getItems().get(p), null);
            }
        });
    }

    public void applyCommentFontSize() {
        remarkText.setFont(Font.font(remarkText.getFont().getFamily(), prop.getChessManualCommentFontSize()));
    }

    private void initRecordTable() {
        TableColumn<ManualRecord, String> idCol = (TableColumn<ManualRecord, String>) recordTable.getColumns().get(0);
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        TableColumn<ManualRecord, String> nameCol = (TableColumn<ManualRecord, String>) recordTable.getColumns().get(1);
        nameCol.setCellValueFactory(cellData -> {
                String text = cellData.getValue().getCnMove() + "      ";
                if (cellData.getValue().getList().size() > 1) {
                    text += "b";
                }
                if (StringUtils.isNotEmpty(cellData.getValue().getRemark())) {
                    text += "*";
                }
                return new SimpleStringProperty(text);
            }
        );
        TableColumn<ManualRecord, String> scoreCol = (TableColumn<ManualRecord, String>) recordTable.getColumns().get(2);
        scoreCol.setCellValueFactory(new PropertyValueFactory<>("score"));

        subRecordTable.setCellFactory(lv -> {
            ListCell<ManualRecord> cell = new ListCell<>() {
                @Override
                protected void updateItem(ManualRecord item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        int lineNo = getIndex() + 1;
                        setText("(" + lineNo + ")    " + item.getCnMove());
                    }
                }
            };
            cell.addEventHandler(MouseEvent.MOUSE_CLICKED, evt -> {
                if (evt.getButton() == MouseButton.PRIMARY && evt.getClickCount() == 2 && !cell.isEmpty()) {
                    ManualRecord selectRecord = subRecordTable.getSelectionModel().getSelectedItem();
                    List<String> nextList = boardMove(selectRecord.getMove(), selectRecord.getCnMove());
                    this.cb.browseChessRecord(fenCode, getMoveList(), getRedGo(), nextList);
                }
            });
            cell.setOnDragDetected(evt -> {
                if (cell.isEmpty()) {
                    return;
                }
                Dragboard db = cell.startDragAndDrop(TransferMode.MOVE);
                ClipboardContent content = new ClipboardContent();
                content.putString(String.valueOf(cell.getIndex()));
                db.setContent(content);
                evt.consume();
            });
            cell.setOnDragOver(evt -> {
                Dragboard db = evt.getDragboard();
                if (db.hasString()) {
                    int sourceIndex = Integer.parseInt(db.getString());
                    int targetIndex = cell.isEmpty() ? subRecordTable.getItems().size() : cell.getIndex();
                    if (sourceIndex != targetIndex) {
                        evt.acceptTransferModes(TransferMode.MOVE);
                    }
                }
                evt.consume();
            });
            cell.setOnDragDropped(evt -> {
                Dragboard db = evt.getDragboard();
                boolean success = false;
                if (db.hasString()) {
                    int sourceIndex = Integer.parseInt(db.getString());
                    int targetIndex = cell.isEmpty() ? subRecordTable.getItems().size() : cell.getIndex();
                    List<ManualRecord> items = recordTable.getItems().get(p).getList();
                    int next = recordTable.getItems().get(p).getNext();
                    if (sourceIndex >= 0 && sourceIndex < items.size() && sourceIndex != targetIndex && sourceIndex + 1 != targetIndex) {
                        ManualRecord dragged = items.remove(sourceIndex);
                        if (sourceIndex < targetIndex) {
                            targetIndex--;
                        }
                        items.add(targetIndex, dragged);

                        if (next == sourceIndex) {
                            next = targetIndex;
                        } else if (sourceIndex < targetIndex) {
                            if (next > sourceIndex && next <= targetIndex) {
                                next--;
                            }
                        } else {
                            if (next < sourceIndex && next >= targetIndex) {
                                next++;
                            }
                        }

                        recordTable.getItems().get(p).setNext(next);
                        refreshRecordView(null, items);
                        subRecordTable.getSelectionModel().select(targetIndex);
                        this.cb.setNextList(items.stream().map(ManualRecord::getMove).toList());

                        success = true;
                    }
                }
                evt.setDropCompleted(success);
                evt.consume();
            });
            cell.setOnDragDone(evt -> evt.consume());
            return cell;
        });
    }

    public void newChessManual(String fenCode) {
        this.fenCode = fenCode;
        this.manualHead = new ManualRecord(0, "开始局面", 0);
        p = 0;
        manualFile = null;

        competitionNameText.setText("");
        competitionDateText.setText("");
        competitionCityText.setText("");
        competitionRedText.setText("");
        competitionBlackText.setText("");

        manualTitleLabel.setText("未命名");
        remarkText.setText("");
        recordTable.getItems().clear();
        recordTable.getItems().add(manualHead);
        subRecordTable.getItems().clear();
    }

    public List<String> boardMove(String move, String cnMove) {
        if (manualPlayTimeline != null)
            manualPlayTimeline.stop();

        ManualRecord currentRecord = recordTable.getItems().get(p);
        ManualRecord next = null;
        for (int i = 0; i < currentRecord.getList().size(); i++) {
            ManualRecord mr = currentRecord.getList().get(i);
            if (mr.getMove().equals(move)) {
                next = mr;
                currentRecord.setNext(i);
                break;
            }
        }
        if (next == null) {
            next = new ManualRecord(p + 1, move, cnMove);
            currentRecord.getList().add(next);
            currentRecord.setNext(currentRecord.getList().size() - 1);
            refreshRecordView(currentRecord, null);
        }
        setRemarkAndNext(next);
        if (p == recordTable.getItems().size() - 1 || recordTable.getItems().get(p + 1) != next) {
            for (int i = recordTable.getItems().size() - 1; i > p; i--) {
                recordTable.getItems().remove(i);
            }
            do {
                recordTable.getItems().add(next);
                if (next.getList().size() > 0) {
                    next = next.getList().get(next.getNext());
                } else {
                    next = null;
                }
            } while (next != null);
        }
        p++;

        reLocationTable();

        return recordTable.getItems().get(p).getList().stream().map(ManualRecord::getMove).toList();
    }

    private void setRemarkAndNext(ManualRecord mr) {
        remarkText.setText(mr.getRemark());
        subRecordTable.getItems().clear();
        subRecordTable.getItems().addAll(mr.getList());
    }

    public void locateRecord(ManualRecord record) {
        int index = recordTable.getItems().indexOf(record);
        if (index < 0) {
            return;
        }
        recordTable.getSelectionModel().select(index);
        manualButtonClick(5);
    }

    public void manualButtonClick(int action) {
        if (action != 9 && manualPlayTimeline != null) {
            manualPlayTimeline.stop();
        }
        switch (action) {
            case 1: if (p > 0) { p = 0; break; } else { return; }
            case 2: if (p > 0) { p--; break; } else { return; }
            case 9:
            case 3: if (p < recordTable.getItems().size() - 1) { p++; break; } else { return; }
            case 4: if (p < recordTable.getItems().size() - 1) { p = recordTable.getItems().size() - 1; break; } else { return; }
            case 5: {
                int index = recordTable.getSelectionModel().getSelectedIndex();
                if (index != p && index >= 0) {
                    p = index;
                    break;
                } else { return; }
            }
            case 6: {
                if (p > 0) {
                    p -= 2;
                    if (p < 0) {
                        p = 0;
                    }
                    break;
                } else { return; }
            }
            case 7: {
                boolean f = false;
                for (int i = p - 1; i >= 0; i--) {
                    if (recordTable.getItems().get(i).getList().size() > 1) {
                        p = i;
                        f = true;
                        break;
                    }
                }
                if (!f) {
                    return;
                } else {
                    break;
                }
            }
            case 8: {
                boolean f = false;
                for (int i = p + 1; i < recordTable.getItems().size(); i++) {
                    if (recordTable.getItems().get(i).getList().size() > 1) {
                        p = i;
                        f = true;
                        break;
                    }
                }
                if (!f) {
                    return;
                } else {
                    break;
                }
            }
            default: return;
        }
        setRemarkAndNext(recordTable.getItems().get(p));
        reLocationTable();

        this.cb.browseChessRecord(fenCode, getMoveList(), getRedGo(), recordTable.getItems().get(p).getList()
                .stream().map(ManualRecord::getMove).toList());
    }

    private void refreshRecordView(ManualRecord record, List<ManualRecord> subRecordList) {
        if (record != null) {
            recordTable.refresh();
        }
        if (subRecordList != null) {
            subRecordTable.getItems().clear();
            subRecordTable.getItems().addAll(subRecordList);
        }
    }

    public void deleteButtonClick(ActionEvent actionEvent) {
        ManualRecord currentRecord = recordTable.getItems().get(p);
        ManualRecord subRecord = subRecordTable.getSelectionModel().getSelectedItem();
        if (subRecord != null) {
            if (!DialogUtils.showConfirmDialog("确认", "要删除 " + subRecord.getCnMove() + " 及后续所有招法吗？")) {
                return;
            }
            currentRecord.getList().remove(subRecord);
            if (subRecord == recordTable.getItems().get(p + 1)) {
                for (int i = recordTable.getItems().size() - 1; i > p; i--) {
                    recordTable.getItems().remove(i);
                }
                if (currentRecord.getList().size() > 0) {
                    ManualRecord next = currentRecord.getList().get(0);
                    currentRecord.setNext(0);
                    do {
                        recordTable.getItems().add(next);
                        if (next.getList().size() > 0) {
                            next = next.getList().get(next.getNext());
                        } else {
                            next = null;
                        }
                    } while (next != null);
                }
            }
            refreshRecordView(currentRecord, currentRecord.getList());
            this.cb.setNextList(currentRecord.getList().stream().map(ManualRecord::getMove).toList());
        } else {
            if (p == 0) {
                return;
            }
            if (!DialogUtils.showConfirmDialog("确认", "要删除 " + currentRecord.getCnMove() + " 及后续所有招法吗？")) {
                return;
            }
            ManualRecord preRecord = recordTable.getItems().get(p - 1);
            preRecord.getList().remove(currentRecord);
            for (int i = recordTable.getItems().size() - 1; i >= p; i--) {
                recordTable.getItems().remove(i);
            }
            if (preRecord.getList().size() > 0) {
                ManualRecord next = preRecord.getList().get(0);
                preRecord.setNext(0);
                do {
                    recordTable.getItems().add(next);
                    if (next.getList().size() > 0) {
                        next = next.getList().get(next.getNext());
                    } else {
                        next = null;
                    }
                } while (next != null);
            }
            refreshRecordView(preRecord, null);
            manualButtonClick(2);
        }
    }

    public void scoreButtonClick(ActionEvent actionEvent) {
        TextInputDialog d = new TextInputDialog(Integer.toString(prop.getChessManualScoreMillis()));
        d.setTitle("棋谱打分");
        d.setHeaderText("请设置每步打分时间(毫秒)，建议不低于300");
        d.setContentText("");
        d.initOwner(App.getMainStage());
        d.showAndWait().ifPresent(s -> {
            long delay;
            try {
                delay = Long.parseLong(s.trim());
            } catch (NumberFormatException e) {
                delay = 0;
            }
            if (delay <= 0) {
                DialogUtils.showWarningDialog("提示", "每步打分时间须为正整数（毫秒）");
                return;
            }

            if (manualPlayTimeline != null && manualPlayTimeline.getStatus() == Animation.Status.RUNNING) {
                manualPlayTimeline.stop();
            }
            manualPlayTimeline = new Timeline(new KeyFrame(Duration.millis(delay), e -> {
                int size = recordTable.getItems().size();
                if (p < size - 1) {
                    manualButtonClick(9);
                } else {
                    manualPlayTimeline.stop();
                }
            }));
            manualPlayTimeline.statusProperty().addListener((obs, old, status) -> {
                if (status == Animation.Status.STOPPED) {
                    this.cb.turnOffAnalysisMode();
                    this.cb.refreshLineChart();
                }
            });
            manualPlayTimeline.setCycleCount(Animation.INDEFINITE);

            manualButtonClick(1);
            this.cb.turnOnAnalysisMode();
            manualPlayTimeline.play();
        });
    }

    public void playButtonClick(ActionEvent event) {
        if (p == recordTable.getItems().size() - 1) {
            return;
        }
        if (manualPlayTimeline != null && manualPlayTimeline.getStatus() == Animation.Status.RUNNING) {
            manualPlayTimeline.stop();
            return;
        }
        manualPlayTimeline = new Timeline(new KeyFrame(Duration.millis(prop.getChessManualPlaybackMillis()), e -> {
            int size = recordTable.getItems().size();
            if (p < size - 1) {
                manualButtonClick(9);
            } else {
                manualPlayTimeline.stop();
            }
        }));
        manualPlayTimeline.setCycleCount(Animation.INDEFINITE);
        manualPlayTimeline.play();
    }

    public void setScore(Integer score, Integer mate) {
        int s;
        if (mate != null) {
            s = (score < 0 ? -30000 : 30000) - score;
        } else {
            s = score;
        }
        ManualRecord currentRecord = recordTable.getItems().get(p);
        currentRecord.setScore(s);
        refreshRecordView(currentRecord, null);
    }

    public List<XYChart.Data<Number, Number>> getScoreList() {
        List<XYChart.Data<Number, Number>> res = new ArrayList<>();
        for (int i = 0; i < recordTable.getItems().size(); i++) {
            ManualRecord mr = recordTable.getItems().get(i);
            if (mr.getScore() != null) {
                int score = mr.getScore();
                res.add(new XYChart.Data<>(mr.getId(), ScoreScale.toAxisValue(score), mr));
            }
        }
        return res;
    }

    private boolean getRedGo() {
        boolean redGo = fenCode.contains("w");
        if (p % 2 != 0) {
            redGo = !redGo;
        }
        return redGo;
    }

    public List<String> getMoveList() {
        return p > 0 ? recordTable.getItems().stream()
                .map(ManualRecord::getMove).toList().subList(1, p + 1)
                : Collections.emptyList();
    }

    public int getP() {
        return this.p;
    }

    public String getFenCode() {
        return this.fenCode;
    }

    public void copyChessManual() {
        PgnChessManualImpl serv = (PgnChessManualImpl) manualServices.get("pgn");
        String manualStr = "[FEN \"" + this.getFenCode() + "\"]";
        manualStr += serv.getTextFromChessManual(this.manualHead, false);
        manualStr += System.lineSeparator() + "来自TCHESS象棋软件";
        ClipboardUtils.setText(manualStr);
    }

    public void pasteChessManual() {
        PgnChessManualImpl serv = (PgnChessManualImpl) manualServices.get("pgn");
        String txt = ClipboardUtils.getText();
        ChessManual cm = serv.getChessManualFromText(txt);

        openFromChessManual(cm);

        this.manualFile = null;
        manualTitleLabel.setText("未命名");

    }

    public void openChessManualFile(ActionEvent actionEvent) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setInitialDirectory(new File(
                StringUtils.isNotEmpty(prop.getChessManualPath()) ? prop.getChessManualPath() : PathUtils.getJarPath()));
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("全部(*.*)", "*.txq", "*.pgn", "*.xqf", "*.cbr"));
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("txq(*.txq)", "*.txq"));
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("pgn(*.pgn)", "*.pgn"));
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("xqf(*.xqf)", "*.xqf"));
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("cbr(*.cbr)", "*.cbr"));
        File file = fileChooser.showOpenDialog(App.getMainStage());
        if (file != null) {
            openFromFile(file);
        }
    }

    private void openFromFile(File file) {
        String ext = PathUtils.getDotExtension(file).toLowerCase();
        ChessManual cm = manualServices.get(ext).openChessManual(file);

        this.manualFile = file;
        manualTitleLabel.setText(file.getName());

        openFromChessManual(cm);
    }

    private void openFromChessManual(ChessManual cm) {
        this.fenCode = cm.getFenCode();
        this.manualHead = cm.getHead();
        this.p = 0;

        remarkText.setText(manualHead.getRemark());

        competitionNameText.setText(cm.getName());
        competitionCityText.setText(cm.getCity());
        competitionDateText.setText(cm.getDate());
        competitionRedText.setText(cm.getRed());
        competitionBlackText.setText(cm.getBlack());

        recordTable.getItems().clear();
        ManualRecord h = manualHead;
        while (h != null) {
            recordTable.getItems().add(h);
            h = h.getList().isEmpty() ? null : h.getList().get(h.getNext());
        }

        subRecordTable.getItems().clear();
        subRecordTable.getItems().addAll(manualHead.getList());

        this.cb.newChessBoardFromManual(this.fenCode);
    }

    public void saveAsChessManualFile(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setInitialDirectory(new File(
                StringUtils.isNotEmpty(prop.getChessManualPath()) ? prop.getChessManualPath() : PathUtils.getJarPath()));
        fileChooser.setInitialFileName("未命名");
        FileChooser.ExtensionFilter txq = new FileChooser.ExtensionFilter("txq(*.txq)", "*.txq");
        FileChooser.ExtensionFilter pgn = new FileChooser.ExtensionFilter("pgn(*.pgn)", "*.pgn");
        FileChooser.ExtensionFilter xqf = new FileChooser.ExtensionFilter("xqf(*.xqf)", "*.xqf");
        FileChooser.ExtensionFilter cbr = new FileChooser.ExtensionFilter("cbr(*.cbr)", "*.cbr");
        fileChooser.getExtensionFilters().addAll(txq, pgn, xqf, cbr);
        fileChooser.setSelectedExtensionFilter(txq);
        File file = fileChooser.showSaveDialog(App.getMainStage());
        if (file != null) {
            if (StringUtils.isEmpty(PathUtils.getDotExtension(file))) {
                FileChooser.ExtensionFilter selectedFilter = fileChooser.getSelectedExtensionFilter();
                String ext = (selectedFilter == null ? txq : selectedFilter)
                        .getExtensions().get(0).substring(1);
                file = new File(file.getParent(), file.getName() + ext);
            }
            saveToFile(file);

            manualTitleLabel.setText(file.getName());
            this.manualFile = file;
            updateManualTree(file);
        }
    }

    public void saveChessManualFile(ActionEvent event) {
        if (manualFile == null || !Files.exists(manualFile.toPath())) {
            saveAsChessManualFile(event);
        } else {
            saveToFile(manualFile);
        }
    }

    private ChessManual currentChessManual() {
        ChessManual cm = new ChessManual();
        cm.setFenCode(this.fenCode);
        cm.setHead(this.manualHead);
        cm.setName(competitionNameText.getText());
        cm.setCity(competitionCityText.getText());
        cm.setDate(competitionDateText.getText());
        cm.setRed(competitionRedText.getText());
        cm.setBlack(competitionBlackText.getText());
        return cm;
    }

    private void saveToFile(File file) {
        ChessManual cm = currentChessManual();
        String ext = PathUtils.getDotExtension(file).toLowerCase();
        manualServices.get(ext).saveChessManual(cm, file);
    }

    /** 在替换棋盘及清空当前棋谱之前尝试保存，失败时仍继续新建局面。 */
    public void autoSaveIfNeeded() {
        String format = prop.getAutoSaveChessManualFormat();
        if (manualHead == null || manualFile != null || StringUtils.isEmpty(format)) {
            return;
        }

        ChessManual cm = currentChessManual();
        String autoSaveDirectory = prop.getChessManualAutoSaveDirectory();
        Thread.startVirtualThread(() -> {
            try {
                File savedFile = ChessManualAutoSaver.save(cm, autoSaveDirectory, format);
                Platform.runLater(() -> updateManualTree(savedFile));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public void openChessNotationFolder(ActionEvent event) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("选择棋谱目录");
        File init = new File(System.getProperty("user.home"));
        if (init.exists() && init.isDirectory()) {
            chooser.setInitialDirectory(init);
        }
        File dir = chooser.showDialog(App.getMainStage());
        if (dir != null && dir.exists() && dir.isDirectory()) {
            prop.setChessManualPath(dir.getAbsolutePath());
            openChessNotationFolder(dir.getAbsolutePath());
        }
    }

    private void showChessManualPane(boolean isShow) {
        chessManualPane.setVisible(isShow);
        chessManualPane.setManaged(isShow);
    }

    private void reLocationTable() {
        recordTable.getSelectionModel().select(p);
        // 新增棋步或刷新表格后，先完成布局，再读取当前页的实际可见范围。
        recordTable.applyCss();
        recordTable.layout();
        if (!(recordTable.lookup(".virtual-flow") instanceof VirtualFlow<?> flow)) {
            recordTable.scrollTo(p);
            return;
        }
        IndexedCell<?> first = flow.getFirstVisibleCell();
        IndexedCell<?> last = flow.getLastVisibleCell();
        if (first == null || last == null) {
            recordTable.scrollTo(p);
            return;
        }

        int firstIndex = first.getIndex();
        int lastIndex = last.getIndex();
        // 边缘被截断的行归入相邻页，保证选中的棋步完整显示。
        if (first.getLayoutY() < 0) {
            firstIndex++;
        }
        if (last.getLayoutY() + last.getHeight() > flow.getViewportLength()) {
            lastIndex--;
        }
        if (p < firstIndex) {
            int pageSize = Math.max(1, lastIndex - firstIndex + 1);
            recordTable.scrollTo(Math.max(0, p - pageSize + 1));
        } else if (p > lastIndex) {
            recordTable.scrollTo(p);
        }
    }

    public void openChessNotationFolder(String path) {
        File rootDir = path == null ? null : new File(path);
        if (rootDir == null || !rootDir.exists() || !rootDir.isDirectory()) {
            notationTree.setRoot(null);
            return;
        }
        TreeItem<File> root = new ManualDirectoryTreeItem(rootDir, manualServices.keySet());
        notationTree.setShowRoot(false);
        notationTree.setRoot(root);
        root.setExpanded(true);
    }

    private void initTreeView() {
        notationTree.setCellFactory(v -> new TreeCell<>() {
            private ContextMenu ctx;
            {
                MenuItem open = new MenuItem("打开");
                MenuItem rename = new MenuItem("重命名");
                MenuItem delete = new MenuItem("删除");
                open.setOnAction(e -> {
                    doOpen();
                });
                rename.setOnAction(e -> {
                    File f = getItem();
                    if (f == null) return;
                    TextInputDialog d = new TextInputDialog(f.getName());
                    d.setTitle("重命名");
                    d.setHeaderText("输入新名称");
                    d.setContentText("");
                    d.initOwner(App.getMainStage());
                    d.showAndWait().ifPresent(s -> {
                        if (s.trim().isEmpty()) return;
                        File target = new File(f.getParentFile(), s);
                        try {
                            Files.move(f.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE);
                            renameManualTreeItem(notationTree, getTreeItem(), target);
                            updateItem(target, false);
                        } catch (Exception ex) {
                            try {
                                boolean ok = f.renameTo(target);
                                if (ok) {
                                    renameManualTreeItem(notationTree, getTreeItem(), target);
                                    updateItem(target, false);
                                }
                            } catch (Exception ignore) {
                            }
                        }
                    });
                });
                delete.setOnAction(e -> {
                    File f = getItem();
                    if (f == null) return;
                    deleteFile(f);
                    TreeItem<File> p = getTreeItem().getParent();
                    if (p != null) {
                        p.getChildren().remove(getTreeItem());
                    } else {
                        notationTree.setRoot(null);
                    }
                });
                ctx = new ContextMenu(open, rename, delete);
                addEventHandler(MouseEvent.MOUSE_CLICKED, evt -> {
                    if (evt.getButton() == MouseButton.PRIMARY && evt.getClickCount() == 2 && !isEmpty()) {
                        doOpen();
                    }
                });
            }
            private void doOpen() {
                File f = getItem();
                if (f == null) return;
                if (f.isDirectory()) {
                    TreeItem<File> ti = getTreeItem();
                    ti.setExpanded(!ti.isExpanded());
                } else if (isManualFile(f)) {
                    openFromFile(f);
                }
            }
            @Override
            protected void updateItem(File item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setContextMenu(null);
                    setGraphic(null);
                } else {
                    setText(item.getName());
                    setContextMenu(ctx);
                }
            }
        });
    }

    static void renameManualTreeItem(TreeView<File> tree, TreeItem<File> item, File target) {
        TreeItem<File> selected = tree.getSelectionModel().getSelectedItem();
        item.setValue(target);
        // 子项排序会触发列表替换事件，恢复仍在树内的原选中节点。
        if (selected != null && tree.getRow(selected) >= 0) {
            tree.getSelectionModel().select(selected);
        }
    }

    private boolean isManualFile(File f) {
        return f.isFile() && manualServices.containsKey(PathUtils.getDotExtension(f).toLowerCase());
    }

    private void deleteFile(File f) {
        if (f.isDirectory()) {
            File[] list = f.listFiles();
            if (list != null) {
                for (File c : list) {
                    deleteFile(c);
                }
            }
        }
        try {
            Files.deleteIfExists(f.toPath());
        } catch (Exception e) {
            f.delete();
        }
    }

}
