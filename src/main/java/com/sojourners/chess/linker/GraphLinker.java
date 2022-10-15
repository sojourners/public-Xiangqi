package com.sojourners.chess.linker;

import java.awt.*;
import java.awt.image.BufferedImage;

public interface GraphLinker {

    void start();

    void stop();

    void getTargetWindowId();

    Rectangle getTargetWindowPosition();

    BufferedImage screenshotByBack(Rectangle windowPos);

    BufferedImage screenshotByFront(Rectangle windowPos);

    void mouseClickByFront(Rectangle windowPos, Point p1, Point p2);

    void mouseClickByByBack(Point p1, Point p2);

}
