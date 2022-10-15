package com.sojourners.chess.linker;

import java.awt.*;
import java.awt.image.BufferedImage;

public class MacosGraphLinker extends AbstractGraphLinker {

    public MacosGraphLinker(LinkerCallBack callBack) throws AWTException {
        super(callBack);
    }

    @Override
    public void getTargetWindowId() {

    }

    @Override
    public Rectangle getTargetWindowPosition() {
        return null;
    }

    @Override
    public BufferedImage screenshotByBack(Rectangle windowPos) {
        return null;
    }

    @Override
    public void mouseClickByByBack(Point p1, Point p2) {

    }
}
