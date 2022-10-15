package com.sojourners.chess.lock;

public class SingleLock {
    private volatile boolean isLock = false;

    public synchronized void lock() {
        while (isLock) {
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        isLock = true;
    }

    public synchronized void unlock() {
        isLock = false;
        notify();
    }
}
