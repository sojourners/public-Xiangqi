package com.sojourners.chess.controller;

import com.sojourners.chess.config.Properties;
import com.sojourners.chess.model.LocalBook;
import com.sojourners.chess.util.PathUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;

import java.io.File;

public class LocalBookController {
    @FXML
    private TableView table;

    private Properties prop;

    public static boolean change;

    @FXML
    void addButtonClick(ActionEvent e) {

        FileChooser fileChooser = new FileChooser();
        fileChooser.setInitialDirectory(new File(PathUtils.getJarPath()));
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("obk(*.obk)", "*.obk"));
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("pfBook(*.pfBook)", "*.pfBook"));
        File file = fileChooser.showOpenDialog(null);
        if (file != null) {
            prop.getOpenBookList().add(file.getPath());
            refreshTable();
        }
    }

    @FXML
    void deleteButtonClick(ActionEvent event) {
        int index = table.getSelectionModel().getSelectedIndex();
        if (index >= 0) {
            prop.getOpenBookList().remove(index);
            refreshTable();

        }
    }

    @FXML
    void upButtonClick(ActionEvent event) {
        int index = table.getSelectionModel().getSelectedIndex();
        if (index > 0 && index < table.getItems().size()) {
            String lb = prop.getOpenBookList().remove(index);
            prop.getOpenBookList().add(index - 1, lb);
            refreshTable();
            table.getSelectionModel().select(index - 1);

        }
    }

    @FXML
    void downButtonClick(ActionEvent event) {
        int index = table.getSelectionModel().getSelectedIndex();
        if (index >= 0 && index < table.getItems().size() - 1) {
            String lb = prop.getOpenBookList().remove(index);
            prop.getOpenBookList().add(index + 1, lb);
            refreshTable();
            table.getSelectionModel().select(index + 1);

        }
    }

    private void refreshTable() {
        table.getItems().clear();
        for (String book : prop.getOpenBookList()) {
            table.getItems().add(new LocalBook(book));
        }

        this.change = true;
    }

    public void initialize() {

        TableColumn nameCol = (TableColumn) table.getColumns().get(0);
        nameCol.setCellValueFactory(new PropertyValueFactory<LocalBook, String>("path"));

        prop = Properties.getInstance();

        refreshTable();

        this.change = false;

    }

}
