package com.sojourners.chess.util;

import com.sun.jna.Platform;


public class PathUtils {
    public static String getJarPath() {
        try {
            String path = PathUtils.class.getProtectionDomain().getCodeSource().getLocation().getPath();
            path = java.net.URLDecoder.decode(path, "UTF-8");
            if (Platform.isWindows() && path.startsWith("/")) {
                path = path.substring(1);
            }
            int i = path.lastIndexOf("/");
            if (i >= 0) {
                path = path.substring(0, i + 1);
            }
            return path;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


}
