package com.sojourners.chess.controller;

import com.sojourners.chess.App;
import com.sojourners.chess.config.Properties;
import com.sojourners.chess.enginee.Engine;
import com.sojourners.chess.model.EngineConfig;
import com.sojourners.chess.util.PathUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;

import java.io.File;

public class EngineAddController {


    private Properties prop;

    @FXML
    private TextField pathText;

    @FXML
    private TextField nameText;

    @FXML
    private TextField protocolText;

    public static EngineConfig ec;

    @FXML
    void selectButtonClick(ActionEvent e) {

        FileChooser fileChooser = new FileChooser();
        fileChooser.setInitialDirectory(new File(PathUtils.getJarPath()));
        File file = fileChooser.showOpenDialog(null);
        if (file != null) {
            pathText.setText(file.getPath());
            nameText.setText(file.getName());
            String protocol = Engine.test(file.getPath());
            if (protocol == null) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("提示");
                alert.setHeaderText("无效的引擎文件");
                return;
            }
            protocolText.setText(protocol);
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
            prop.getEngineConfigList().add(new EngineConfig(nameText.getText(), pathText.getText(), protocolText.getText()));
        } else {
            ec.setName(nameText.getText());
            ec.setPath(pathText.getText());
            ec.setProtocol(protocolText.getText());
        }
        App.closeEngineAdd();
    }

    public void initialize() {
        prop = Properties.getInstance();

        if (ec != null) {
            nameText.setText(ec.getName());
            pathText.setText(ec.getPath());
            protocolText.setText(ec.getProtocol());
        }
    }
}
