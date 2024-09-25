package com.sojourners.chess.util;

import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class ClipboardUtils {
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
    public static Image getImage() {
        Clipboard sysc = Toolkit.getDefaultToolkit().getSystemClipboard();
        Transferable cc = sysc.getContents(null);
        if (cc == null)
            return null;
        else if (cc.isDataFlavorSupported(DataFlavor.imageFlavor)) {
            try {
                Image image = (Image) cc.getTransferData(DataFlavor.imageFlavor);
                if (image instanceof BufferedImage) {
                    // 当图片本身就是BufferedImage实例，直接返回
                    return image;
                }
                // 否则需要从当前提供的Image对象创建一个BufferedImage
                int width = image.getWidth(null);
                int height = image.getHeight(null);
                // 创建兼容型buffered image，确保图片适用于当前系统平台窗口
                BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                Graphics2D bGr = bufferedImage.createGraphics();
                bGr.drawImage(image, 0, 0, null);
                bGr.dispose();
                // 返回已经画好的bufferedImage对象
                return bufferedImage;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
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
