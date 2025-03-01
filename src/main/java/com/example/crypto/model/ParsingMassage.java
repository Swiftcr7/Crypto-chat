package com.example.crypto.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ParsingMassage {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private ParsingMassage() {
    }

    public static Message parsingMessage(String message) {
        try {
            return objectMapper.readValue(message, Message.class);
        } catch (JsonProcessingException e) {
            log.error("Error parsing message");
        }
        return null;
    }
}
