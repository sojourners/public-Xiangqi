package com.sojourners.chess.controller;

import com.sojourners.chess.App;
import com.sojourners.chess.config.Properties;
import com.sojourners.chess.enginee.Engine;
import com.sojourners.chess.util.DialogUtils;
import com.sojourners.chess.util.StringUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;


public class TimeSettingController {

    @FXML
    private RadioButton fixTimeButton;

    @FXML
    private TextField timeText;

    @FXML
    private RadioButton fixDepthButton;

    @FXML
    private TextField depthText;

    @FXML
    private TextField engineDelayStart;

    @FXML
    private TextField engineDelayEnd;

    @FXML
    private TextField bookDelayStart;

    @FXML
    private TextField bookDelayEnd;


    private Properties prop;

    @FXML
    void cancelButtonClick(ActionEvent e) {
        App.closeTimeSetting();
    }

    @FXML
    void okButtonClick(ActionEvent e) {
        if (fixDepthButton.isSelected()) {
            String txt = depthText.getText();
            if (!StringUtils.isPositiveInt(txt)) {
                DialogUtils.showErrorDialog("失败", "层数错误");
                return;
            }
            prop.setAnalysisModel(Engine.AnalysisModel.FIXED_STEPS);
            prop.setAnalysisValue(Long.parseLong(txt));
        } else {
            String txt = timeText.getText();
            if (!StringUtils.isPositiveInt(txt)) {
                DialogUtils.showErrorDialog("失败", "时间错误");
                return;
            }
            prop.setAnalysisModel(Engine.AnalysisModel.FIXED_TIME);
            prop.setAnalysisValue(Long.parseLong(txt));
        }

        String txt = engineDelayStart.getText();
        if (!StringUtils.isNonNegativeInt(txt)) {
            DialogUtils.showErrorDialog("失败", "输入引擎出招延迟错误");
            return;
        }
        prop.setEngineDelayStart(Integer.parseInt(txt));
        txt = engineDelayEnd.getText();
        if (!StringUtils.isNonNegativeInt(txt)) {
            DialogUtils.showErrorDialog("失败", "输入引擎出招延迟错误");
            return;
        }
        prop.setEngineDelayEnd(Integer.parseInt(txt));

        txt = bookDelayStart.getText();
        if (!StringUtils.isNonNegativeInt(txt)) {
            DialogUtils.showErrorDialog("失败", "输入库招出招延迟错误");
            return;
        }
        prop.setBookDelayStart(Integer.parseInt(txt));
        txt = bookDelayEnd.getText();
        if (!StringUtils.isNonNegativeInt(txt)) {
            DialogUtils.showErrorDialog("失败", "输入库招出招延迟错误");
            return;
        }
        prop.setBookDelayEnd(Integer.parseInt(txt));

        App.closeTimeSetting();
    }


    public void initialize() {

        ToggleGroup group = new ToggleGroup();
        fixTimeButton.setToggleGroup(group);
        fixDepthButton.setToggleGroup(group);

        prop = Properties.getInstance();
        if (prop.getAnalysisModel() == Engine.AnalysisModel.FIXED_TIME) {
            fixTimeButton.setSelected(true);
            timeText.setText(String.valueOf(prop.getAnalysisValue()));
        } else {
            fixDepthButton.setSelected(true);
            depthText.setText(String.valueOf(prop.getAnalysisValue()));
        }

        engineDelayStart.setText(String.valueOf(prop.getEngineDelayStart()));
        engineDelayEnd.setText(String.valueOf(prop.getEngineDelayEnd()));

        bookDelayStart.setText(String.valueOf(prop.getBookDelayStart()));
        bookDelayEnd.setText(String.valueOf(prop.getBookDelayEnd()));

    }

}
