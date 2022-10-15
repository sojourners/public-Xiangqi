package com.sojourners.chess.controller;

import com.sojourners.chess.App;
import com.sojourners.chess.config.Properties;
import com.sojourners.chess.enginee.Engine;
import com.sojourners.chess.openbook.MoveRule;
import com.sojourners.chess.openbook.OpenBookManager;
import com.sojourners.chess.util.DialogUtils;
import com.sojourners.chess.util.StringUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;


public class BattleSettingController {

    @FXML
    private RadioButton fixTimeButton;

    @FXML
    private TextField timeText;

    @FXML
    private RadioButton fixDepthButton;

    @FXML
    private TextField depthText;

    @FXML
    private CheckBox useCloudBook;

    @FXML
    private TextField cloudBookTimeout;

    @FXML
    private CheckBox onlyCloudFinalPhase;

    @FXML
    private CheckBox localBookFirst;

    @FXML
    private RadioButton bestScoreRadioButton;

    @FXML
    private RadioButton bestWinrateRadioButton;

    @FXML
    private RadioButton positiveRandomRadioButton;

    @FXML
    private RadioButton fullRandomRadioButton;

    @FXML
    private TextField offManualSteps;

    private Properties prop;

    @FXML
    void cancelButtonClick(ActionEvent e) {
        App.closeBattleSetting();
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

        String timeOut = cloudBookTimeout.getText();
        String offSteps = offManualSteps.getText();
        if (!StringUtils.isPositiveInt(timeOut) || !StringUtils.isPositiveInt(offSteps)) {
            DialogUtils.showErrorDialog("失败", "超时时间或脱谱步数错误");
            return;
        }

        prop.setUseCloudBook(useCloudBook.isSelected());
        prop.setCloudBookTimeout(Integer.parseInt(timeOut));
        prop.setOnlyCloudFinalPhase(onlyCloudFinalPhase.isSelected());
        prop.setLocalBookFirst(localBookFirst.isSelected());
        if (bestScoreRadioButton.isSelected()) {
            prop.setMoveRule(MoveRule.BEST_SCORE);
        } else if (bestWinrateRadioButton.isSelected()) {
            prop.setMoveRule(MoveRule.BEST_WINRATE);
        } else if (positiveRandomRadioButton.isSelected()) {
            prop.setMoveRule(MoveRule.POSITIVE_RANDOM);
        } else {
            prop.setMoveRule(MoveRule.FULL_RANDOM);
        }
        prop.setOffManualSteps(Integer.parseInt(offSteps));

        App.closeBattleSetting();
    }

    @FXML
    void localBookManageButtonClick(ActionEvent event) {
        if (App.openLocalBookDialog()) {
            OpenBookManager.getInstance().setLocalOpenBooks();
        }
    }

    public void initialize() {

        ToggleGroup group = new ToggleGroup();
        fixTimeButton.setToggleGroup(group);
        fixDepthButton.setToggleGroup(group);

        ToggleGroup group2 = new ToggleGroup();
        bestScoreRadioButton.setToggleGroup(group2);
        bestWinrateRadioButton.setToggleGroup(group2);
        positiveRandomRadioButton.setToggleGroup(group2);
        fullRandomRadioButton.setToggleGroup(group2);

        prop = Properties.getInstance();
        if (prop.getAnalysisModel() == Engine.AnalysisModel.FIXED_TIME) {
            fixTimeButton.setSelected(true);
            timeText.setText(String.valueOf(prop.getAnalysisValue()));
        } else {
            fixDepthButton.setSelected(true);
            depthText.setText(String.valueOf(prop.getAnalysisValue()));
        }

        useCloudBook.setSelected(prop.getUseCloudBook());
        cloudBookTimeout.setText(String.valueOf(prop.getCloudBookTimeout()));
        onlyCloudFinalPhase.setSelected(prop.getOnlyCloudFinalPhase());
        localBookFirst.setSelected(prop.getLocalBookFirst());
        switch (prop.getMoveRule()) {
            case BEST_SCORE -> bestScoreRadioButton.setSelected(true);
            case BEST_WINRATE -> bestWinrateRadioButton.setSelected(true);
            case POSITIVE_RANDOM -> positiveRandomRadioButton.setSelected(true);
            case FULL_RANDOM -> fullRandomRadioButton.setSelected(true);
        }
        offManualSteps.setText(String.valueOf(prop.getOffManualSteps()));
    }

}
