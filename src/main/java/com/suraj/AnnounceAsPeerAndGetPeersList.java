package com.suraj;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

@Service
public class AnnounceAsPeerAndGetPeersList {

    RestTemplate restTemplate = new RestTemplate();
    public Object getPeers(Map<String, Object> decodedData) throws UnsupportedEncodingException, NoSuchAlgorithmException {

        Object announceUrl = decodedData.get("announce");
        System.out.println("announceURL :: {}"+announceUrl);

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

        String infoHash = hexString.toString();

        String port = "6969";
        String peerId = "TR2940-k8hj0wgej6ch";

        String finalUrl = announceUrl + "?info_hash=" + infoHash +
                "&peer_id=" + peerId +
                "&port=" + port +
                "&uploaded=0" +
                "&downloaded=0" +
                "&left=0" +
                "&compact=1";
        System.out.println(finalUrl);
        Object dataFromAnnounceUrl = restTemplate.getForEntity(finalUrl,String.class);
        System.out.println(dataFromAnnounceUrl);




        return null;
    }
}
