package com.sojourners.chess.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ExecutorsUtils {

    private static volatile ExecutorsUtils instance;

    private ExecutorService threadPoolExecutor;

    private ExecutorsUtils() {
        threadPoolExecutor = Executors.newSingleThreadExecutor();
    }

    public static ExecutorsUtils getInstance() {
        if (instance == null) {
            synchronized (ExecutorsUtils.class) {
                if (instance == null) {
                    instance = new ExecutorsUtils();
                }
            }
        }
        return instance;
    }

    public void exec(Runnable task) {
        threadPoolExecutor.execute(task);
    }

    public void close() {
        threadPoolExecutor.shutdownNow();
    }

}
