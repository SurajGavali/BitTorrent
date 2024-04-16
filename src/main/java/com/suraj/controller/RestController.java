package com.suraj.controller;

import com.dampcake.bencode.Bencode;
import com.dampcake.bencode.BencodeInputStream;
import com.dampcake.bencode.Type;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

@org.springframework.web.bind.annotation.RestController
public class RestController {

    Bencode bencode = new Bencode();
    StringBuilder decodedDataString = new StringBuilder();
    Map<String, ?> decodedData;
    @PostMapping(value = "/api/surajtorrent")
    public ResponseEntity<Map<String,?>> getDataFromTorrentFile(@RequestParam("file") MultipartFile file) throws IOException {

        try{
            byte[] fileBytes = file.getBytes();
            decodedData = bencode.decode(fileBytes,Type.DICTIONARY);
        }catch(Exception e){
            e.printStackTrace();
        }

        return new ResponseEntity<>(decodedData, HttpStatus.OK);
    }
}
