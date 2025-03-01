package com.example.crypto.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CipherInfoMassage {
    private byte[] initializationVector;
    private String nameAlgorithm;
    private int sizeKeyInBits;
    private int sizeBlockInBits;
    private String namePadding;
    private String encryptionMode;
    private byte[] p;
    private byte[] g;
    private long roomId;
    private byte[] publicKey;

    private String typeMessage = "cipher_info";
    private long anotherClientId;

    public CipherInfoMassage(CipherInfo cipherInfo, RoomInfo roomInfo, long anotherClientId) {
        this.initializationVector = cipherInfo.getInitializationVector();
        this.nameAlgorithm = cipherInfo.getNameAlgorithm();
        this.sizeKeyInBits = cipherInfo.getSizeKeyInBits();
        this.sizeBlockInBits = cipherInfo.getSizeBlockInBits();
        this.namePadding = cipherInfo.getNamePadding();
        this.encryptionMode = cipherInfo.getEncryptionMode();
        this.p = roomInfo.getP();
        this.g = roomInfo.getG();
        this.roomId = roomInfo.getRoomId();
        this.anotherClientId = anotherClientId;
    }

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
