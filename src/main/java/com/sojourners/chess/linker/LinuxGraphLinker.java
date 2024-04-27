package com.sojourners.chess.linker;

import com.sojourners.chess.util.ShellUtils;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * linux 连线器，基于xdotool 实现
 * 使用方法：点击连线按钮，再点击选择目标平台，然后等待连线识别成功即可
 */
public class LinuxGraphLinker extends AbstractGraphLinker {

    private String windowId;

    public LinuxGraphLinker(LinkerCallBack callBack) throws AWTException {
        super(callBack);
    }

    @Override
    public void getTargetWindowId() {
        this.windowId = ShellUtils.exec("xdotool selectwindow");
        ShellUtils.exec("xdotool windowactivate --sync " + this.windowId);

        scan();
    }

    @Override
    public Rectangle getTargetWindowPosition() {
        Rectangle rec = new Rectangle();
        String result = ShellUtils.exec("xdotool getwindowgeometry " + this.windowId);
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

    /**
     * 后台模式暂未实现
     * @param windowPos
     * @return
     */
    @Override
    public BufferedImage screenshotByBack(Rectangle windowPos) {
        return null;
    }

    /**
     * 后台模式暂未实现
     * @param p1
     * @param p2
     */
    @Override
    public void mouseClickByBack(Point p1, Point p2) {

    }
}
