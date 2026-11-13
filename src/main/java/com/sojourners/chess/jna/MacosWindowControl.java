package com.sojourners.chess.jna;

import com.sun.jna.Function;
import com.sun.jna.Library;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.platform.mac.CoreFoundation.CFArrayRef;
import com.sun.jna.platform.mac.CoreFoundation.CFBooleanRef;
import com.sun.jna.platform.mac.CoreFoundation.CFDictionaryRef;
import com.sun.jna.platform.mac.CoreFoundation.CFNumberRef;
import com.sun.jna.platform.mac.CoreFoundation.CFStringRef;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.function.BooleanSupplier;

/** Native macOS window geometry, background capture and process-directed mouse input. */
public final class MacosWindowControl {

    private static final int ON_SCREEN_ONLY = 1;
    private static final int INCLUDING_WINDOW = 1 << 3;
    private static final int EXCLUDE_DESKTOP_ELEMENTS = 1 << 4;

    private static final int IGNORE_FRAMING = 1;
    private static final int NOMINAL_RESOLUTION = 1 << 4;
    private static final int BGRA_PREMULTIPLIED = 0x2002;

    private static final int LEFT_DOWN = 1;
    private static final int LEFT_UP = 2;
    private static final int MOUSE_MOVED = 5;
    private static final int CLICK_STATE = 1;
    private static final int TARGET_PID = 40;
    private static final int WINDOW_UNDER_POINTER = 91;
    private static final int WINDOW_HANDLING_EVENT = 92;
    private static final long COMMAND_FLAG = 1L << 20; // kCGEventFlagMaskCommand

    public record Window(int id, int pid) {
    }

    private interface CoreGraphics extends Library {
        CFArrayRef CGWindowListCopyWindowInfo(int options, int windowId);

        byte CGRectMakeWithDictionaryRepresentation(CFDictionaryRef dictionary, CGRect rect);

        byte CGPreflightScreenCaptureAccess();

        // Deprecated since macOS 14; a future capture implementation can use ScreenCaptureKit.
        Pointer CGWindowListCreateImage(CGRect.ByValue bounds,
                                        int options, int windowId, int imageOptions);

        NativeLong CGImageGetWidth(Pointer image);
        NativeLong CGImageGetHeight(Pointer image);
        void CGImageRelease(Pointer image);
        Pointer CGColorSpaceCreateDeviceRGB();
        void CGColorSpaceRelease(Pointer colorSpace);
        Pointer CGBitmapContextCreate(Pointer data, NativeLong width, NativeLong height,
                                      NativeLong bitsPerComponent, NativeLong bytesPerRow,
                                      Pointer colorSpace, int bitmapInfo);
        void CGContextDrawImage(Pointer context, CGRect.ByValue bounds, Pointer image);
        void CGContextRelease(Pointer context);

        byte CGPreflightPostEventAccess();

        Pointer CGEventCreate(Pointer source);

        Pointer CGEventCreateCopy(Pointer event);

        long CGEventGetTimestamp(Pointer event);

        void CGEventSetTimestamp(Pointer event, long timestamp);

        void CGEventSetLocation(Pointer event, CGPoint point);

        void CGEventSetIntegerValueField(Pointer event, int field, long value);

        void CGEventSetFlags(Pointer event, long flags);

        void CGEventPostToPid(int pid, Pointer event);
    }

    private interface Foundation extends Library {
        void CFRelease(Pointer object);
    }

    private interface ObjectiveC extends Library {
        Pointer objc_getClass(String name);

        Pointer sel_registerName(String name);

        Pointer objc_msgSend(Pointer object, Pointer selector);

        // NSEventType, NSEventModifierFlags and NSInteger are 64-bit on supported macOS.
        Pointer objc_msgSend(Pointer object, Pointer selector, long type, CGPoint location,
                             long flags, double timestamp, long windowNumber, Pointer context,
                             long eventNumber, long clickCount, float pressure);
    }

    // Load macOS frameworks only when a native query is actually needed.
    private static final class Api {
        private static final CoreGraphics CG = Native.load("CoreGraphics", CoreGraphics.class);
        private static final NativeLibrary LIBRARY = NativeLibrary.getInstance("CoreGraphics");
        private static final CFStringRef NUMBER = key("kCGWindowNumber");
        private static final CFStringRef OWNER_PID = key("kCGWindowOwnerPID");
        private static final CFStringRef LAYER = key("kCGWindowLayer");
        private static final CFStringRef ON_SCREEN = key("kCGWindowIsOnscreen");
        private static final CFStringRef BOUNDS = key("kCGWindowBounds");

        private static CFStringRef key(String name) {
            // Framework-owned constants: these references must not be released.
            return new CFStringRef(LIBRARY.getGlobalVariableAddress(name).getPointer(0));
        }
    }

    // AppKit and Objective-C are only needed for background mouse input.
    private static final class InputApi {
        private static final Foundation CF = Native.load("CoreFoundation", Foundation.class);
        private static final NativeLibrary APP_KIT = NativeLibrary.getInstance("AppKit");
        private static final ObjectiveC OBJC = Native.load("objc", ObjectiveC.class);
        private static final Pointer EVENT = OBJC.objc_getClass("NSEvent");
        private static final Pointer POOL = OBJC.objc_getClass("NSAutoreleasePool");
        private static final Pointer NEW = OBJC.sel_registerName("new");
        private static final Pointer DRAIN = OBJC.sel_registerName("drain");
        private static final Pointer CG_EVENT = OBJC.sel_registerName("CGEvent");
        private static final Pointer MOUSE_EVENT = OBJC.sel_registerName(
                "mouseEventWithType:location:modifierFlags:timestamp:windowNumber:context:eventNumber:clickCount:pressure:");
        private static final Function SET_WINDOW_LOCATION = windowLocationFunction();

        private static Function windowLocationFunction() {
            try {
                // Exported by CoreGraphics, but not declared in its public headers. Detect it
                // explicitly: sending a down event without this produces an invalid local point.
                return Api.LIBRARY.getFunction("CGEventSetWindowLocation");
            } catch (UnsatisfiedLinkError unsupported) {
                return null;
            }
        }
    }

    // CGPoint followed by CGSize; CGFloat is a double on both Intel and Apple Silicon macOS.
    @Structure.FieldOrder({"x", "y", "width", "height"})
    public static class CGRect extends Structure {
        public double x, y, width, height;

        public static class ByValue extends CGRect implements Structure.ByValue {
            public ByValue() {
            }

            public ByValue(double x, double y, double width, double height) {
                this.x = x;
                this.y = y;
                this.width = width;
                this.height = height;
            }
        }
    }

    @Structure.FieldOrder({"x", "y"})
    public static class CGPoint extends Structure implements Structure.ByValue {
        public double x, y;

        public CGPoint() {
        }

        CGPoint(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }

    private MacosWindowControl() {
    }

    public static Window findFrontmostWindow(int pid) {
        if (pid <= 0) {
            return null;
        }
        CFArrayRef windows = Api.CG.CGWindowListCopyWindowInfo(
                ON_SCREEN_ONLY | EXCLUDE_DESKTOP_ELEMENTS, 0);
        if (windows == null) {
            return null;
        }
        try {
            // CoreGraphics returns on-screen windows in front-to-back order.
            int count = windows.getCount();
            for (int i = 0; i < count; i++) {
                CFDictionaryRef info = new CFDictionaryRef(windows.getValueAtIndex(i));
                Integer ownerPid = number(info, Api.OWNER_PID);
                Integer layer = number(info, Api.LAYER);
                if (ownerPid == null || ownerPid != pid || layer == null || layer != 0) {
                    continue;
                }
                Integer id = number(info, Api.NUMBER);
                if (id != null && id != 0 && isOnScreen(info) && !bounds(info).isEmpty()) {
                    return new Window(id, pid);
                }
            }
            return null;
        } finally {
            // The dictionaries and their values are borrowed from this array.
            windows.release();
        }
    }

    public static Rectangle getBounds(Window window) {
        if (window == null || window.id() == 0 || window.pid() <= 0) {
            return new Rectangle();
        }
        CFArrayRef windows = Api.CG.CGWindowListCopyWindowInfo(INCLUDING_WINDOW, window.id());
        if (windows == null) {
            return new Rectangle();
        }
        try {
            int count = windows.getCount();
            for (int i = 0; i < count; i++) {
                CFDictionaryRef info = new CFDictionaryRef(windows.getValueAtIndex(i));
                Integer id = number(info, Api.NUMBER);
                Integer pid = number(info, Api.OWNER_PID);
                if (id != null && id == window.id() && pid != null && pid == window.pid()
                        && isOnScreen(info)) {
                    return bounds(info);
                }
            }
            // Do not switch to another window if the selected one is closed or minimized.
            return new Rectangle();
        } finally {
            windows.release();
        }
    }

    private static Integer number(CFDictionaryRef info, CFStringRef key) {
        Pointer value = info.getValue(key);
        return value == null ? null : new CFNumberRef(value).intValue();
    }

    private static boolean isOnScreen(CFDictionaryRef info) {
        Pointer value = info.getValue(Api.ON_SCREEN);
        return value != null && new CFBooleanRef(value).booleanValue();
    }

    private static Rectangle bounds(CFDictionaryRef info) {
        Pointer value = info.getValue(Api.BOUNDS);
        if (value == null) {
            return new Rectangle();
        }
        CGRect rect = new CGRect();
        if (Api.CG.CGRectMakeWithDictionaryRepresentation(new CFDictionaryRef(value), rect) == 0
                || !Double.isFinite(rect.x) || !Double.isFinite(rect.y)
                || !Double.isFinite(rect.width) || !Double.isFinite(rect.height)
                || rect.width <= 0 || rect.height <= 0) {
            return new Rectangle();
        }
        // Screen points already match Robot's logical coordinates; do not apply Retina scaling.
        return new Rectangle2D.Double(rect.x, rect.y, rect.width, rect.height).getBounds();
    }

    /** localBounds is in window-local screen points; null requests the entire window. */
    public static BufferedImage capture(Window window, Rectangle localBounds) {
        if (Thread.currentThread().isInterrupted()) {
            return null;
        }
        Rectangle bounds = getBounds(window);
        Rectangle local = localBounds == null ? new Rectangle(bounds.width, bounds.height)
                : new Rectangle(localBounds);
        if (bounds.isEmpty() || local.isEmpty()
                || !new Rectangle(bounds.width, bounds.height).contains(local)) {
            return null;
        }
        // Capturing our own windows needs no TCC grant (also useful for native integration tests).
        if (window.pid() != ProcessHandle.current().pid() && Api.CG.CGPreflightScreenCaptureAccess() == 0) {
            return null;
        }
        // CGRectNull lets CoreGraphics use the whole backing image, even if partly off screen.
        Pointer image = Api.CG.CGWindowListCreateImage(
                new CGRect.ByValue(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, 0, 0),
                INCLUDING_WINDOW, window.id(), IGNORE_FRAMING | NOMINAL_RESOLUTION);
        if (image == null) {
            return null;
        }
        try {
            // Refuse a frame whose size changed during capture instead of misaligning board coordinates.
            if (Api.CG.CGImageGetWidth(image).longValue() != bounds.width
                    || Api.CG.CGImageGetHeight(image).longValue() != bounds.height) {
                return null;
            }
            return toBufferedImage(image, bounds.width, bounds.height, local);
        } finally {
            Api.CG.CGImageRelease(image);
        }
    }

    private static BufferedImage toBufferedImage(Pointer image, int width, int height, Rectangle local) {
        long pixels = (long) local.width * local.height;
        if (pixels > Integer.MAX_VALUE) {
            return null;
        }
        Pointer colorSpace = Api.CG.CGColorSpaceCreateDeviceRGB();
        if (colorSpace == null) {
            return null;
        }
        try (Memory buffer = new Memory(pixels * Integer.BYTES)) {
            buffer.clear();
            Pointer context = Api.CG.CGBitmapContextCreate(buffer,
                    new NativeLong(local.width), new NativeLong(local.height), new NativeLong(8),
                    new NativeLong((long) local.width * Integer.BYTES), colorSpace, BGRA_PREMULTIPLIED);
            if (context == null) {
                return null;
            }
            try {
                // Image rows are top-down in the bitmap. Offset the full image to crop in window coordinates.
                Api.CG.CGContextDrawImage(context,
                        new CGRect.ByValue(-local.x, local.y + local.height - height,
                                width, height), image);
                BufferedImage result = new BufferedImage(local.width, local.height, BufferedImage.TYPE_INT_ARGB_PRE);
                int[] data = ((DataBufferInt) result.getRaster().getDataBuffer()).getData();
                buffer.read(0, data, 0, data.length);
                // CoreGraphics can return an all-transparent image for a non-shareable/missing window.
                for (int pixel : data) {
                    if ((pixel >>> 24) != 0) {
                        return result;
                    }
                }
                return null;
            } finally {
                Api.CG.CGContextRelease(context);
            }
        } finally {
            Api.CG.CGColorSpaceRelease(colorSpace);
        }
    }

    static boolean isSupported() {
        return InputApi.SET_WINDOW_LOCATION != null && InputApi.EVENT != null;
    }

    static boolean hasPermission() {
        return Api.CG.CGPreflightPostEventAccess() != 0;
    }

    public static boolean click(Window window, Point from, Point to,
                                int clickDelay, int moveDelay, BooleanSupplier stillSelected) {
        if (window == null || from == null || to == null || Thread.currentThread().isInterrupted()
                || !stillSelected.getAsBoolean()) {
            return false;
        }
        Rectangle bounds = getBounds(window);
        if (!contains(bounds, from) || !contains(bounds, to)) {
            return false;
        }
        if (!isSupported() || !hasPermission()) {
            return false;
        }
        if (!clickOne(window, from, clickDelay, stillSelected) || !pause(moveDelay)) {
            return false;
        }
        // Query again: the window may have moved, closed or minimized between board clicks.
        return clickOne(window, to, clickDelay, stillSelected);
    }

    private static boolean clickOne(Window window, Point point, int delay,
                                    BooleanSupplier stillSelected) {
        if (Thread.currentThread().isInterrupted() || !stillSelected.getAsBoolean()) {
            return false;
        }
        Rectangle bounds = getBounds(window);
        if (!contains(bounds, point)) {
            return false;
        }
        Pointer move = null, down = null, up = null;
        boolean pressed = false;
        try {
            Pointer pool = InputApi.OBJC.objc_msgSend(InputApi.POOL, InputApi.NEW);
            try {
                // Allocate the release event before posting a press, so allocation failure cannot
                // leave the destination application with a stuck mouse button.
                move = event(window, bounds, point, MOUSE_MOVED);
                down = event(window, bounds, point, LEFT_DOWN);
                up = event(window, bounds, point, LEFT_UP);
            } finally {
                // Autorelease pools belong to an OS thread. Drain before sleeping, since a
                // virtual thread can resume on a different carrier after the click delay.
                if (pool != null) {
                    InputApi.OBJC.objc_msgSend(pool, InputApi.DRAIN);
                }
            }
            if (move == null || down == null || up == null || Thread.currentThread().isInterrupted()
                    || !stillSelected.getAsBoolean()) {
                return false;
            }
            post(window.pid(), move);
            post(window.pid(), down);
            pressed = true;
            return pause(delay);
        } finally {
            try {
                // Even an interrupt after mouse-down must be paired with mouse-up.
                if (pressed) {
                    post(window.pid(), up);
                }
            } finally {
                release(up);
                release(down);
                release(move);
            }
        }
    }

    private static Pointer event(Window window, Rectangle bounds, Point point, int type) {
        // AppKit's public constructor attaches the real window number to the event. The
        // public CGWindow-under-pointer fields alone do not populate NSEvent.windowNumber.
        Pointer nsEvent = InputApi.OBJC.objc_msgSend(InputApi.EVENT, InputApi.MOUSE_EVENT, (long) type,
                new CGPoint(point.x, bounds.height - point.y), 0L, 0.0,
                Integer.toUnsignedLong(window.id()), null, 0L, 1L, type == LEFT_DOWN ? 1.0f : 0.0f);
        if (nsEvent == null) {
            return null;
        }
        Pointer borrowed = InputApi.OBJC.objc_msgSend(nsEvent, InputApi.CG_EVENT);
        Pointer event = borrowed == null ? null : Api.CG.CGEventCreateCopy(borrowed);
        if (event == null) {
            return null;
        }
        try {
            Api.CG.CGEventSetLocation(event, new CGPoint((double) bounds.x + point.x, (double) bounds.y + point.y));
            InputApi.SET_WINDOW_LOCATION.invokeVoid(new Object[]{event, new CGPoint(point.x, point.y)});
            // AppKit can discard a normal click on an inactive window (acceptsFirstMouse = NO).
            // Command-click delivers it without activating the window. This is an event flag;
            // no keyboard event is posted, but web content will see metaKey = true.
            Api.CG.CGEventSetFlags(event, type == MOUSE_MOVED ? 0 : COMMAND_FLAG);
            Api.CG.CGEventSetIntegerValueField(event, CLICK_STATE, type == MOUSE_MOVED ? 0 : 1);
            Api.CG.CGEventSetIntegerValueField(event, TARGET_PID, window.pid());
            Api.CG.CGEventSetIntegerValueField(event, WINDOW_UNDER_POINTER, Integer.toUnsignedLong(window.id()));
            Api.CG.CGEventSetIntegerValueField(event, WINDOW_HANDLING_EVENT, Integer.toUnsignedLong(window.id()));
            return event;
        } catch (RuntimeException | Error failure) {
            release(event);
            throw failure;
        }
    }

    private static void post(int pid, Pointer event) {
        // Timestamp at delivery, including mouse-up after its configured delay.
        Pointer now = Api.CG.CGEventCreate(null);
        if (now != null) {
            try {
                Api.CG.CGEventSetTimestamp(event, Api.CG.CGEventGetTimestamp(now));
            } finally {
                release(now);
            }
        }
        Api.CG.CGEventPostToPid(pid, event);
    }

    static boolean contains(Rectangle bounds, Point point) {
        return bounds != null && point != null && !bounds.isEmpty()
                && point.x >= 0 && point.y >= 0 && point.x < bounds.width && point.y < bounds.height;
    }

    private static boolean pause(int millis) {
        if (Thread.currentThread().isInterrupted()) {
            return false;
        }
        try {
            if (millis > 0) {
                Thread.sleep(millis);
            }
            return !Thread.currentThread().isInterrupted();
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private static void release(Pointer object) {
        if (object != null) {
            InputApi.CF.CFRelease(object);
        }
    }

}
