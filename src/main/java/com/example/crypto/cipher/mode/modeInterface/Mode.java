package com.example.crypto.cipher.mode.modeInterface;

import java.util.concurrent.ExecutionException;

public interface Mode {
    public byte[] encryption(byte [] block) throws IllegalArgumentException, ExecutionException, InterruptedException;

    public byte[] decoding(byte[] block) throws IllegalArgumentException, ExecutionException, InterruptedException;
}
