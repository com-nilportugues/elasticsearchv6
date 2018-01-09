package com.nilportugues.elasticsearchv6;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.nilportugues.elasticsearch.annotations.Id;

import java.io.Serializable;

@JsonSerialize
public class TestUser implements Serializable {

    @Id
    @JsonProperty(value = "id")
    private String id;

    @JsonProperty(value = "username")
    private String username;

    public void setId(String id) {
        this.id = id;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }
}
