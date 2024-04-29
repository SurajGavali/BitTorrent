package com.suraj.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Info {

    private int length;

    private String name;

    @SerializedName("piece length")
    private int pieceLength;

    private String pieces;

}
