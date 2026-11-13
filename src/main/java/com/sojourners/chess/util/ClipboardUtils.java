package com.sojourners.chess.util;

import com.sun.jna.Platform;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

public class ClipboardUtils {
    static {
        if (Platform.isLinux()) {
            // X11 exposes these legacy targets as imageFlavor only by default.
            SystemFlavorMap flavors = (SystemFlavorMap) SystemFlavorMap.getDefaultFlavorMap();
            flavors.addFlavorForUnencodedNative("PNG", new DataFlavor("image/x-x11-png", "X11 PNG"));
            flavors.addFlavorForUnencodedNative("JFIF", new DataFlavor("image/x-x11-jfif", "X11 JFIF"));
        }
    }

    /**
     * 从剪切板获得文字。
     */
    public static String getText() {
        String ret = "";
        Clipboard sysClip = Toolkit.getDefaultToolkit().getSystemClipboard();
        // 获取剪切板中的内容
        Transferable clipTf = sysClip.getContents(null);

        if (clipTf != null) {
            // 检查内容是否是文本类型
            if (clipTf.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                try {
                    ret = (String) clipTf.getTransferData(DataFlavor.stringFlavor);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        return ret;
    }

    /**
     * 将字符串复制到剪切板。
     */
    public static void setText(String writeMe) {
        Clipboard clip = Toolkit.getDefaultToolkit().getSystemClipboard();
        Transferable tText = new StringSelection(writeMe);
        clip.setContents(tText, null);
    }

    /**
     * 从剪切板获得图片。
     */
    public static BufferedImage getImage() {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        try {
            return getImage(clipboard.getContents(null));
        } catch (IllegalStateException e) {
            // The system clipboard may be temporarily unavailable.
            return null;
        }
    }

    static BufferedImage getImage(Transferable contents) {
        if (contents == null) {
            return null;
        }
        if (contents.isDataFlavorSupported(DataFlavor.imageFlavor)) {
            try {
                if (contents.getTransferData(DataFlavor.imageFlavor) instanceof Image image) {
                    BufferedImage bufferedImage = toBufferedImage(image);
                    if (bufferedImage != null) {
                        return bufferedImage;
                    }
                }
            } catch (IOException | UnsupportedFlavorException | RuntimeException e) {
                // AWT selects its decoder from the advertised native format.
            }
        }

        for (DataFlavor flavor : contents.getTransferDataFlavors()) {
            if (!"image".equals(flavor.getPrimaryType()) || !flavor.isRepresentationClassInputStream()) {
                continue;
            }
            try {
                if (!(contents.getTransferData(flavor) instanceof InputStream input)) {
                    continue;
                }
                try (input) {
                    // ImageIO identifies the format from the bytes in the stream.
                    BufferedImage image = ImageIO.read(input);
                    if (image != null) {
                        return image;
                    }
                }
            } catch (IOException | UnsupportedFlavorException | RuntimeException e) {
                // Another advertised image format may still be readable.
            }
        }
        return null;
    }

    private static BufferedImage toBufferedImage(Image image) {
        if (image instanceof BufferedImage bufferedImage) {
            return bufferedImage;
        }
        int width = image.getWidth(null);
        int height = image.getHeight(null);
        if (width <= 0 || height <= 0) {
            return null;
        }
        BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = bufferedImage.createGraphics();
        try {
            return graphics.drawImage(image, 0, 0, null) ? bufferedImage : null;
        } finally {
            graphics.dispose();
        }
    }

    /**
     * 复制图片到剪切板。
     */
    public static void setImage(final Image image) {
        Transferable trans = new Transferable() {
            public DataFlavor[] getTransferDataFlavors() {
                return new DataFlavor[] { DataFlavor.imageFlavor };
            }

            public boolean isDataFlavorSupported(DataFlavor flavor) {
                return DataFlavor.imageFlavor.equals(flavor);
            }

            public Object getTransferData(DataFlavor flavor)
                    throws UnsupportedFlavorException, IOException {
                if (isDataFlavorSupported(flavor))
                    return image;
                throw new UnsupportedFlavorException(flavor);
            }

        };
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(trans,
                null);
    }
}
