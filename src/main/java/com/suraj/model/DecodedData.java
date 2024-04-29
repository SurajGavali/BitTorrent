package com.suraj.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;

import java.util.List;


@Getter
@Setter
public class DecodedData {

    private String announce;

    @SerializedName("announce-list")
    private List<List<String>> announceList;

    private String comment;

    @SerializedName("created by")
    private String createdBy;

    @SerializedName("creation date")
    private Double creationDate;

    private Info info;
}
