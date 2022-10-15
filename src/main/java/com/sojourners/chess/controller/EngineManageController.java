package com.sojourners.chess.controller;

import com.sojourners.chess.App;
import com.sojourners.chess.config.Properties;
import com.sojourners.chess.model.EngineConfig;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

public class EngineManageController {

    @FXML
    private TableView table;

    private Properties prop;


    @FXML
    void addButtonClick(ActionEvent e) {
        EngineAddController.ec = null;
        App.openEngineAdd();
        refreshTable();
    }

    @FXML
    void editButtonClick(ActionEvent e) {
        EngineConfig ec = (EngineConfig) table.getSelectionModel().getSelectedItem();
        if (ec != null) {
            EngineAddController.ec = ec;
            App.openEngineAdd();
            refreshTable();
        }
    }

    @FXML
    void deleteButtonClick(ActionEvent event) {
        int index = table.getSelectionModel().getSelectedIndex();
        if (index >= 0) {
            EngineConfig e = prop.getEngineConfigList().remove(index);
            if (prop.getEngineName().equals(e.getName())) {
                prop.setEngineName("");
            }
            refreshTable();
        }
    }

    private void refreshTable() {
        table.getItems().clear();
        for (EngineConfig ec : prop.getEngineConfigList()) {
            table.getItems().add(ec);
        }
    }

    public void initialize() {

        TableColumn nameCol = (TableColumn) table.getColumns().get(0);
        nameCol.setCellValueFactory(new PropertyValueFactory<EngineConfig, String>("name"));
        TableColumn pathCol = (TableColumn) table.getColumns().get(1);
        pathCol.setCellValueFactory(new PropertyValueFactory<EngineConfig, String>("path"));
        TableColumn proCol = (TableColumn) table.getColumns().get(2);
        proCol.setCellValueFactory(new PropertyValueFactory<EngineConfig, String>("protocol"));

        prop = Properties.getInstance();

        refreshTable();

    }
}
