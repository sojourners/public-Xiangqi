package com.sojourners.chess.model;

import java.io.Serializable;

public class EngineConfig implements Serializable {

    private static final long serialVersionUID = 1323134234;

    private String name;

    private String path;

    private String protocol;

    public EngineConfig(String name, String path, String protocol) {
        this.name = name;
        this.path = path;
        this.protocol = protocol;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }
}
