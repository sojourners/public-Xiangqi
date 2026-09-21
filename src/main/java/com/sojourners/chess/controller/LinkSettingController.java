package com.sojourners.chess.controller;

import com.sojourners.chess.App;
import com.sojourners.chess.config.Properties;
import com.sojourners.chess.linker.LinkScheme;
import com.sojourners.chess.linker.WindowsGraphLinker;
import com.sojourners.chess.mouse.GlobalMouseListener;
import com.sojourners.chess.mouse.MouseListenCallBack;
import com.sojourners.chess.util.DialogUtils;
import com.sojourners.chess.util.StringUtils;
import com.sun.jna.platform.win32.WinDef;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.util.List;


public class LinkSettingController {

    private static final String DEFAULT_SCHEME = "手动选择窗口";

    @FXML
    private ComboBox<String> schemeComboBox;
    @FXML
    private TextField schemeName;
    @FXML
    private TextField schemeWindowTitle;
    @FXML
    private TextField schemeWindowClass;
    @FXML
    private TextField schemeClickTitle;
    @FXML
    private TextField schemeClickClass;
    @FXML
    private TextField schemeScaleFactor;
    @FXML
    private Button pickWindowButton;
    @FXML
    private Label pickHintLabel;

    @FXML
    private TextField linkScanTime;
    @FXML
    private TextField linkThreadNum;

    @FXML
    private TextField mouseClickDelay;

    @FXML
    private TextField mouseMoveDelay;

    private Properties prop;
    private boolean refreshing;

    @FXML
    void cancelButtonClick(ActionEvent e) {
        App.closeLinkSetting();
    }

    @FXML
    void okButtonClick(ActionEvent e) {
        if (!saveCurrentScheme()) {
            return;
        }

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

        prop.save();
        App.closeLinkSetting();
    }

    /**
     * 把界面上的方案字段保存到当前方案
     */
    private boolean saveCurrentScheme() {
        LinkScheme s = getCurrentScheme();
        if (s == null) {
            return true;
        }
        String name = schemeName.getText().trim();
        if (name.isEmpty()) {
            DialogUtils.showErrorDialog("失败", "方案名称不能为空");
            return false;
        }
        String sf = schemeScaleFactor.getText().trim();
        if (!StringUtils.isPositiveNumber(sf)) {
            DialogUtils.showErrorDialog("失败", "点击缩放比必须为正数");
            return false;
        }
        double scale = Double.parseDouble(sf);
        if (scale <= 0 || scale > 4) {
            DialogUtils.showErrorDialog("失败", "点击缩放比范围应为 0~4");
            return false;
        }

        boolean nameChanged = !name.equals(s.getName());
        s.setName(name);
        s.setWindowTitle(schemeWindowTitle.getText().trim());
        s.setWindowClass(schemeWindowClass.getText().trim());
        s.setClickTitle(schemeClickTitle.getText().trim());
        s.setClickClass(schemeClickClass.getText().trim());
        s.setScaleFactor(scale);
        prop.setSelectedLinkScheme(name);
        if (nameChanged) {
            refreshSchemeCombo();
            selectScheme(name);
        }
        return true;
    }

    @FXML
    void newSchemeButtonClick(ActionEvent e) {
        if (!saveCurrentScheme()) {
            return;
        }
        LinkScheme s = new LinkScheme("新方案" + System.currentTimeMillis() % 10000, "", "", "", "");
        prop.getLinkSchemeList().add(s);
        prop.setSelectedLinkScheme(s.getName());
        refreshSchemeCombo();
        selectScheme(s.getName());
    }

    @FXML
    void deleteSchemeButtonClick(ActionEvent e) {
        LinkScheme s = getCurrentScheme();
        if (s == null) {
            return;
        }
        if (DEFAULT_SCHEME.equals(s.getName())) {
            DialogUtils.showWarningDialog("提示", "默认方案不可删除");
            return;
        }
        prop.getLinkSchemeList().remove(s);
        prop.setSelectedLinkScheme(DEFAULT_SCHEME);
        refreshSchemeCombo();
        selectScheme(DEFAULT_SCHEME);
    }

    /**
     * 拾取窗口：点击按钮后去点一下目标游戏窗口，
     * 自动把前台窗口的标题/类名填入当前方案（借鉴 VinXiangQi 获取句柄）
     */
    @FXML
    void pickWindowButtonClick(ActionEvent e) {
        if (!saveCurrentScheme()) {
            return;
        }
        pickWindowButton.setDisable(true);
        pickHintLabel.setText("请点击目标游戏窗口（3秒内）...");

        GlobalMouseListener listener = new GlobalMouseListener(new MouseListenCallBack() {
            @Override
            public void mouseClick() {
                try {
                    Thread.sleep(300);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
                Platform.runLater(() -> {
                    WinDef.HWND h = WindowsGraphLinker.getForegroundWindow();
                    if (h != null) {
                        schemeWindowTitle.setText(WindowsGraphLinker.getWindowTitle(h));
                        schemeWindowClass.setText(WindowsGraphLinker.getWindowClass(h));
                        saveCurrentScheme();
                    }
                    pickWindowButton.setDisable(false);
                    pickHintLabel.setText("已获取窗口信息，可修改后点确定保存");
                });
            }
        });
        try {
            listener.startListenMouse();
        } catch (Exception ex) {
            ex.printStackTrace();
            pickWindowButton.setDisable(false);
            pickHintLabel.setText("点击后去点一下目标游戏窗口");
        }
    }

    public void initialize() {
        prop = Properties.getInstance();
        if (prop.getLinkSchemeList() == null) {
            return;
        }
        refreshSchemeCombo();
        selectScheme(prop.getSelectedLinkScheme());

        linkScanTime.setText(String.valueOf(prop.getLinkScanTime()));
        linkThreadNum.setText(String.valueOf(prop.getLinkThreadNum()));
        mouseClickDelay.setText(String.valueOf(prop.getMouseClickDelay()));
        mouseMoveDelay.setText(String.valueOf(prop.getMouseMoveDelay()));
    }

    private void refreshSchemeCombo() {
        refreshing = true;
        schemeComboBox.getItems().clear();
        for (LinkScheme s : prop.getLinkSchemeList()) {
            schemeComboBox.getItems().add(s.getName());
        }
        refreshing = false;
    }

    private void selectScheme(String name) {
        if (name != null && schemeComboBox.getItems().contains(name)) {
            schemeComboBox.setValue(name);
        } else if (!schemeComboBox.getItems().isEmpty()) {
            schemeComboBox.setValue(schemeComboBox.getItems().get(0));
        }
        loadSchemeFields();
    }

    private void loadSchemeFields() {
        LinkScheme s = getCurrentScheme();
        if (s == null) {
            return;
        }
        schemeName.setText(s.getName());
        schemeWindowTitle.setText(s.getWindowTitle() == null ? "" : s.getWindowTitle());
        schemeWindowClass.setText(s.getWindowClass() == null ? "" : s.getWindowClass());
        schemeClickTitle.setText(s.getClickTitle() == null ? "" : s.getClickTitle());
        schemeClickClass.setText(s.getClickClass() == null ? "" : s.getClickClass());
        schemeScaleFactor.setText(String.valueOf(s.getScaleFactor()));
    }

    private LinkScheme getCurrentScheme() {
        if (prop == null || prop.getLinkSchemeList() == null) {
            return null;
        }
        String name = schemeComboBox.getValue();
        if (name == null) {
            return null;
        }
        for (LinkScheme s : prop.getLinkSchemeList()) {
            if (name.equals(s.getName())) {
                return s;
            }
        }
        return null;
    }

}
