package com.sojourners.chess.controller;

import com.sojourners.chess.App;
import com.sojourners.chess.config.Properties;
import com.sojourners.chess.util.DialogUtils;
import com.sojourners.chess.util.StringUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;


public class LinkSettingController {

    @FXML
    private TextField linkScanTime;
    @FXML
    private TextField linkThreadNum;
    @FXML
    private CheckBox linkAnimation;
    @FXML
    private CheckBox linkShowInfo;
    @FXML
    private CheckBox linkBackMode;

    @FXML
    private TextField engineDelayStart;

    @FXML
    private TextField engineDelayEnd;

    @FXML
    private TextField bookDelayStart;

    @FXML
    private TextField bookDelayEnd;

    @FXML
    private TextField mouseClickDelay;

    @FXML
    private TextField mouseMoveDelay;

    private Properties prop;

    @FXML
    void cancelButtonClick(ActionEvent e) {
        App.closeLinkSetting();
    }

    @FXML
    void okButtonClick(ActionEvent e) {

        String txt = linkScanTime.getText();
        if (!StringUtils.isPositiveInt(txt)) {
            DialogUtils.showErrorDialog("失败", "输入扫描时间错误");
            return;
        }
        prop.setLinkScanTime(Long.parseLong(txt));
        txt = linkThreadNum.getText();
        if (!StringUtils.isPositiveInt(txt)) {
            DialogUtils.showErrorDialog("失败", "输入扫描扫描线程数量错误");
            return;
        }
        prop.setLinkThreadNum(Integer.parseInt(txt));

        prop.setLinkAnimation(linkAnimation.isSelected());
        prop.setLinkShowInfo(linkShowInfo.isSelected());
        prop.setLinkBackMode(linkBackMode.isSelected());

        txt = engineDelayStart.getText();
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

        txt = mouseClickDelay.getText();
        if (!StringUtils.isNonNegativeInt(txt)) {
            DialogUtils.showErrorDialog("失败", "输入鼠标点击延迟错误");
            return;
        }
        prop.setMouseClickDelay(Integer.parseInt(txt));
        txt = mouseMoveDelay.getText();
        if (!StringUtils.isNonNegativeInt(txt)) {
            DialogUtils.showErrorDialog("失败", "输入鼠标走子延迟错误");
            return;
        }
        prop.setMouseMoveDelay(Integer.parseInt(txt));

        App.closeLinkSetting();


    }

    public void initialize() {

        prop = Properties.getInstance();

        linkScanTime.setText(String.valueOf(prop.getLinkScanTime()));
        linkThreadNum.setText(String.valueOf(prop.getLinkThreadNum()));
        linkAnimation.setSelected(prop.isLinkAnimation());
        linkShowInfo.setSelected(prop.isLinkShowInfo());
        linkBackMode.setSelected(prop.isLinkBackMode());

        engineDelayStart.setText(String.valueOf(prop.getEngineDelayStart()));
        engineDelayEnd.setText(String.valueOf(prop.getEngineDelayEnd()));

        bookDelayStart.setText(String.valueOf(prop.getBookDelayStart()));
        bookDelayEnd.setText(String.valueOf(prop.getBookDelayEnd()));

        mouseClickDelay.setText(String.valueOf(prop.getMouseClickDelay()));
        mouseMoveDelay.setText(String.valueOf(prop.getMouseMoveDelay()));

    }

}
