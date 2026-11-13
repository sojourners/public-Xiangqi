package com.sojourners.chess.linker;

import com.sojourners.chess.jna.X11WindowControl;
import com.sojourners.chess.util.ShellUtils;
import com.sojourners.chess.util.StringUtils;
import com.sojourners.chess.config.Properties;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Linux 连线器，使用 xdotool 选择窗口、X11 API 截图和点击。
 * 使用方法：点击连线按钮，再点击选择目标平台，然后等待连线识别成功即可
 */
public class LinuxGraphLinker extends AbstractGraphLinker {

    private String windowId;
    private X11WindowControl.BackgroundCapture backgroundCapture;

    static {
        // Keep one process-wide dispatcher for errors on our own X11 connections.
        X11WindowControl.initialize();
    }

    public LinuxGraphLinker(LinkerCallBack callBack) throws AWTException {
        super(callBack);
    }

    @Override
    public void getTargetWindowId() {
        stop();
        this.windowId = ShellUtils.exec("xdotool selectwindow");
        if (StringUtils.isEmpty(this.windowId) || this.windowId.isBlank()) {
            return;
        }
        this.windowId = this.windowId.trim();
        if (Properties.getInstance().isLinkBackMode()) {
            backgroundCapture = X11WindowControl.prepareBackgroundCapture(this.windowId);
        } else {
            ShellUtils.exec("xdotool windowactivate --sync " + this.windowId);
        }

        scan();
    }

    @Override
    public void stop() {
        super.stop();
        if (backgroundCapture != null) {
            backgroundCapture.close();
            backgroundCapture = null;
        }
    }

    @Override
    public Rectangle getTargetWindowPosition() {
        Rectangle rec = new Rectangle();
        String result = ShellUtils.exec("xdotool getwindowgeometry " + this.windowId);
        if (StringUtils.isEmpty(result)) {
            return rec;
        }
        String[] ss = result.split(System.getProperty("line.separator"));
        for (String s : ss) {
            if (s.contains("Position")) {
                String pos = s.split(" ")[3];
                String[] nums = pos.split(",");
                rec.setLocation(Integer.parseInt(nums[0]), Integer.parseInt(nums[1]));
            } else if (s.contains("Geometry")) {
                String size = s.split(" ")[3];
                String[] nums = size.split("x");
                rec.setSize(Integer.parseInt(nums[0]), Integer.parseInt(nums[1]));
            }
        }
        return rec;
    }

    @Override
    public BufferedImage screenshotByFront(Rectangle windowPos) {
        // JDK-8335468: AWT's ScreenCast/GTK loop can hang when JavaFX is running.
        // XWayland's root image can also be black; read the selected X11 window itself.
        // Keep screenshots and clicks in the same X11 pixel coordinate system on both desktops.
        return X11WindowControl.capture(windowId, windowPos);
    }

    @Override
    public void mouseClickByFront(Rectangle windowPos, Point p1, Point p2) {
        Properties prop = Properties.getInstance();
        // p1/p2 are window-local X11 pixels, matching xdotool and the captured board.
        // Avoid AWT's DPI conversion and its mixed XWarpPointer/XTEST path (JDK-8351907).
        X11WindowControl.clickByFront(windowId, p1, p2, prop.getMouseClickDelay(), prop.getMouseMoveDelay());
    }

    @Override
    public BufferedImage screenshotByBack(Rectangle windowPos) {
        // The base class supplies window-local board bounds, or null for the full window.
        return X11WindowControl.captureByBack(windowId, windowPos);
    }

    @Override
    public void mouseClickByBack(Point p1, Point p2) {
        Properties prop = Properties.getInstance();
        X11WindowControl.clickByBack(windowId, p1, p2, prop.getMouseClickDelay(), prop.getMouseMoveDelay());
    }
}
