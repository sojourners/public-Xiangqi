package com.sojourners.chess.linker;

import com.sojourners.chess.config.Properties;
import com.sojourners.chess.jna.MacosWindowControl;
import com.sojourners.chess.util.ShellUtils;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * macOS 连线器，选窗口时通过 osascript 获取进程，随后使用 CoreGraphics 查询窗口位置。
 * 使用方法：点击连线按钮，3秒内再点击目标平台，然后等待连线识别成功即可
 */
public class MacosGraphLinker extends AbstractGraphLinker {

    private volatile MacosWindowControl.Window targetWindow;

    public MacosGraphLinker(LinkerCallBack callBack) throws AWTException {
        super(callBack);
    }

    @Override
    public void getTargetWindowId() {
        stop();
        sleep(3000);
        if (Thread.currentThread().isInterrupted()) {
            return;
        }
        String script = "tell application \"System Events\" to return unix id of "
                + "(first application process whose frontmost is true)";
        String result = ShellUtils.exec(new String[]{"osascript", "-e", script });
        if (result == null || result.isBlank()) {
            System.err.println("无法获取前台应用进程，请检查 osascript 权限后重新连线。");
            return;
        }
        int pid;
        try {
            pid = Integer.parseInt(result.trim());
        } catch (NumberFormatException e) {
            System.err.println("无法解析前台应用进程，请重新选择目标窗口。");
            return;
        }
        MacosWindowControl.Window selected = MacosWindowControl.findFrontmostWindow(pid);
        if (selected == null) {
            System.err.println("未找到目标应用的可见窗口，请重新选择目标窗口。");
            return;
        }
        if (Thread.currentThread().isInterrupted()) {
            return;
        }
        this.targetWindow = selected;

        scan();
    }

    @Override
    public Rectangle getTargetWindowPosition() {
        return MacosWindowControl.getBounds(this.targetWindow);
    }

    @Override
    public void stop() {
        super.stop();
        this.targetWindow = null;
    }

    /** windowPos 为窗口内的逻辑坐标；null 表示整个窗口。 */
    @Override
    public BufferedImage screenshotByBack(Rectangle windowPos) {
        return MacosWindowControl.capture(this.targetWindow, windowPos);
    }

    /** p1、p2 与后台截图一致，均为窗口内的逻辑坐标。 */
    @Override
    public void mouseClickByBack(Point p1, Point p2) {
        Properties prop = Properties.getInstance();
        MacosWindowControl.Window selected = this.targetWindow;
        MacosWindowControl.click(selected, p1, p2,
                prop.getMouseClickDelay(), prop.getMouseMoveDelay(), () -> this.targetWindow == selected);
    }
}
