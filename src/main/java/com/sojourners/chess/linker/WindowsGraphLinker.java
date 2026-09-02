package com.sojourners.chess.linker;

import com.sojourners.chess.config.Properties;
import com.sojourners.chess.jna.DwmapiExtra;
import com.sojourners.chess.jna.User32Extra;
import com.sojourners.chess.mouse.GlobalMouseListener;
import com.sojourners.chess.mouse.MouseListenCallBack;
import com.sojourners.chess.util.PathUtils;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.*;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Windows 连线器（借鉴 VinXiangQi 的方案/窗口句柄适配）：
 * 1. 优先按当前方案（截图标题/类名）自动查找游戏窗口，找不到再退回手动点选；
 * 2. 支持指定子窗口作为点击目标，并自动计算两个窗口客户区原点偏移；
 * 3. 窗口矩形优先使用 DWM 可见边框（不含 Win10/11 隐形缩放边框），更准确；
 * 4. 后台点击支持方案级缩放比微调。
 */
public class WindowsGraphLinker extends AbstractGraphLinker implements MouseListenCallBack {

    private WinDef.HWND hwnd;
    /** 点击目标句柄（方案指定子窗口时为子窗口，否则等于 hwnd） */
    private WinDef.HWND clickHwnd;
    /** 截图窗口客户区原点相对点击窗口客户区原点的偏移 */
    private Point clickOffset = new Point(0, 0);
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
            // 优先按方案自动查找窗口，无需手动点选
            LinkScheme scheme = getSelectedScheme();
            if (scheme != null && scheme.hasWindowMatch() && findWindowByScheme(scheme)) {
                scan();
                return;
            }
            // 兜底：手动点选目标窗口
            this.listener.startListenMouse();
            selectCursor();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private LinkScheme getSelectedScheme() {
        Properties prop = Properties.getInstance();
        List<LinkScheme> list = prop.getLinkSchemeList();
        String name = prop.getSelectedLinkScheme();
        if (list == null || name == null) {
            return null;
        }
        for (LinkScheme s : list) {
            if (name.equals(s.getName())) {
                return s;
            }
        }
        return null;
    }

    private double getSchemeScaleFactor() {
        LinkScheme s = getSelectedScheme();
        if (s != null && s.getScaleFactor() > 0) {
            return s.getScaleFactor();
        }
        return 1.0d;
    }

    /**
     * 按方案查找截图窗口和点击窗口
     */
    private boolean findWindowByScheme(LinkScheme scheme) {
        List<WinDef.HWND> matches = new ArrayList<>();
        User32.INSTANCE.EnumWindows(new WinUser.WNDENUMPROC() {
            @Override
            public boolean callback(WinDef.HWND hWnd, Pointer arg) {
                if (User32.INSTANCE.IsWindowVisible(hWnd)
                        && matchWindow(hWnd, scheme.getWindowClass(), scheme.getWindowTitle())) {
                    matches.add(hWnd);
                }
                return true;
            }
        }, null);
        if (matches.isEmpty()) {
            return false;
        }

        // 标题完全匹配优先，其次取第一个可见窗口
        WinDef.HWND target = matches.get(0);
        if (!isBlank(scheme.getWindowTitle())) {
            for (WinDef.HWND h : matches) {
                if (scheme.getWindowTitle().equalsIgnoreCase(getWindowTitle(h))) {
                    target = h;
                    break;
                }
            }
        }
        this.hwnd = target;
        this.needScaling = needScaling(this.hwnd);

        // 查找点击子窗口（如 DirectX/渲染子窗口）
        if (isBlank(scheme.getClickClass()) && isBlank(scheme.getClickTitle())) {
            this.clickHwnd = this.hwnd;
        } else {
            List<WinDef.HWND> children = new ArrayList<>();
            User32.INSTANCE.EnumChildWindows(this.hwnd, new WinUser.WNDENUMPROC() {
                @Override
                public boolean callback(WinDef.HWND hWnd, Pointer arg) {
                    if (matchWindow(hWnd, scheme.getClickClass(), scheme.getClickTitle())) {
                        children.add(hWnd);
                    }
                    return true;
                }
            }, null);
            this.clickHwnd = children.isEmpty() ? this.hwnd : children.get(0);
        }
        computeClickOffset();
        return true;
    }

    /**
     * 窗口匹配：类名精确匹配（可空=不限），标题包含匹配（可空=不限）
     */
    private boolean matchWindow(WinDef.HWND h, String wantClass, String wantTitle) {
        if (isBlank(wantClass) && isBlank(wantTitle)) {
            return false;
        }
        if (!isBlank(wantClass)) {
            String cls = getWindowClass(h);
            if (cls == null || !wantClass.equalsIgnoreCase(cls)) {
                return false;
            }
        }
        if (!isBlank(wantTitle)) {
            String title = getWindowTitle(h);
            if (title == null || !title.toLowerCase().contains(wantTitle.toLowerCase())) {
                return false;
            }
        }
        return true;
    }

    /**
     * 计算截图窗口与点击窗口客户区原点偏移。
     * 点击坐标 = 截图窗口内坐标 + 偏移。
     */
    private void computeClickOffset() {
        if (this.clickHwnd == null) {
            this.clickHwnd = this.hwnd;
        }
        if (this.hwnd == null || this.clickHwnd.equals(this.hwnd)) {
            this.clickOffset = new Point(0, 0);
            return;
        }
        try {
            WinDef.POINT pt = new WinDef.POINT(0, 0);
            User32Extra.INSTANCE.ClientToScreen(this.hwnd, pt);
            int gx = pt.x, gy = pt.y;
            pt = new WinDef.POINT(0, 0);
            User32Extra.INSTANCE.ClientToScreen(this.clickHwnd, pt);
            this.clickOffset = new Point(gx - pt.x, gy - pt.y);
        } catch (Exception e) {
            e.printStackTrace();
            this.clickOffset = new Point(0, 0);
        }
    }

    /**
     * 窗口句柄失效时（游戏重启/窗口重建）按方案重新查找
     */
    private void ensureWindow() {
        if (this.hwnd != null && User32.INSTANCE.IsWindow(this.hwnd)) {
            return;
        }
        LinkScheme scheme = getSelectedScheme();
        if (scheme != null && scheme.hasWindowMatch()) {
            findWindowByScheme(scheme);
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
            this.clickHwnd = this.hwnd;
            this.clickOffset = new Point(0, 0);
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
        ensureWindow();
        Rectangle rectangle = getWindowRectWithDwm(this.hwnd);
        if (rectangle == null) {
            WinDef.RECT rect = new WinDef.RECT();
            User32.INSTANCE.GetWindowRect(hwnd, rect);
            rectangle = rect.toRectangle();
        }
        // windows缩放处理（Robot截图/前台点击使用逻辑坐标）
        rectangle.x /= screenScalingFactor;
        rectangle.y /= screenScalingFactor;
        rectangle.width /= screenScalingFactor;
        rectangle.height /= screenScalingFactor;
        return rectangle;
    }

    /**
     * 优先获取 DWM 可见边框矩形（不含隐形缩放边框），失败时返回 null
     */
    private Rectangle getWindowRectWithDwm(WinDef.HWND h) {
        try {
            if (h == null) {
                return null;
            }
            WinDef.RECT rect = new WinDef.RECT();
            int hr = DwmapiExtra.INSTANCE.DwmGetWindowAttribute(h, DwmapiExtra.DWMWA_EXTENDED_FRAME_BOUNDS, rect, rect.size());
            if (hr == 0 && rect.right > rect.left && rect.bottom > rect.top) {
                return rect.toRectangle();
            }
        } catch (Throwable t) {
            // dwmapi 不可用时回退 GetWindowRect
        }
        return null;
    }

    private double getScreenScalingFactor() {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice gd = ge.getDefaultScreenDevice();
        return gd.getDefaultConfiguration().getDefaultTransform().getScaleX();
    }

    @Override
    public BufferedImage screenshotByBack(Rectangle windowPos) {
        ensureWindow();
        return capture(this.hwnd, windowPos);
    }

    @Override
    public void mouseClickByBack(Point p1, Point p2) {
        ensureWindow();
        // 处理windows缩放问题
        if (needScaling) {
            p1.x *= screenScalingFactor;
            p1.y *= screenScalingFactor;
            p2.x *= screenScalingFactor;
            p2.y *= screenScalingFactor;
        }
        // 方案级缩放微调（DPI/窗口缩放）
        double sf = getSchemeScaleFactor();
        if (sf != 1.0d) {
            p1.x = (int) Math.round(p1.x * sf);
            p1.y = (int) Math.round(p1.y * sf);
            p2.x = (int) Math.round(p2.x * sf);
            p2.y = (int) Math.round(p2.y * sf);
        }

        leftClick(p1.x + clickOffset.x, p1.y + clickOffset.y);
        if (Properties.getInstance().getMouseMoveDelay() > 0) {
            sleep(Properties.getInstance().getMouseMoveDelay());
        }
        leftClick(p2.x + clickOffset.x, p2.y + clickOffset.y);
    }

    private void leftClick(int x, int y) {
        WinDef.HWND target = this.clickHwnd != null ? this.clickHwnd : this.hwnd;
        User32.INSTANCE.PostMessage(target, 0x0200, new WinDef.WPARAM(1), new WinDef.LPARAM(makeLParam(x, y)));
        User32.INSTANCE.PostMessage(target, 0x0201, new WinDef.WPARAM(1), new WinDef.LPARAM(makeLParam(x, y)));
        if (Properties.getInstance().getMouseClickDelay() > 0) {
            sleep(Properties.getInstance().getMouseClickDelay());
        }
        User32.INSTANCE.PostMessage(target, 0x0202, new WinDef.WPARAM(0), new WinDef.LPARAM(makeLParam(x, y)));
    }

    private int makeLParam(int loWord, int hiWord) {
        return (hiWord << 16) | (loWord & 0xFFFF);
    }

    private BufferedImage capture(WinDef.HWND hWnd, Rectangle rect) {
        if (hWnd == null) {
            return null;
        }
        // 创建与窗口相关联的设备上下文和一个内存设备上下文以执行离屏渲染
        WinDef.HDC hdcWindow = User32.INSTANCE.GetDC(hWnd);
        WinDef.HDC hdcMemDC = GDI32.INSTANCE.CreateCompatibleDC(hdcWindow);
        try {
            int width, height;
            WinDef.RECT bounds = new WinDef.RECT();
            User32.INSTANCE.GetClientRect(hWnd, bounds);
            width = bounds.right - bounds.left;
            height = bounds.bottom - bounds.top;
            if (width <= 0 || height <= 0) {
                return null;
            }
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
                if (x < 0) x = 0;
                if (y < 0) y = 0;
                if (x + width > image.getWidth()) {
                    width = image.getWidth() - x;
                }
                if (y + height > image.getHeight()) {
                    height = image.getHeight() - y;
                }
                if (width <= 0 || height <= 0) {
                    return null;
                }
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

    /**
     * 获取窗口标题（供方案拾取）
     */
    public static String getWindowTitle(WinDef.HWND h) {
        if (h == null) {
            return "";
        }
        try {
            int len = User32.INSTANCE.GetWindowTextLength(h);
            if (len <= 0) {
                return "";
            }
            char[] buffer = new char[len + 1];
            int n = User32.INSTANCE.GetWindowText(h, buffer, buffer.length);
            return n > 0 ? new String(buffer, 0, n) : "";
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * 获取窗口类名（供方案拾取）
     */
    public static String getWindowClass(WinDef.HWND h) {
        if (h == null) {
            return "";
        }
        try {
            char[] buffer = new char[256];
            int n = User32.INSTANCE.GetClassName(h, buffer, buffer.length);
            return n > 0 ? new String(buffer, 0, n) : "";
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * 获取当前前台窗口（供方案拾取）
     */
    public static WinDef.HWND getForegroundWindow() {
        return User32.INSTANCE.GetForegroundWindow();
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private void selectCursor() {
        WinDef.HCURSOR h = User32Extra.INSTANCE.LoadCursorFromFileA(PathUtils.getJarPath() + "ui/circle.ico");
        User32Extra.INSTANCE.SetSystemCursor(h, new WinDef.DWORD(32512));
    }

    private void restoreCursor() {
        User32Extra.INSTANCE.SystemParametersInfoA(87, 0, 0, 2);
    }
}
