package com.sojourners.chess.linker;

import com.sojourners.chess.util.ShellUtils;
import com.sojourners.chess.util.StringUtils;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * macos 连线器，基于osascript 实现
 * 使用方法：点击连线按钮，3秒内再点击目标平台，然后等待连线识别成功即可
 */
public class MacosGraphLinker extends AbstractGraphLinker {

    private String windowId;

    public MacosGraphLinker(LinkerCallBack callBack) throws AWTException {
        super(callBack);
    }

    @Override
    public void getTargetWindowId() {
        sleep(3000);
        String script = "tell application \"System Events\" to return first application process whose frontmost is true";
        String result = ShellUtils.exec(new String[]{"osascript", "-e", script });
        this.windowId = result.substring(20, result.length() - 1);

        scan();
    }

    @Override
    public Rectangle getTargetWindowPosition() {
        String[] cmd = new String[]{"osascript",
                "-e", "tell application \"System Events\"",
                "-e", "set frontmostWindow to window 1 of application process \"" + this.windowId + "\"",
                "-e", "set windowPosition to position of frontmostWindow",
                "-e", "set windowSize to size of frontmostWindow",
                "-e", "end tell",
                "-e", "return {windowPosition, windowSize}"
        };
        String result = ShellUtils.exec(cmd);
        if (StringUtils.isEmpty(result)) {
            System.out.println(this.windowId + " getTargetWindowPosition failed");
            return new Rectangle();
        }
        String[] ss = result.split(",");
        int x = Integer.parseInt(ss[0].trim());
        int y = Integer.parseInt(ss[1].trim());
        int w = Integer.parseInt(ss[2].trim());
        int h = Integer.parseInt(ss[3].trim());
        return new Rectangle(x, y, w, h);
    }

    /**
     * 后台模式暂未实现
     * @param windowPos
     * @return
     */
    @Override
    public BufferedImage screenshotByBack(Rectangle windowPos) {
        return null;
    }

    /**
     * 后台模式暂未实现
     * @param p1
     * @param p2
     */
    @Override
    public void mouseClickByBack(Point p1, Point p2) {

    }
}
