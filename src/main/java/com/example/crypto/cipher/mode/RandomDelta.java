package com.example.crypto.cipher.mode;

import com.example.crypto.cipher.algoritm.CipherInterface;
import com.example.crypto.cipher.mode.modeInterface.Mode;
import com.example.crypto.cipher.thread.text.TextTaskRunConcrete;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutionException;

@Slf4j
@AllArgsConstructor
public class RandomDelta implements Mode {
    private final CipherInterface cipher;
    private final byte[] vector;

    @Override
    public byte[] encryption(byte[] block) throws IllegalArgumentException, ExecutionException, InterruptedException {
        if (block == null || block.length == 0) {
            throw new IllegalArgumentException("Empty block");
        }
        log.info("Start encryption mode RD");

        return new TextTaskRunConcrete(new ThreadModeRdEncrypt(vector, cipher, block), cipher.getSizeBlock()).threadTextRun(block);
    }

    @Override
    public byte[] decoding(byte[] block) throws IllegalArgumentException, ExecutionException, InterruptedException {
        if (block == null || block.length == 0) {
            throw new IllegalArgumentException("Empty block");
        }
        log.info("Start decoding mode RD");
        return new TextTaskRunConcrete(new ThreadModeRdDecrypt(vector, cipher, block), cipher.getSizeBlock()).threadTextRun(block);
    }
}
