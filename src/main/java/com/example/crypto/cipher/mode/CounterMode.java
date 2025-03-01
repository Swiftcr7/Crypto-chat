package com.example.crypto.cipher.mode;

import com.example.crypto.cipher.algoritm.CipherInterface;
import com.example.crypto.cipher.mode.modeInterface.Mode;
import com.example.crypto.cipher.thread.text.TextTaskRunConcrete;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutionException;

@Slf4j
@AllArgsConstructor
public class CounterMode implements Mode {
    private final byte[] vector;

    private final CipherInterface cipher;

    @Override
    public byte[] encryption(byte[] block) throws IllegalArgumentException, ExecutionException, InterruptedException {
        log.info("Start encryption mode CTR");

        return executeAlgorithm(block);
    }

    @Override
    public byte[] decoding(byte[] block) throws IllegalArgumentException, ExecutionException, InterruptedException {
        log.info("Start decoding mode CTR");

        return executeAlgorithm(block);
    }

    private byte[] executeAlgorithm(byte[] block) throws ExecutionException, InterruptedException {
        if (block == null || block.length == 0) {
            throw new IllegalArgumentException("Empty block");
        }
        return new TextTaskRunConcrete(new ThreadModeCtr(vector, block, cipher), cipher.getSizeBlock()).threadTextRun(block);
    }
}
