package com.suraj.model;

import lombok.ToString;

@ToString
public class SockAddr {

    private String ip;
    private int port;
    private boolean allowed;

    public SockAddr(String ip, int port, boolean allowed) {
        this.ip = ip;
        this.port = port;
        this.allowed = allowed;
    }

    @Override
    public int hashCode() {
        return (ip + ":" + port).hashCode();
    }
}
