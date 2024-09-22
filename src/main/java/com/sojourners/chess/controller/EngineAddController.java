package com.sojourners.chess.controller;

import com.sojourners.chess.App;
import com.sojourners.chess.config.Properties;
import com.sojourners.chess.enginee.Engine;
import com.sojourners.chess.model.EngineConfig;
import com.sojourners.chess.util.PathUtils;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.util.Callback;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

public class EngineAddController {


    private Properties prop;

    @FXML
    private TextField pathText;

    @FXML
    private TextField nameText;

    @FXML
    private TextField protocolText;

    @FXML
    private ListView<Map.Entry<String, String>> optionsListView;

    public static EngineConfig ec;

    private LinkedHashMap<String, String> options;

    @FXML
    void selectButtonClick(ActionEvent e) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setInitialDirectory(new File(PathUtils.getJarPath()));
        File file = fileChooser.showOpenDialog(App.getEngineAdd());
        if (file != null) {
            pathText.setText(file.getPath());
            nameText.setText(file.getName());
            String protocol = Engine.test(file.getPath(), options = new LinkedHashMap<>());
            if (protocol == null) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("提示");
                alert.setHeaderText("无效的引擎文件");
            }
            protocolText.setText(protocol);
            showOptions();
        }
    }

    private void showOptions() {
        optionsListView.getItems().clear();
        for (Map.Entry<String, String> entry : options.entrySet()) {
            optionsListView.getItems().add(entry);
        }
    }

    @FXML
    void cancelButtonClick(ActionEvent event) {
        App.closeEngineAdd();
    }

    @FXML
    void okButtonClick(ActionEvent event) {
        String protocol = protocolText.getText();
        if (!"uci".equals(protocol) && !"ucci".equals(protocol)) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("提示");
            alert.setHeaderText("引擎协议不正确");
            return;
        }
        if (ec == null) {
            // 添加引擎
            prop.getEngineConfigList().add(new EngineConfig(nameText.getText(), pathText.getText(), protocolText.getText(), options));
        } else {
            // 编辑引擎
            ec.setName(nameText.getText());
            ec.setPath(pathText.getText());
            ec.setProtocol(protocolText.getText());
            ec.setOptions(options);
        }
        App.closeEngineAdd();
    }

    public void initialize() {
        prop = Properties.getInstance();

        initListView();

        if (ec != null) {
            nameText.setText(ec.getName());
            pathText.setText(ec.getPath());
            protocolText.setText(ec.getProtocol());

            this.options = (LinkedHashMap<String, String>) ec.getOptions().clone();
            showOptions();
        }
    }

    private void initListView() {
        optionsListView.setSelectionModel(new MultipleSelectionModel<>() {
            private ObservableList emptyList = FXCollections.emptyObservableList();

            @Override
            public ObservableList<Integer> getSelectedIndices() {
                return emptyList;
            }

            @Override
            public ObservableList<Map.Entry<String, String>> getSelectedItems() {
                return emptyList;
            }

            @Override
            public void selectIndices(int i, int... ints) {

            }

            @Override
            public void selectAll() {

            }

            @Override
            public void selectFirst() {

            }

            @Override
            public void selectLast() {

            }

            @Override
            public void clearAndSelect(int i) {

            }

            @Override
            public void select(int i) {

            }

            @Override
            public void select(Map.Entry<String, String> stringStringEntry) {

            }

            @Override
            public void clearSelection(int i) {

            }

            @Override
            public void clearSelection() {

            }

            @Override
            public boolean isSelected(int i) {
                return false;
            }

            @Override
            public boolean isEmpty() {
                return true;
            }

            @Override
            public void selectPrevious() {

            }

            @Override
            public void selectNext() {

            }
        });
        optionsListView.setCellFactory(new Callback() {
            @Override
            public Object call(Object param) {
                ListCell<Map.Entry<String, String>> cell = new ListCell<>() {
                    @Override
                    protected void updateItem(Map.Entry<String, String> item, boolean bln) {
                        super.updateItem(item, bln);
                        if (!bln) {
                            HBox box = new HBox();

                            Label label = new Label();
                            label.setText(item.getKey());
                            label.setAlignment(Pos.CENTER_LEFT);
                            label.setPrefHeight(27);
                            label.setPrefWidth(100);
                            box.getChildren().add(label);

                            TextField input = new TextField();
                            input.setText(item.getValue());
                            input.setPrefWidth(120);
                            input.textProperty().addListener(new ChangeListener<String>() {
                                @Override
                                public void changed(ObservableValue<? extends String> observableValue, String s, String t1) {
                                    options.put(item.getKey(), t1);
                                }
                            });
                            box.getChildren().add(input);

                            setGraphic(box);
                        }
                    }
                };
                return cell;
            }

        });
    }
}
