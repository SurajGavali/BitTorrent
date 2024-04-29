package com.suraj;
import com.dampcake.bencode.Bencode;
import com.dampcake.bencode.Type;
import com.suraj.model.DecodedData;
import com.suraj.model.FileInfo;
import com.suraj.model.SockAddr;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.*;
import java.net.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AnnounceAsPeerAndGetPeersList {

    RestTemplate restTemplate = new RestTemplateBuilder().setReadTimeout(Duration.ofMillis(10000)).build();
    Bencode bencode = new Bencode();

    Map<Integer,SockAddr> dictSockAddr = new HashMap<>();
    int MAX_PEERS_CONNECTED = 8;
    int MAX_PEERS_TRY_CONNECT = 30;

    public Object getPeers(DecodedData decodedData) throws UnsupportedEncodingException, NoSuchAlgorithmException, InterruptedException {

        List<String> announceUrlList = new ArrayList<>();
        int pieceLength = decodedData.getInfo().getPieceLength();
        String pieces = decodedData.getInfo().getPieces();
        byte[] rawInfoHash = bencode.encode(decodedData.getInfo().toString());

        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        digest.update(rawInfoHash);

        byte[] infoHash = digest.digest();
        String peerId = generatePeerId();
        String root = decodedData.getInfo().getName();
        announceUrlList.add(decodedData.getAnnounce());
        if (decodedData.getAnnounceList() != null && !decodedData.getAnnounceList().isEmpty()) {
            List<List<String>> announceList = decodedData.getAnnounceList();
            for (List<String> urlList : announceList) {
                for (String url : urlList) {
                    announceUrlList.add(url);
                }
            }
        }

        int totalLength = decodedData.getInfo().getLength();
        List<FileInfo> fileNames = new ArrayList<>();
        FileInfo fileInfo = new FileInfo();
        fileInfo.setPath(decodedData.getInfo().getName());
        fileInfo.setLength(decodedData.getInfo().getLength());

        fileNames.add(fileInfo);

        long numPieces = (long) Math.ceil((double) totalLength / pieceLength);



        for (String trackerUrl : announceUrlList) {


            try {
                if (trackerUrl.startsWith("http")) {
                    getDataFromhttp(decodedData, trackerUrl);
                }
                if (trackerUrl.startsWith("udp")) {
                    getDataFromUdp(trackerUrl, infoHash,peerId);
                }
            } catch (Exception e) {
                System.out.println(e);
//                e.printStackTrace();
                continue;
            }
        }
        return null;
    }

    public void getDataFromhttp(DecodedData decodedData, String trackerUrl) throws UnsupportedEncodingException, NoSuchAlgorithmException {



        int port = getPortFromUrl(trackerUrl);
        String peerId = generatePeerId();

        String infoHash = createInfoHash(decodedData);

        String finalUrl = trackerUrl + "?info_hash=" + infoHash +
                "&peer_id=" + peerId +
                "&port=" + "6881" +
                "&uploaded=0" +
                "&downloaded=0" +
                "&left=0" +
                "&compact=1";
        System.out.println(finalUrl);

        try {
            URL url = new URL(trackerUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                byte[] content = connection.getInputStream().readAllBytes();
                Map<String, Object> listPeers = bencode.decode(content,Type.DICTIONARY);

                System.out.println("List ofe peers :: "+listPeers);
                for (Map<String, Object> peer : (Iterable<Map<String, Object>>) listPeers.get("peers")) {
                    String ip = (String) peer.get("ip");
                    int p = (int) peer.get("port");
                    SockAddr sockAddr = new SockAddr(ip, p,true);
                    dictSockAddr.put(sockAddr.hashCode(), sockAddr);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("HTTP scraping failed: " + e.getMessage());
        }

    }

    public void getDataFromUdp(String trackerUrl, byte[] infoHash,String peerId) throws UnsupportedEncodingException, NoSuchAlgorithmException {

        int trackerPort = getPortFromUrl(trackerUrl);

        try {
            // Create a UDP socket
            DatagramSocket socket = new DatagramSocket();

            String hostName = extractDomain(trackerUrl);
            // Send the request
            InetAddress trackerAddress = InetAddress.getByName(hostName);
            socket.setSoTimeout(4000);

            if (trackerAddress.isSiteLocalAddress()) {
                return;
            }

            UdpTrackerConnection trackerConnectionInput = new UdpTrackerConnection();
            byte[] responseUdpTrackerConnection = sendMessageUdpTrackerConnection(trackerAddress, trackerPort, socket, trackerConnectionInput);

            if (responseUdpTrackerConnection == null) {
                throw new IOException("No response for UdpTrackerConnection");
            }

            UdpTrackerConnection trackerConnectionOutput = new UdpTrackerConnection();
            trackerConnectionOutput.fromBytes(responseUdpTrackerConnection);


            UdpTrackerAnnounce trackerAnnounceInput = new UdpTrackerAnnounce(infoHash, trackerConnectionOutput.getConnId(), peerId);
            System.out.println("trackerAnnounceInput :: " +trackerAnnounceInput);
            byte[] responseUdpTrackerAnnounce = sendMessageUdpTrackerConnection(trackerAddress, trackerPort, socket, trackerAnnounceInput);

            System.out.println("responseUdpTrackerAnnounce :: "+responseUdpTrackerAnnounce);
            if (responseUdpTrackerAnnounce == null) {
                throw new IOException("No response for UdpTrackerAnnounce");
            }

            UdpTrackerAnnounceOutput trackerAnnounceOutput = new UdpTrackerAnnounceOutput();
            trackerAnnounceOutput.fromBytes(responseUdpTrackerAnnounce);


            System.out.println(trackerAnnounceOutput.getListSockAddr());

            for (SockAddr sockAddr : trackerAnnounceOutput.getListSockAddr()) {
                if (!dictSockAddr.containsKey(sockAddr.hashCode())) {
                    dictSockAddr.put(sockAddr.hashCode(), sockAddr);
                }
            }

            System.out.println("Got " + dictSockAddr.size() + " peers");
            System.out.println("dictSockAddr :: "+dictSockAddr);


        } catch (SocketException e) {
            throw new RuntimeException(e);
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

//    public void tryPeerConnect() {
//        System.out.println("Trying to connect to " + dictSockAddr.size() + " peer(s)");
//
//        for (SockAddr sockAddr : dictSockAddr.values()) {
//            if (connectedPeers.size() >= MAX_PEERS_CONNECTED) {
//                break;
//            }
//
//            Peer newPeer = new Peer(torrent.getNumberOfPieces(), sockAddr.getIp(), sockAddr.getPort());
//            if (newPeer.connect()) {
//                System.out.println("Connected to " + connectedPeers.size() + "/" + MAX_PEERS_CONNECTED + " peers");
//                connectedPeers.put(newPeer.hashCode(), newPeer);
//            }
//        }
//    }
    public int getPortFromUrl(String trackerUrl){

        String numberStr = null;

        String numberRegex = "\\d+"; // Match one or more digits
        Pattern pattern = Pattern.compile(numberRegex);
        Matcher matcher = pattern.matcher(trackerUrl);

        while (matcher.find()) {
            numberStr = matcher.group();
        }
        return Integer.parseInt(numberStr);
    }

    public String createInfoHash(DecodedData decodedData) throws UnsupportedEncodingException, NoSuchAlgorithmException {

        byte[] data = decodedData.getInfo().toString().getBytes("UTF-8");
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        digest.update(data);

        byte[] hashBytes = digest.digest();

        StringBuilder hexString = new StringBuilder();

        for(byte b : hashBytes){

            String hex = Integer.toHexString(0xff & b);
            if(hex.length() == 1){
                hexString.append('0');
            }

            hexString.append(hex);

        }

        return hexString.toString();
    }

    public String extractDomain(String url) {
        // Regular expression pattern to match domain
        Pattern pattern = Pattern.compile("udp://([^:/]+)");
        Matcher matcher = pattern.matcher(url);
        if (matcher.find()) {
            return matcher.group(1);
        } else {
            return null;
        }
    }

    public String generatePeerId() {
        String seed = String.valueOf(System.currentTimeMillis() / 1000L);
        byte[] sha1Digest = sha1(seed.getBytes(StandardCharsets.UTF_8));
        return bytesToHex(sha1Digest);
    }

    public static byte[] sha1(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            return digest.digest(data);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    public byte[] sendMessageUdpTrackerConnection(InetAddress ip, int port, DatagramSocket sock, UdpTrackerConnection trackerMessage) throws IOException {
        byte[] message = trackerMessage.toBytes();
        int transId = trackerMessage.getTransId();
        int action = trackerMessage.getAction();
        int size = message.length;

        DatagramPacket sendPacket = new DatagramPacket(message, size, ip, port);
        sock.send(sendPacket);

        byte[] response = readFromSocket(sock);

        System.out.println("response size ::"+response.length);
        if (response.length < size) {
            System.out.println("Did not get full message.");
        }

        ByteBuffer responseBuffer = ByteBuffer.wrap(response);

        if (action != responseBuffer.getInt(0) || transId != responseBuffer.getInt(4)) {
            System.out.println("Transaction or Action ID did not match");
        }

        return response;
    }

    public byte[] sendMessageUdpTrackerConnection(InetAddress ip, int port, DatagramSocket sock, UdpTrackerAnnounce trackerMessage) throws IOException {
        byte[] message = trackerMessage.toBytes();
        int transId = trackerMessage.getTransId();
        int action = trackerMessage.getAction();
        int size = message.length;

        DatagramPacket sendPacket = new DatagramPacket(message, size, ip, port);
        sock.send(sendPacket);

        byte[] response = readFromSocket(sock);

        if (response.length < size) {
            System.out.println("Did not get full message.");
        }

        ByteBuffer responseBuffer = ByteBuffer.wrap(response);

        if (action != responseBuffer.getInt(0) || transId != responseBuffer.getInt(4)) {
            System.out.println("Transaction or Action ID did not match");
        }

        return response;
    }

    public byte[] readFromSocket(DatagramSocket socket) throws IOException {
        byte[] data = new byte[0];

        while (true) {
            byte[] buffer = new byte[4096];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            try {
                socket.receive(packet);
                byte[] receivedData = packet.getData();
                if (receivedData.length <= 0) {
                    break;
                }
                byte[] newData = new byte[data.length + receivedData.length];
                System.arraycopy(data, 0, newData, 0, data.length);
                System.arraycopy(receivedData, 0, newData, data.length, receivedData.length);
                data = newData;
            } catch (SocketTimeoutException e) {
                // Socket timeout occurred, handle as needed
                break;
            } catch (IOException e) {
                // Handle other IOExceptions
                e.printStackTrace();
                break;
            }
        }

        return data;
    }
}