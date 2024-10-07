package com.sojourners.chess.linker;

import com.sojourners.chess.config.Properties;
import com.sojourners.chess.jna.User32Extra;
import com.sojourners.chess.mouse.GlobalMouseListener;
import com.sojourners.chess.mouse.MouseListenCallBack;
import com.sojourners.chess.util.PathUtils;
import com.sun.jna.Memory;
import com.sun.jna.platform.win32.*;

import java.awt.*;
import java.awt.image.BufferedImage;

public class WindowsGraphLinker extends AbstractGraphLinker implements MouseListenCallBack {

    private WinDef.HWND hwnd;
    private GlobalMouseListener listener;
    private double screenScalingFactor;
    private boolean needScaling;

    public WindowsGraphLinker(LinkerCallBack callBack) throws AWTException {
        super(callBack);
        this.listener = new GlobalMouseListener(this);
        // 分辨率缩放系数
        this.screenScalingFactor = getScreenScalingFactor();
    }

    @Override
    public void getTargetWindowId() {
        try {
            this.listener.startListenMouse();
            selectCursor();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @Override
    public void mouseClick() {
        try {
            this.listener.stopListenMouse();
            restoreCursor();

            long[] getPos = new long[1];
            User32Extra.INSTANCE.GetCursorPos(getPos);
            this.hwnd = User32Extra.INSTANCE.WindowFromPoint(getPos[0]);

            this.needScaling = needScaling(this.hwnd);

            scan();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private boolean needScaling(WinDef.HWND hwnd) {
        // 获取系统DPI
        int systemDpi = User32Extra.INSTANCE.GetDpiForSystem();
        // 通过窗口句柄获取当前窗口的DPI
        int windowDpi = User32Extra.INSTANCE.GetDpiForWindow(hwnd);
        // 比较系统DPI和窗口DPI是否相同，如果不同则需要缩放处理
        return systemDpi != windowDpi;
    }

    @Override
    public Rectangle getTargetWindowPosition() {
        WinDef.RECT rect = new WinDef.RECT();
        User32.INSTANCE.GetWindowRect(hwnd, rect);
        Rectangle rectangle = rect.toRectangle();
        // windows缩放处理
        rectangle.x /= screenScalingFactor;
        rectangle.y /= screenScalingFactor;
        rectangle.width /= screenScalingFactor;
        rectangle.height /= screenScalingFactor;
        return rectangle;
    }

    private double getScreenScalingFactor() {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice gd = ge.getDefaultScreenDevice();
        return gd.getDefaultConfiguration().getDefaultTransform().getScaleX();
    }

    @Override
    public BufferedImage screenshotByBack(Rectangle windowPos) {
        return capture(this.hwnd, windowPos);
    }

    @Override
    public void mouseClickByBack(Point p1, Point p2) {
        // 处理windows缩放问题
        if (needScaling) {
            p1.x *= screenScalingFactor;
            p1.y *= screenScalingFactor;
            p2.x *= screenScalingFactor;
            p2.y *= screenScalingFactor;
        }

        leftClick(p1.x, p1.y);
        if (Properties.getInstance().getMouseMoveDelay() > 0) {
            sleep(Properties.getInstance().getMouseMoveDelay());
        }
        leftClick(p2.x, p2.y);
    }

    private void leftClick(int x, int y) {
//        User32.INSTANCE.PostMessage(hwnd, 0x0200, new WinDef.WPARAM(1), new WinDef.LPARAM(makeLParam(x, y)));
        User32.INSTANCE.PostMessage(hwnd, 0x0201, new WinDef.WPARAM(1), new WinDef.LPARAM(makeLParam(x, y)));
        if (Properties.getInstance().getMouseClickDelay() > 0) {
            sleep(Properties.getInstance().getMouseClickDelay());
        }
        User32.INSTANCE.PostMessage(hwnd, 0x0202, new WinDef.WPARAM(0), new WinDef.LPARAM(makeLParam(x, y)));
    }
    private int makeLParam(int loWord, int hiWord) {
        return (hiWord << 16) | (loWord & 0xFFFF);
    }

    private BufferedImage capture(WinDef.HWND hWnd, Rectangle rect) {
        // 创建与窗口相关联的设备上下文和一个内存设备上下文以执行离屏渲染
        WinDef.HDC hdcWindow = User32.INSTANCE.GetDC(hWnd);
        WinDef.HDC hdcMemDC = GDI32.INSTANCE.CreateCompatibleDC(hdcWindow);
        try {
            int width, height;
            WinDef.RECT bounds = new WinDef.RECT();
            User32.INSTANCE.GetClientRect(hWnd, bounds);
            width = bounds.right - bounds.left;
            height = bounds.bottom - bounds.top;
            // 处理windows缩放问题
            if (needScaling) {
                width /= screenScalingFactor;
                height /= screenScalingFactor;
            }
            // 创建兼容的位图，并且将其选入内存设备上下文
            WinDef.HBITMAP hBitmap = GDI32.INSTANCE.CreateCompatibleBitmap(hdcWindow, width, height);
            WinNT.HANDLE hOld = GDI32.INSTANCE.SelectObject(hdcMemDC, hBitmap);
            // 请求窗口自行完成绘制工作
            if (!User32.INSTANCE.PrintWindow(hWnd, hdcMemDC, 0x1 | 0x2)) {
                return null;
            }

            // 将所绘制的位图转化为Java缓冲图片（BufferedImage）
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            WinGDI.BITMAPINFO bmi = new WinGDI.BITMAPINFO();
            bmi.bmiHeader.biWidth = width;
            bmi.bmiHeader.biHeight = -height; // 注意：biHeight为负表示顶向下DIB
            bmi.bmiHeader.biPlanes = 1;
            bmi.bmiHeader.biBitCount = 32;
            bmi.bmiHeader.biCompression = WinGDI.BI_RGB;

            Memory buffer = new Memory(width * height * 4);
            GDI32.INSTANCE.GetDIBits(hdcMemDC, hBitmap, 0, height, buffer, bmi, WinGDI.DIB_RGB_COLORS);

            int[] data = buffer.getIntArray(0, width * height);
            image.setRGB(0, 0, width, height, data, 0, width);

            // 清理资源
            GDI32.INSTANCE.SelectObject(hdcMemDC, hOld);
            GDI32.INSTANCE.DeleteObject(hBitmap);

            if (rect != null) {
                width = (int) rect.getWidth();
                height = (int) rect.getHeight();
                int x = rect.x;
                int y = rect.y;
                image = image.getSubimage(x, y, width, height);
            }

            return image;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            // 清理设备上下文对象
            GDI32.INSTANCE.DeleteDC(hdcMemDC);
            User32.INSTANCE.ReleaseDC(hWnd, hdcWindow);
        }
    }

    private void selectCursor() {
        WinDef.HCURSOR h = User32Extra.INSTANCE.LoadCursorFromFileA(PathUtils.getJarPath() + "ui/circle.ico");
        User32Extra.INSTANCE.SetSystemCursor(h, new WinDef.DWORD(32512));
    }

    private void restoreCursor() {
        User32Extra.INSTANCE.SystemParametersInfoA(87, 0, 0, 2);
    }
}
