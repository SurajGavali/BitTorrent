package com.suraj;
import com.dampcake.bencode.Bencode;
import com.dampcake.bencode.Type;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.net.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AnnounceAsPeerAndGetPeersList {

    RestTemplate restTemplate = new RestTemplate();
    Bencode bencode = new Bencode();

    public Object getPeers(Map<String, Object> decodedData) throws UnsupportedEncodingException, NoSuchAlgorithmException {

        try {

            List<String> announceUrlList = new ArrayList<>();

            announceUrlList.add(decodedData.get("announce").toString());

            if (decodedData.get("announce-list") != null) {
                List<List<String>> announceList = (List<List<String>>) decodedData.get("announce-list");
                for (List<String> urlList : announceList) {
                    for (String url : urlList) {
                        announceUrlList.add(url);
                    }
                }
            }

            //System.out.println("announceURL :: " + announceUrlList);

            for (String trackerUrl : announceUrlList) {


                if (trackerUrl.startsWith("http")) {
                    getDataFromhttp(decodedData,trackerUrl);
                }

                if (trackerUrl.startsWith("udp")) {
                    getDataFromUdp(trackerUrl,decodedData);
                }
            }
        } catch (Exception e) {

        }
        return null;
    }

    public void getDataFromhttp(Map<String, Object> decodedData, String trackerUrl) throws UnsupportedEncodingException, NoSuchAlgorithmException {



        int port = getPortFromUrl(trackerUrl);
        String peerId = "TR2940-k8hj0wgej6ch";

        String infoHash = createInfoHash(decodedData);

        String finalUrl = trackerUrl + "?info_hash=" + infoHash +
                "&peer_id=" + peerId +
                "&port=" + port +
                "&uploaded=0" +
                "&downloaded=0" +
                "&left=0" +
                "&compact=1";
        System.out.println(finalUrl);

        ResponseEntity<String> dataFromAnnounceUrl = restTemplate.getForEntity(finalUrl, String.class);
        System.out.println(dataFromAnnounceUrl);

        if (dataFromAnnounceUrl.getStatusCode().equals(HttpStatus.OK)) {

            System.out.println("Data from announce URL :: " + dataFromAnnounceUrl.getBody());

            decodedData = bencode.decode(dataFromAnnounceUrl.getBody().getBytes(), Type.DICTIONARY);

            System.out.println(decodedData);

        } else {
            System.out.println("Failed with http status code :: " + dataFromAnnounceUrl.getStatusCode());
        }

    }

    public void getDataFromUdp(String trackerUrl, Map<String, Object> decodedData) throws UnsupportedEncodingException, NoSuchAlgorithmException {
        System.out.println("Doing UDP call");


        int trackerPort = getPortFromUrl(trackerUrl);

        // Generate a random peer ID
        String peerId = generatePeerId();

        String infoHash = createInfoHash(decodedData);

        // Prepare the request data
        byte[] requestData = prepareRequest(infoHash, peerId);

        try {
            // Create a UDP socket
            DatagramSocket socket = new DatagramSocket();

            // Send the request
            InetAddress trackerAddress = InetAddress.getByName(trackerUrl.substring(6)); // Remove "udp://" prefix
            DatagramPacket requestPacket = new DatagramPacket(requestData, requestData.length, trackerAddress, trackerPort);
//            System.out.println(requestPacket);
            socket.send(requestPacket);

            // Receive the response
            byte[] responseData = new byte[1024]; // Adjust size as needed
            DatagramPacket responsePacket = new DatagramPacket(responseData, responseData.length);
            socket.receive(responsePacket);

            // Process the response
            String response = new String(responsePacket.getData(), 0, responsePacket.getLength());
            System.out.println("Tracker Response: " + response);

            // Close the socket
            socket.close();
        } catch (SocketException e) {
            throw new RuntimeException(e);
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private byte[] prepareRequest(String infoHash, String peerId) {
        ByteBuffer buffer = ByteBuffer.allocate(98);
        buffer.putLong(0L); // Connection ID (0 for initial connection)
        buffer.putInt(0); // Action (0 for "connect")
        buffer.putInt((int) System.currentTimeMillis()); // Transaction ID

        // Info Hash
        try {
            byte[] infoHashBytes = hexStringToByteArray(infoHash);
            buffer.put(infoHashBytes);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }

        // Peer ID
        buffer.put(peerId.getBytes());

        return buffer.array();
    }

    private String generatePeerId() {
        // A simple method to generate a random peer ID
        // You can replace this with a more sophisticated approach if needed
        return "PEERID1234567890";
    }

    private byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

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

    public String createInfoHash(Map<String, Object> decodedData) throws UnsupportedEncodingException, NoSuchAlgorithmException {

        byte[] data = decodedData.get("info").toString().getBytes("UTF-8");
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
}
