package com.example.crypto.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Message {
    private String typeMessage;
    private String typeFormat;
    private String fileName;
    private int indexMessage;
    private byte[] bytes;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public byte[] convertToBytes() {
        try {
            return objectMapper.writeValueAsString(this).getBytes();
        } catch (JsonProcessingException e) {
            log.error("Error with json covert byte");
        }
        return new byte[0];
    }

    public String convertToString() {
        try {
            return objectMapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            log.error("Error with json covert byte");
        }
        return "";
    }
}
