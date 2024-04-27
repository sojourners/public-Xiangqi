package com.sojourners.chess.util;

import java.io.InputStreamReader;
import java.io.LineNumberReader;

public class ShellUtils {

    public static String exec(String command) {
        String[] cmd = { "bash", "-c", command};
         return exec(cmd);
    }

    public static String exec(String[] cmd) {
        try {
            Process p = Runtime.getRuntime().exec(cmd);
            InputStreamReader ir = new InputStreamReader(p.getInputStream());
            LineNumberReader input = new LineNumberReader(ir);
            String line;
            StringBuffer sb = new StringBuffer();
            while ((line = input.readLine ()) != null){
                sb.append(line);
                sb.append(System.getProperty("line.separator"));
            }
            p.waitFor();
            p.destroy();
            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
