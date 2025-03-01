package com.example.crypto.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class KeyMassage {
    private String typeMessage;
    private byte[] publicKey;
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
