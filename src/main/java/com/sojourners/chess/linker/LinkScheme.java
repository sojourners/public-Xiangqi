package com.sojourners.chess.linker;

import java.io.Serializable;

/**
 * 连线方案（借鉴 VinXiangQi 的方案/窗口句柄适配）：
 * 每个平台保存一份“截图窗口 + 点击窗口”的匹配信息，
 * 启动连线时按标题/类名自动找到游戏窗口，无需每次手动点选。
 */
public class LinkScheme implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 方案名称 */
    private String name;

    /** 截图窗口标题（空=不限，匹配时按包含匹配） */
    private String windowTitle;

    /** 截图窗口类名（空=不限，匹配时按精确匹配） */
    private String windowClass;

    /** 点击子窗口标题（空=点击主窗口；用于部分游戏需向子窗口发送点击） */
    private String clickTitle;

    /** 点击子窗口类名（空=点击主窗口） */
    private String clickClass;

    /** 后台连线点击坐标缩放比（DPI/窗口缩放微调，默认1.0） */
    private double scaleFactor = 1.0d;

    public LinkScheme() {
    }

    public LinkScheme(String name, String windowTitle, String windowClass, String clickTitle, String clickClass) {
        this.name = name;
        this.windowTitle = windowTitle;
        this.windowClass = windowClass;
        this.clickTitle = clickTitle;
        this.clickClass = clickClass;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getWindowTitle() {
        return windowTitle;
    }

    public void setWindowTitle(String windowTitle) {
        this.windowTitle = windowTitle;
    }

    public String getWindowClass() {
        return windowClass;
    }

    public void setWindowClass(String windowClass) {
        this.windowClass = windowClass;
    }

    public String getClickTitle() {
        return clickTitle;
    }

    public void setClickTitle(String clickTitle) {
        this.clickTitle = clickTitle;
    }

    public String getClickClass() {
        return clickClass;
    }

    public void setClickClass(String clickClass) {
        this.clickClass = clickClass;
    }

    public double getScaleFactor() {
        return scaleFactor;
    }

    public void setScaleFactor(double scaleFactor) {
        this.scaleFactor = scaleFactor;
    }

    /** 是否已配置窗口匹配条件（都为空则只能手动点选） */
    public boolean hasWindowMatch() {
        return !isBlank(windowTitle) || !isBlank(windowClass);
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
