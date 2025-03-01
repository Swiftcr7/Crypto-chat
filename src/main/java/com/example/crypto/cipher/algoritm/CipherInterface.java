package com.example.crypto.cipher.algoritm;

public interface CipherInterface {
    public String getName();

    int getSizeBlock();

    public byte[] encryption(byte[] block);

    public byte[] decoding(byte[] block);
}