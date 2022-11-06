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

    public WindowsGraphLinker(LinkerCallBack callBack) throws AWTException {
        super(callBack);
        this.listener = new GlobalMouseListener(this);
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

            this.hwnd = User32.INSTANCE.GetForegroundWindow();

            scan();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public Rectangle getTargetWindowPosition() {
        WinDef.RECT rect = new WinDef.RECT();
        User32.INSTANCE.GetWindowRect(hwnd, rect);
        return rect.toRectangle();
    }

    @Override
    public BufferedImage screenshotByBack(Rectangle windowPos) {
        return capture(this.hwnd, windowPos);
    }

    @Override
    public void mouseClickByBack(Point p1, Point p2) {
        leftClick(p1.x, p1.y);
        if (Properties.getInstance().getMouseMoveDelay() > 0) {
            sleep(Properties.getInstance().getMouseMoveDelay());
        }
        leftClick(p2.x, p2.y);
    }

    private void leftClick(int x, int y) {
        User32.INSTANCE.PostMessage(hwnd, 0x0200, new WinDef.WPARAM(1), new WinDef.LPARAM(makeLParam(x, y)));
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
        try {
            int x, y, width, height;
            if (rect == null) {
                WinDef.RECT bounds = new WinDef.RECT();
                User32.INSTANCE.GetClientRect(hWnd, bounds);
                width = bounds.right - bounds.left;
                height = bounds.bottom - bounds.top;
                x = 0;
                y = 0;
            } else {
                width = (int) rect.getWidth();
                height = (int) rect.getHeight();
                x = rect.x;
                y = rect.y;
            }
            if (width == 0 || height == 0) {
                return null;
            }

            WinDef.HDC hdcWindow = User32.INSTANCE.GetDC(hWnd);
            WinDef.HDC hdcMemDC = GDI32.INSTANCE.CreateCompatibleDC(hdcWindow);

            WinDef.HBITMAP hBitmap = GDI32.INSTANCE.CreateCompatibleBitmap(hdcWindow, width, height);

            WinNT.HANDLE hOld = GDI32.INSTANCE.SelectObject(hdcMemDC, hBitmap);
            GDI32.INSTANCE.BitBlt(hdcMemDC, 0, 0, width, height, hdcWindow, x, y, 0x00CC0020);

            GDI32.INSTANCE.SelectObject(hdcMemDC, hOld);
            GDI32.INSTANCE.DeleteDC(hdcMemDC);

            WinGDI.BITMAPINFO bmi = new WinGDI.BITMAPINFO();
            bmi.bmiHeader.biWidth = width;
            bmi.bmiHeader.biHeight = -height;
            bmi.bmiHeader.biPlanes = 1;
            bmi.bmiHeader.biBitCount = 32;
            bmi.bmiHeader.biCompression = WinGDI.BI_RGB;

            Memory buffer = new Memory(width * height * 4);
            GDI32.INSTANCE.GetDIBits(hdcWindow, hBitmap, 0, height, buffer, bmi, WinGDI.DIB_RGB_COLORS);

            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            image.setRGB(0, 0, width, height, buffer.getIntArray(0, width * height), 0, width);

            GDI32.INSTANCE.DeleteObject(hBitmap);
            User32.INSTANCE.ReleaseDC(hWnd, hdcWindow);

            return image;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
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
