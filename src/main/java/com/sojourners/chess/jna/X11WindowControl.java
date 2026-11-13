package com.sojourners.chess.jna;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.platform.unix.X11;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/** X11/XWayland window capture and foreground/background mouse input. */
public final class X11WindowControl {

    private static final Set<Pointer> OWNED_DISPLAYS = ConcurrentHashMap.newKeySet();
    private static X11.XErrorHandler previousHandler;
    private static boolean handlerInstalled;
    // Keep the callback alive for the process lifetime. GDK can temporarily wrap this handler.
    private static final X11.XErrorHandler ERROR_HANDLER = (connection, error) -> {
        if (OWNED_DISPLAYS.contains(connection.getPointer())) {
            return 0;
        }
        return previousHandler == null ? 0 : previousHandler.apply(connection, error);
    };

    private interface Xlib extends X11 {
        int XScreenNumberOfScreen(Screen screen);
        int XGetInputFocus(Display display, WindowByReference focus, IntByReference revertTo);
        Xlib INSTANCE = Native.load("X11", Xlib.class);

        XImage XGetImage(Display display, Drawable drawable, int x, int y,
                         int width, int height, NativeLong planes, int format);
    }

    private interface Xcomposite extends Library {
        Xcomposite INSTANCE = Native.load("Xcomposite", Xcomposite.class);

        boolean XCompositeQueryExtension(X11.Display display, IntByReference event, IntByReference error);
        void XCompositeRedirectWindow(X11.Display display, X11.Window window, int update);
        X11.Pixmap XCompositeNameWindowPixmap(X11.Display display, X11.Window window);
    }

    // The prefix of Xlib's XImage structure, through its RGB masks.
    @Structure.FieldOrder({"width", "height", "xoffset", "format", "data", "byteOrder",
            "bitmapUnit", "bitmapBitOrder", "bitmapPad", "depth", "bytesPerLine",
            "bitsPerPixel", "redMask", "greenMask", "blueMask"})
    public static class ImageData extends Structure {
        public int width, height, xoffset, format;
        public Pointer data;
        public int byteOrder, bitmapUnit, bitmapBitOrder, bitmapPad, depth, bytesPerLine, bitsPerPixel;
        public NativeLong redMask, greenMask, blueMask;

        public ImageData(Pointer pointer) {
            super(pointer);
            read();
        }
    }

    private X11WindowControl() {
    }

    public static synchronized void initialize() {
        if (!handlerInstalled) {
            previousHandler = Xlib.INSTANCE.XSetErrorHandler(ERROR_HANDLER);
            handlerInstalled = true;
        }
    }

    /** Keeps off-screen storage alive throughout a link session, including on desktops without a compositor. */
    public static synchronized BackgroundCapture prepareBackgroundCapture(String windowId) {
        X11.Window window = new X11.Window(Long.parseLong(windowId.trim()));
        X11.Display display = openDisplay();
        try {
            if (Xlib.INSTANCE.XGetWindowAttributes(display, window, new X11.XWindowAttributes()) == 0) {
                throw new IllegalStateException("连线目标窗口已关闭，请重新选择窗口。");
            }
            if (!Xcomposite.INSTANCE.XCompositeQueryExtension(display,
                    new IntByReference(), new IntByReference())) {
                throw new IllegalStateException("X11 显示服务不支持 Composite，无法获取后台窗口图像。");
            }
            // Automatic redirection keeps normal display updates, and coexists with the compositor.
            // Keep this connection open: closing it removes only our redirection request.
            Xcomposite.INSTANCE.XCompositeRedirectWindow(display, window, 0);
            Xlib.INSTANCE.XSync(display, false);
            return new BackgroundCapture(display);
        } catch (RuntimeException | Error e) {
            closeDisplay(display);
            throw e;
        }
    }

    public static final class BackgroundCapture implements AutoCloseable {
        private X11.Display display;

        private BackgroundCapture(X11.Display display) {
            this.display = display;
        }

        @Override
        public void close() {
            synchronized (X11WindowControl.class) {
                if (display != null) {
                    closeDisplay(display);
                    display = null;
                }
            }
        }
    }

    public static BufferedImage capture(String windowId, Rectangle screenBounds) {
        if (windowId == null || screenBounds == null || screenBounds.isEmpty()) {
            return null;
        }
        X11.Window window = new X11.Window(Long.parseLong(windowId.trim()));
        return withDisplay(display -> capture(display, window, screenBounds));
    }

    /** Reads the compositor's backing pixmap, including pixels obscured by other windows.
     * Bounds are window-local; null requests the whole window. Unmapped windows have no live image.
     */
    public static BufferedImage captureByBack(String windowId, Rectangle bounds) {
        if (windowId == null || windowId.isBlank() || bounds != null && bounds.isEmpty()) {
            return null;
        }
        X11.Window window = new X11.Window(Long.parseLong(windowId.trim()));
        Rectangle requested = bounds == null ? null : new Rectangle(bounds);
        return withDisplay(display -> {
            Xlib xlib = Xlib.INSTANCE;
            X11.XWindowAttributes attributes = new X11.XWindowAttributes();
            if (xlib.XGetWindowAttributes(display, window, attributes) == 0
                    || attributes.map_state != X11.IsViewable) {
                return null;
            }
            Rectangle local = requested == null ? new Rectangle(attributes.width, attributes.height) : requested;
            if (!new Rectangle(attributes.width, attributes.height).contains(local)) {
                return null;
            }
            if (!Xcomposite.INSTANCE.XCompositeQueryExtension(display,
                    new IntByReference(), new IntByReference())) {
                throw new IllegalStateException("X11 显示服务不支持 Composite，无法获取后台窗口图像。");
            }
            X11.Pixmap pixmap = Xcomposite.INSTANCE.XCompositeNameWindowPixmap(display, window);
            // NameWindowPixmap allocates an ID even on BadMatch (unmapped or not redirected).
            if (pixmap == null || xlib.XGetGeometry(display, pixmap, new X11.WindowByReference(),
                    new IntByReference(), new IntByReference(), new IntByReference(), new IntByReference(),
                    new IntByReference(), new IntByReference()) == 0) {
                return null;
            }
            X11.XImage image = null;
            X11.XVisualInfo visual = null;
            try {
                image = xlib.XGetImage(display, pixmap, local.x + attributes.border_width,
                        local.y + attributes.border_width, local.width, local.height,
                        new NativeLong(-1), X11.ZPixmap);
                if (image == null) {
                    return null;
                }
                // Pixmaps have no Visual, so XGetImage returns zero RGB masks for them.
                X11.XVisualInfo template = new X11.XVisualInfo();
                template.visualid = attributes.visual.getVisualID();
                visual = xlib.XGetVisualInfo(display, new NativeLong(X11.VisualIDMask),
                        template, new IntByReference());
                if (visual == null) {
                    return null;
                }
                ImageData data = new ImageData(image.getPointer());
                data.redMask = visual.red_mask;
                data.greenMask = visual.green_mask;
                data.blueMask = visual.blue_mask;
                return toBufferedImage(data);
            } finally {
                if (visual != null) {
                    xlib.XFree(visual.getPointer());
                }
                if (image != null) {
                    xlib.XDestroyImage(image);
                }
                xlib.XFreePixmap(display, pixmap);
            }
        });
    }

    // Serialize our Xlib connections and initialization of the process-wide error dispatcher.
    static synchronized <T> T withDisplay(Function<X11.Display, T> action) {
        X11.Display display = openDisplay();
        try {
            return action.apply(display);
        } finally {
            closeDisplay(display);
        }
    }

    private static X11.Display openDisplay() {
        Xlib xlib = Xlib.INSTANCE;
        X11.Display display = xlib.XOpenDisplay(null);
        if (display == null) {
            throw new IllegalStateException("无法连接 X11/XWayland 显示服务，请检查 DISPLAY。");
        }

        OWNED_DISPLAYS.add(display.getPointer());
        initialize();
        return display;
    }

    private static void closeDisplay(X11.Display display) {
        Xlib.INSTANCE.XCloseDisplay(display);
        // Restoring an old handler here would overwrite GDK's concurrently pushed error trap.
        OWNED_DISPLAYS.remove(display.getPointer());
    }

    private static BufferedImage capture(X11.Display display, X11.Window window, Rectangle screenBounds) {
        Xlib xlib = Xlib.INSTANCE;
        X11.XImage image = null;
        try {
            X11.XWindowAttributes attributes = new X11.XWindowAttributes();
            if (xlib.XGetWindowAttributes(display, window, attributes) == 0
                    || attributes.map_state != X11.IsViewable) {
                return null;
            }
            IntByReference rootX = new IntByReference();
            IntByReference rootY = new IntByReference();
            if (!xlib.XTranslateCoordinates(display, window, attributes.root, 0, 0,
                    rootX, rootY, new X11.WindowByReference())) {
                return null;
            }
            Rectangle local = new Rectangle(screenBounds);
            local.translate(-rootX.getValue(), -rootY.getValue());
            if (!new Rectangle(attributes.width, attributes.height).contains(local)) {
                // Do not silently crop: that would shift the detected board and subsequent clicks.
                return null;
            }
            image = xlib.XGetImage(display, window, local.x, local.y, local.width, local.height,
                    new NativeLong(-1), X11.ZPixmap);
            return image == null ? null : toBufferedImage(new ImageData(image.getPointer()));
        } finally {
            if (image != null) {
                xlib.XDestroyImage(image);
            }
        }
    }

    static BufferedImage toBufferedImage(ImageData image) {
        if (image.bitsPerPixel != 16 && image.bitsPerPixel != 24 && image.bitsPerPixel != 32
                || image.redMask.longValue() == 0 || image.greenMask.longValue() == 0
                || image.blueMask.longValue() == 0) {
            throw new IllegalStateException("不支持的 X11 图像格式：" + image.bitsPerPixel + " 位。");
        }
        BufferedImage result = new BufferedImage(image.width, image.height, BufferedImage.TYPE_INT_RGB);
        int bytesPerPixel = image.bitsPerPixel / 8;
        int[] row = new int[image.width];
        byte[] data = new byte[image.bytesPerLine];
        for (int y = 0; y < image.height; y++) {
            image.data.read((long) y * image.bytesPerLine, data, 0, data.length);
            for (int x = 0; x < image.width; x++) {
                long pixel = 0;
                for (int b = 0; b < bytesPerPixel; b++) {
                    int shift = 8 * (image.byteOrder == X11.LSBFirst ? b : bytesPerPixel - b - 1);
                    pixel |= (data[x * bytesPerPixel + b] & 0xffL) << shift;
                }
                row[x] = component(pixel, image.redMask.longValue()) << 16
                        | component(pixel, image.greenMask.longValue()) << 8
                        | component(pixel, image.blueMask.longValue());
            }
            result.setRGB(0, y, image.width, 1, row, 0, image.width);
        }
        return result;
    }

    private static int component(long pixel, long mask) {
        int shift = Long.numberOfTrailingZeros(mask);
        return (int) (((pixel & mask) >>> shift) * 255 / (mask >>> shift));
    }

    private static final NativeLong NO_DELAY = new NativeLong(0);

    /** Sends directly to the target client without changing the X server's pointer/focus/stacking.
     * Client behaviour still matters: it may ignore synthetic events or activate its own window.
     * A successful XSendEvent is not an acknowledgement from the application.
     */
    public static void clickByBack(String windowId, Point from, Point to, int clickDelay, int moveDelay) {
        if (windowId == null || windowId.isBlank() || Thread.currentThread().isInterrupted()) {
            return;
        }
        X11.Window window = new X11.Window(Long.parseLong(windowId.trim()));
        Point first = new Point(from);
        Point second = new Point(to);
        withDisplay(display -> {
            X11.XWindowAttributes attributes = new X11.XWindowAttributes();
            if (Xlib.INSTANCE.XGetWindowAttributes(display, window, attributes) == 0
                    || !new Rectangle(attributes.width, attributes.height).contains(first)
                    || !new Rectangle(attributes.width, attributes.height).contains(second)) {
                return null;
            }
            boolean syntheticFocus = !hasInputFocus(display, window);
            try {
                if (syntheticFocus) {
                    // Chromium activates an inactive window on ButtonPress. Supply a logical focus
                    // context to its event handler without calling XSetInputFocus/XRaiseWindow.
                    sendFocus(display, window, true);
                }
                sendClick(display, window, first, clickDelay);
                pause(moveDelay);
                sendClick(display, window, second, clickDelay);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                // Do not tell the client it lost focus if the user really focused it meanwhile.
                if (syntheticFocus && !hasInputFocus(display, window)) {
                    sendFocus(display, window, false);
                }
            }
            return null;
        });
    }

    private record ClickTarget(X11.Window window, X11.Window root, Point local, Point global) { }

    private static boolean hasInputFocus(X11.Display display, X11.Window window) {
        Xlib xlib = Xlib.INSTANCE;
        X11.WindowByReference focus = new X11.WindowByReference();
        xlib.XGetInputFocus(display, focus, new IntByReference());
        X11.Window current = focus.getValue();
        for (int depth = 0; depth < 32 && current != null && current.longValue() > 1; depth++) {
            if (current.equals(window)) {
                return true;
            }
            X11.WindowByReference parent = new X11.WindowByReference();
            PointerByReference children = new PointerByReference();
            int status = xlib.XQueryTree(display, current, new X11.WindowByReference(), parent,
                    children, new IntByReference());
            if (children.getValue() != null) {
                xlib.XFree(children.getValue());
            }
            if (status == 0) {
                break;
            }
            current = parent.getValue();
        }
        return false;
    }

    private static void sendFocus(X11.Display display, X11.Window window, boolean focused) {
        X11.XFocusChangeEvent focus = new X11.XFocusChangeEvent();
        focus.type = focused ? X11.FocusIn : X11.FocusOut;
        focus.display = display;
        focus.window = window;
        focus.mode = X11.NotifyNormal;
        focus.detail = X11.NotifyNonlinear;
        X11.XEvent event = new X11.XEvent();
        event.setType(X11.XFocusChangeEvent.class);
        event.xfocus = focus;
        sendEvent(display, window, event);
    }

    private static ClickTarget clickTarget(X11.Display display, X11.Window window, Point point) {
        Xlib xlib = Xlib.INSTANCE;
        X11.XWindowAttributes attributes = new X11.XWindowAttributes();
        if (xlib.XGetWindowAttributes(display, window, attributes) == 0
                || !new Rectangle(attributes.width, attributes.height).contains(point)) {
            throw new IllegalStateException("连线目标窗口已关闭或改变大小，请重新选择窗口。");
        }
        IntByReference rootX = new IntByReference();
        IntByReference rootY = new IntByReference();
        if (!xlib.XTranslateCoordinates(display, window, attributes.root, point.x, point.y,
                rootX, rootY, new X11.WindowByReference())) {
            throw new IllegalStateException("无法转换连线目标窗口的鼠标坐标。");
        }
        X11.Window target = window;
        Point local = new Point(point);
        // Search only inside the selected window, independently of the real pointer/occluders.
        // XQueryTree returns children in bottom-to-top stacking order.
        for (int depth = 0; depth < 32; depth++) {
            PointerByReference children = new PointerByReference();
            IntByReference count = new IntByReference();
            X11.Window child = null;
            if (xlib.XQueryTree(display, target, new X11.WindowByReference(),
                    new X11.WindowByReference(), children, count) == 0) {
                break;
            }
            try {
                for (int i = count.getValue() - 1; i >= 0; i--) {
                    X11.Window candidate = new X11.Window(children.getValue()
                            .getNativeLong((long) i * NativeLong.SIZE).longValue());
                    X11.XWindowAttributes attrs = new X11.XWindowAttributes();
                    if (xlib.XGetWindowAttributes(display, candidate, attrs) == 0
                            || attrs.map_state == X11.IsUnmapped) {
                        continue;
                    }
                    IntByReference x = new IntByReference();
                    IntByReference y = new IntByReference();
                    if (xlib.XTranslateCoordinates(display, target, candidate, local.x, local.y,
                            x, y, new X11.WindowByReference())
                            && new Rectangle(attrs.width, attrs.height).contains(x.getValue(), y.getValue())) {
                        child = candidate;
                        local = new Point(x.getValue(), y.getValue());
                        break;
                    }
                }
            } finally {
                if (children.getValue() != null) {
                    xlib.XFree(children.getValue());
                }
            }
            if (child == null) {
                break;
            }
            target = child;
        }
        return new ClickTarget(target, attributes.root, local, new Point(rootX.getValue(), rootY.getValue()));
    }

    private static void sendClick(X11.Display display, X11.Window window, Point point, int clickDelay)
            throws InterruptedException {
        pause(0);
        ClickTarget target = clickTarget(display, window, point);
        X11.XMotionEvent motion = new X11.XMotionEvent();
        motion.type = X11.MotionNotify;
        motion.display = display;
        motion.window = target.window;
        motion.root = target.root;
        motion.subwindow = X11.Window.None;
        motion.time = NO_DELAY;
        motion.x = target.local.x;
        motion.y = target.local.y;
        motion.x_root = target.global.x;
        motion.y_root = target.global.y;
        motion.same_screen = 1;
        X11.XEvent event = new X11.XEvent();
        event.setType(X11.XMotionEvent.class);
        event.xmotion = motion;
        sendEvent(display, target.window, event);
        sendButton(display, target, true);
        try {
            pause(clickDelay);
        } finally {
            // Pair release with the same child even if stop() interrupts or the window moves.
            sendButton(display, target, false);
        }
    }

    private static void sendButton(X11.Display display, ClickTarget target, boolean pressed) {
        X11.XButtonEvent button = new X11.XButtonEvent();
        button.type = pressed ? X11.ButtonPress : X11.ButtonRelease;
        button.display = display;
        button.window = target.window;
        button.root = target.root;
        button.subwindow = X11.Window.None;
        button.time = NO_DELAY;
        button.x = target.local.x;
        button.y = target.local.y;
        button.x_root = target.global.x;
        button.y_root = target.global.y;
        button.state = pressed ? 0 : X11.Button1Mask;
        button.button = 1;
        button.same_screen = 1;
        X11.XEvent event = new X11.XEvent();
        event.setType(X11.XButtonEvent.class);
        event.xbutton = button;
        sendEvent(display, target.window, event);
    }

    private static void sendEvent(X11.Display display, X11.Window window, X11.XEvent event) {
        // NoEventMask sends to the owning client. Never propagate into another application's window.
        if (Xlib.INSTANCE.XSendEvent(display, window, 0, new NativeLong(0), event) == 0) {
            throw new IllegalStateException("X11 后台鼠标事件发送失败。");
        }
        Xlib.INSTANCE.XSync(display, false);
    }

    public static void clickByFront(String windowId, Point from, Point to, int clickDelay, int moveDelay) {
        if (windowId == null || windowId.isBlank() || Thread.currentThread().isInterrupted()) {
            return;
        }
        X11.Window window = new X11.Window(Long.parseLong(windowId.trim()));
        Point first = new Point(from);
        Point second = new Point(to);
        withDisplay(display -> {
            Xlib xlib = Xlib.INSTANCE;
            X11.XTest xtest = X11.XTest.INSTANCE;
            if (!xtest.XTestQueryExtension(display, new IntByReference(), new IntByReference(),
                    new IntByReference(), new IntByReference())) {
                throw new IllegalStateException("X11 显示服务不支持 XTEST，无法模拟鼠标点击。");
            }
            X11.XWindowAttributes attributes = new X11.XWindowAttributes();
            if (xlib.XGetWindowAttributes(display, window, attributes) == 0
                    || attributes.map_state != X11.IsViewable
                    || !new Rectangle(attributes.width, attributes.height).contains(first)
                    || !new Rectangle(attributes.width, attributes.height).contains(second)) {
                return null;
            }
            int screen = xlib.XScreenNumberOfScreen(attributes.screen);
            IntByReference oldX = new IntByReference();
            IntByReference oldY = new IntByReference();
            IntByReference buttons = new IntByReference();
            boolean restore = xlib.XQueryPointer(display, window, new X11.WindowByReference(),
                    new X11.WindowByReference(), oldX, oldY,
                    new IntByReference(), new IntByReference(), buttons);
            if ((buttons.getValue() & X11.Button1Mask) != 0) {
                return null;
            }
            try {
                clickAt(display, window, screen, first, clickDelay);
                pause(moveDelay);
                clickAt(display, window, screen, second, clickDelay);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                if (restore) {
                    xtest.XTestFakeMotionEvent(display, screen, oldX.getValue(), oldY.getValue(), NO_DELAY);
                    xlib.XSync(display, false);
                }
            }
            return null;
        });
    }

    private static void clickAt(X11.Display display, X11.Window window, int screen, Point point,
                                int clickDelay) throws InterruptedException {
        pause(0);
        Xlib xlib = Xlib.INSTANCE;
        X11.XTest xtest = X11.XTest.INSTANCE;
        X11.XWindowAttributes attributes = new X11.XWindowAttributes();
        if (xlib.XGetWindowAttributes(display, window, attributes) == 0
                || attributes.map_state != X11.IsViewable
                || !new Rectangle(attributes.width, attributes.height).contains(point)) {
            throw new IllegalStateException("连线目标窗口已关闭、隐藏或改变大小，请重新选择窗口。");
        }
        IntByReference rootX = new IntByReference();
        IntByReference rootY = new IntByReference();
        if (!xlib.XTranslateCoordinates(display, window, attributes.root, point.x, point.y,
                rootX, rootY, new X11.WindowByReference())) {
            throw new IllegalStateException("无法转换连线目标窗口的鼠标坐标。");
        }
        if (xtest.XTestFakeMotionEvent(display, screen, rootX.getValue(), rootY.getValue(), NO_DELAY) == 0) {
            throw new IllegalStateException("XTEST 鼠标移动失败。");
        }
        xlib.XSync(display, false);
        // libei/compositors may apply motion asynchronously, even after XSync.
        long deadline = System.nanoTime() + 10_000_000_000L;
        while (!pointerAt(display, window, point)) {
            if (System.nanoTime() >= deadline) {
                throw new IllegalStateException("鼠标未到达目标窗口；请保持窗口可见，并允许桌面的远程控制请求。");
            }
            pause(50);
            // The first motion may be consumed while XWayland creates/authorizes its libei device.
            xtest.XTestFakeMotionEvent(display, screen, rootX.getValue(), rootY.getValue(), NO_DELAY);
            xlib.XSync(display, false);
        }
        pause(0);
        if (xtest.XTestFakeButtonEvent(display, 1, true, NO_DELAY) == 0) {
            throw new IllegalStateException("XTEST 鼠标按下失败。");
        }
        try {
            xlib.XSync(display, false);
            pause(clickDelay);
        } finally {
            // Always release, including when stop() interrupts the hold delay.
            xtest.XTestFakeButtonEvent(display, 1, false, NO_DELAY);
            xlib.XSync(display, false);
        }
    }

    private static boolean pointerAt(X11.Display display, X11.Window window, Point point) {
        IntByReference x = new IntByReference();
        IntByReference y = new IntByReference();
        return Xlib.INSTANCE.XQueryPointer(display, window, new X11.WindowByReference(),
                new X11.WindowByReference(), new IntByReference(), new IntByReference(),
                x, y, new IntByReference()) && x.getValue() == point.x && y.getValue() == point.y;
    }

    private static void pause(int millis) throws InterruptedException {
        if (Thread.currentThread().isInterrupted()) {
            throw new InterruptedException();
        }
        if (millis > 0) {
            Thread.sleep(millis);
        }
    }
}
