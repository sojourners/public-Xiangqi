package com.sojourners.chess.enginee;


import com.sojourners.chess.config.Properties;
import com.sojourners.chess.model.BookData;
import com.sojourners.chess.model.EngineConfig;
import com.sojourners.chess.model.ThinkData;
import com.sojourners.chess.openbook.OpenBookManager;
import com.sojourners.chess.util.ExecutorsUtils;
import com.sojourners.chess.util.PathUtils;
import com.sojourners.chess.util.StringUtils;

import java.io.*;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 引擎封装
 */
public class Engine {

    private Process process;

    private String protocol;

    private AnalysisModel analysisModel;
    private long analysisValue;

    /**
     * 最后的分数
     */
    private Integer lastScore;

    private volatile boolean threadNumChange;
    private int threadNum;

    private volatile boolean hashSizeChange;
    private int hashSize;

    private BufferedReader reader;

    private BufferedWriter writer;

    private EngineCallBack cb;

    private Thread thread;

    private Random random;

    public enum AnalysisModel {
        FIXED_TIME,
        FIXED_STEPS,
        INFINITE;
    }

    public Integer getLastScore() {
        return lastScore;
    }

    public Engine(EngineConfig ec, EngineCallBack cb) throws IOException {
        this.protocol = ec.getProtocol();
        this.cb = cb;
        this.random = new SecureRandom();

        process = Runtime.getRuntime().exec(ec.getPath(), null, PathUtils.getParentDir(ec.getPath()));
        reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));

        (thread = new Thread(() -> {
            try {
                String line;
                while ((line = reader.readLine()) != null) {
//                    System.out.println(line);
                    if (line.contains("nps")) {
                        thinkDetail(line);
                    } else if (line.contains("bestmove")) {
                        bestMove(line);
                    }else if("info depth 0 score mate 0".equals(line)){
                        //说明是最后一步
                        int tmpScore = this.cb.isRedGo()? -30000 : 30000;
                        this.lastScore = this.cb.isReverse()? -tmpScore:tmpScore;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        })).start();

        cmd(protocol);

        for (Map.Entry<String, String> entry : ec.getOptions().entrySet()) {
            if ("uci".equals(this.protocol)) {
                cmd("setoption name " + entry.getKey() + " value " + entry.getValue());
            } else if ("ucci".equals(this.protocol)) {
                cmd("setoption " + entry.getKey() + " " + entry.getValue());
            }
        }
    }

    private void sleep(long t) {
        try {
            Thread.sleep(t);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static String test(String filePath, LinkedHashMap<String, String> options) {
        Process p = null;
        Thread h = null;
        BufferedWriter bw = null;
        BufferedReader br = null;
        try {
            p = Runtime.getRuntime().exec(filePath);
            bw = new BufferedWriter(new OutputStreamWriter(p.getOutputStream()));
            br = new BufferedReader(new InputStreamReader(p.getInputStream()));

            AtomicBoolean f = new AtomicBoolean(false);
            BufferedReader finalBr = br;
            (h = new Thread(() -> {
                try {
                    String line;
                    while ((line = finalBr.readLine()) != null) {
                        if ("uciok".equals(line) || "ucciok".equals(line) ) {
                            f.set(true);
                        }
                        if (line.startsWith("option") && line.contains("name") && line.contains("type") && line.contains("default")
                            && !line.contains("Threads") && !line.contains("Hash")) {

                            String[] str = line.split("name|type|default");
                            String key = str[1].trim();
                            String value = str[3].trim().split(" ")[0];
                            options.put(key, value);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            })).start();

            bw.write("uci" + System.getProperty("line.separator"));
            bw.flush();
            Thread.sleep(1000);
            if (f.get()) {
                return "uci";
            }

            bw.write("ucci" + System.getProperty("line.separator"));
            bw.flush();
            Thread.sleep(1000);
            if (f.get()) {
                return "ucci";
            }

            return null;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if (p != null) {
                p.destroy();
            }
            if (h.isAlive()) {
                h.stop();
            }
            try {
                if (bw != null) {
                    bw.close();
                }
                if (br != null) {
                    br.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private boolean validateMove(String move) {
        if (StringUtils.isEmpty(move) || move.length() != 4) {
            return false;
        }
        if (move.charAt(0) < 'a' || move.charAt(0) > 'i' || move.charAt(2) < 'a' || move.charAt(2) > 'i') {
            return false;
        }
        if (move.charAt(1) < '0' || move.charAt(1) > '9' || move.charAt(3) < '0' || move.charAt(3) > '9') {
            return false;
        }
        return true;
    }
    private void bestMove(String msg) {
        String[] str = msg.split(" ");
        if (str.length < 2 || !validateMove(str[1])) {
            return;
        }
        if (Properties.getInstance().getEngineDelayEnd() > 0 && Properties.getInstance().getEngineDelayEnd() >= Properties.getInstance().getEngineDelayStart()) {
            int t = random.nextInt(Properties.getInstance().getEngineDelayStart(), Properties.getInstance().getEngineDelayEnd());
            if(!AnalysisModel.INFINITE.equals(this.analysisModel) && !this.cb.getReplayFlag()){
                sleep(t);
            }
        }
        cb.bestMove(str[1], str.length == 4 ? str[3] : null);
    }
    private void thinkDetail(String msg) {
        String[] str = msg.split(" ");
        ThinkData td = new ThinkData();
        List<String> detail = new ArrayList<>();
        td.setDetail(detail);
        int flag = 0;
        for (int i = 0; i < str.length; i++) {
            if (flag != 0) {
                if (flag == 6) {
                    detail.add(str[i]);
                } else {
                    if (StringUtils.isDigit(str[i])) {
                        if (flag == 1) {
                            td.setNps(Long.parseLong(str[i]));

                        } else if (flag == 2) {
                            td.setTime(Long.parseLong(str[i]));

                        } else if (flag == 3) {
                            td.setDepth(Integer.parseInt(str[i]));
                        } else if (flag == 4) {
                            td.setMate(Integer.parseInt(str[i]));

                        } else if (flag == 5) {
                            td.setScore(Integer.parseInt(str[i]));
                        }
                        flag = 0;
                    } else {
                        continue;
                    }
                }
            } else {
                if ("depth".equals(str[i])) {
                    flag = 3;
                } else if ("score".equals(str[i])) {
                    if ("mate".equals(str[i + 1])) {
                        flag = 4;
                    } else {
                        flag = 5;
                    }
                } else if ("mate".equals(str[i])) {
                    flag = 4;
                } else if ("nps".equals(str[i])) {
                    flag = 1;
                } else if ("time".equals(str[i])) {
                    flag = 2;
                } else if ("pv".equals(str[i])) {
                    flag = 6;
                }
            }
        }
        if (td.getDetail().size() > 0) {
            if(cb.getReplayFlag()){
                this.lastScore = td.calculateScore(this.cb.isRedGo(),this.cb.isReverse());
            }
            cb.thinkDetail(td);
        }
    }

    public void analysis(String fenCode, List<String> moves, char[][] board, boolean redGo) {
        ExecutorsUtils.getInstance().exec(() -> {
            if (Properties.getInstance().getBookSwitch() && !cb.getReplayFlag()) {
                long s = System.currentTimeMillis();
                List<BookData> results = OpenBookManager.getInstance().queryBook(board, redGo, moves.size() / 2 >= Properties.getInstance().getOffManualSteps());
                System.out.println("查询库时间" + (System.currentTimeMillis() - s));
                this.cb.showBookResults(results);
                if (results.size() > 0 && this.analysisModel != AnalysisModel.INFINITE) {
                    if (Properties.getInstance().getBookDelayEnd() > 0 && Properties.getInstance().getBookDelayEnd() >= Properties.getInstance().getBookDelayStart()) {
                        int t = random.nextInt(Properties.getInstance().getBookDelayStart(), Properties.getInstance().getBookDelayEnd());
                        if(!AnalysisModel.INFINITE.equals(this.analysisModel) && !this.cb.getReplayFlag()){
                            sleep(t);
                        }
                    }
                    this.cb.bestMove(results.get(0).getMove(), null);
                    return;
                }

            }
            this.analysis(fenCode, moves);
        });

    }

    private void analysis(String fenCode, List<String> moves) {
        stop();

        if (threadNumChange) {
            cmd(("uci".equals(this.protocol) ? "setoption name Threads value " : "setoption Threads ") + threadNum);
            this.threadNumChange = false;
        }
        if (hashSizeChange) {
            cmd(("uci".equals(this.protocol) ? "setoption name Hash value " : "setoption Hash ") + hashSize);
            this.hashSizeChange = false;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("position fen ").append(fenCode);
        if (moves != null && moves.size() > 0) {
            sb.append(" moves");
            for (String move : moves) {
                sb.append(" ").append(move);
            }
        }
        cmd(sb.toString());

        if (analysisModel == AnalysisModel.FIXED_STEPS) {
            cmd("go depth " + analysisValue);
        } else if (analysisModel == AnalysisModel.FIXED_TIME) {
            cmd("go movetime " + analysisValue);
        } else {
            cmd("go infinite");
        }
    }

    public void stop() {
        cmd("stop");
    }

    private void cmd(String command) {
        System.out.println(command);
        try {
            writer.write(command + System.getProperty("line.separator"));
            writer.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setThreadNum(int threadNum) {
        if (threadNum != this.threadNum) {
            this.threadNum = threadNum;
            this.threadNumChange = true;
        }

    }

    public void setHashSize(int hashSize) {
        if (hashSize != this.hashSize) {
            this.hashSize = hashSize;
            this.hashSizeChange = true;
        }
    }

    public void setAnalysisModel(AnalysisModel model, long v) {
        this.analysisModel = model;
        this.analysisValue = v;
    }

    public void close() {
        try {
            if (process.isAlive()) {
                cmd("quit");
            }

            if (thread.isAlive()) {
                thread.stop();
            }

            if (process.isAlive()) {
                process.destroy();
            }

            if (reader != null) {
                reader.close();
            }
            if (writer != null) {
                writer.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
