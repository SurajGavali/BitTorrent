package com.suraj;

import lombok.Getter;
import lombok.Setter;

import java.nio.ByteBuffer;
import java.util.Random;

@Getter
@Setter
public class UdpTrackerAnnounce {
    private byte[] infoHash;
    private long connId;
    private String peerId;
    private int transId;
    private int action;

    public UdpTrackerAnnounce(byte[] infoHash, long connId, String peerId) {
        this.infoHash = infoHash;
        this.connId = connId;
        this.peerId = peerId;
        this.transId = new Random().nextInt(100000);
        this.action = 1;
    }

    public byte[] toBytes() {
        ByteBuffer buffer = ByteBuffer.allocate(200);
        buffer.putLong(connId);
        buffer.putInt(action);
        buffer.putInt(transId);
        buffer.put(infoHash);
        buffer.put(peerId.getBytes());
        buffer.putLong(0); // downloaded
        buffer.putLong(0); // left
        buffer.putLong(0); // uploaded
        buffer.putInt(0); // event
        buffer.putInt(0); // IP address
        buffer.putInt(0); // key
        buffer.putInt(-1); // num_want
        buffer.putShort((short) 8000); // port
        return buffer.array();
    }
}
