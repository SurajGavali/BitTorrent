package com.suraj.controller;

import com.dampcake.bencode.Bencode;
import com.dampcake.bencode.Type;
import com.google.gson.Gson;
import com.suraj.AnnounceAsPeerAndGetPeersList;
import com.suraj.model.DecodedData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.Map;

@org.springframework.web.bind.annotation.RestController
public class RestController {

    Bencode bencode = new Bencode();
    StringBuilder decodedDataString = new StringBuilder();
    @Autowired
    AnnounceAsPeerAndGetPeersList announceAsPeerAndGetPeersList;
    @Autowired Gson gson;
    @PostMapping(value = "/api/surajtorrent")
    public ResponseEntity<DecodedData> getDataFromTorrentFile(@RequestParam("file") MultipartFile file) throws IOException {

        DecodedData decodedData = new DecodedData();
        try{
            byte[] fileBytes = file.getBytes();

            Map<String, Object> decodedDataDictionary = bencode.decode(fileBytes,Type.DICTIONARY);
            System.out.println(decodedDataDictionary);



            decodedData = gson.fromJson(gson.toJsonTree(decodedDataDictionary),DecodedData.class);
            System.out.println(gson.toJson(decodedData));


            announceAsPeerAndGetPeersList.getPeers(decodedData);
        }catch(Exception e){
            e.printStackTrace();
        }

        return new ResponseEntity<>(decodedData, HttpStatus.OK);
    }
}
