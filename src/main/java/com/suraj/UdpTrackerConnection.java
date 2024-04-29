package com.suraj;

import lombok.Getter;
import lombok.Setter;

import java.nio.ByteBuffer;
import java.util.Random;

@Getter
@Setter
public class UdpTrackerConnection {

    private long connId;
    private int action;
    private int transId;

    public UdpTrackerConnection() {
        this.connId = 0x41727101980L;
        this.action = 0;
        this.transId = new Random().nextInt(100000);
    }

    public byte[] toBytes() {
        ByteBuffer buffer = ByteBuffer.allocate(16);
        buffer.putLong(connId);
        buffer.putInt(action);
        buffer.putInt(transId);
        return buffer.array();
    }

    public void fromBytes(byte[] payload) {
        ByteBuffer buffer = ByteBuffer.wrap(payload);
        connId = buffer.getLong();
        action = buffer.getInt();
        transId = buffer.getInt();
    }

}
