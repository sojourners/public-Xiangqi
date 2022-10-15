package com.sojourners.chess.model;

public class LocalBook {
    private String path;

    public LocalBook(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
