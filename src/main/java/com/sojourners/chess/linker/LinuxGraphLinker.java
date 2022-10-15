package com.sojourners.chess.linker;

import com.sojourners.chess.util.ShellUtils;

import java.awt.*;
import java.awt.image.BufferedImage;

public class LinuxGraphLinker extends AbstractGraphLinker {

    private String windowId;

    public LinuxGraphLinker(LinkerCallBack callBack) throws AWTException {
        super(callBack);
    }

    @Override
    public void getTargetWindowId() {
        this.windowId = ShellUtils.exec("xdotool selectwindow");
        ShellUtils.exec("xdotool windowactivate --sync " + this.windowId);
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

//    @Override
//    public void mouseClickByFront(Rectangle windowPos, Point p1, Point p2) {
//
//        Point mouse = MouseInfo.getPointerInfo().getLocation();
//        String cmd = "xdotool mousemove " + (windowPos. x + p1.x) + " " + (windowPos. y + p1.y) + " click 1 "
//                + "mousemove " + (windowPos. x + p2.x) + " " + (windowPos. y + p2.y) + " click 1 "
//                + "mousemove " + mouse.x + " " + mouse.y;
//        ShellUtils.exec(cmd);
//    }

    @Override
    public BufferedImage screenshotByBack(Rectangle windowPos) {
        return null;
    }


    @Override
    public void mouseClickByByBack(Point p1, Point p2) {

    }
}
