package com.suraj;

import com.suraj.model.SockAddr;
import lombok.Getter;
import lombok.Setter;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class UdpTrackerAnnounceOutput {
    private int action;
    private int transactionId;
    private int interval;
    private int leechers;
    private int seeders;
    private List<SockAddr> listSockAddr;

    public UdpTrackerAnnounceOutput() {
        this.action = 0;
        this.transactionId = 0;
        this.interval = 0;
        this.leechers = 0;
        this.seeders = 0;
        this.listSockAddr = new ArrayList<SockAddr>();
    }

    public void fromBytes(byte[] payload) {
        ByteBuffer buffer = ByteBuffer.wrap(payload);
        this.action = buffer.getInt();
        this.transactionId = buffer.getInt();
        this.interval = buffer.getInt();
        this.leechers = buffer.getInt();
        this.seeders = buffer.getInt();
        this.listSockAddr = parseSockAddr(payload, 20);
    }

    private List<SockAddr> parseSockAddr(byte[] rawBytes, int startIndex) {
        List<SockAddr> sockAddrList = new ArrayList<>();
        for (int i = startIndex; i < rawBytes.length/6; i += 6) {
            try {
                byte[] ipBytes = new byte[]{rawBytes[i], rawBytes[i + 1], rawBytes[i + 2], rawBytes[i + 3]};
                String ip = InetAddress.getByAddress(ipBytes).getHostAddress();
                int port = ((rawBytes[i + 4] & 0xFF) << 8) | (rawBytes[i + 5] & 0xFF);
                sockAddrList.add(new SockAddr(ip, port,true));
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
        }
        return sockAddrList;
    }
}
