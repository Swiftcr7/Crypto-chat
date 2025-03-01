package com.example.crypto.cipher.mode;

import com.example.crypto.cipher.algoritm.CipherInterface;
import com.example.crypto.cipher.mode.modeInterface.Mode;
import com.example.crypto.cipher.thread.text.TextTaskRun;
import com.example.crypto.cipher.thread.text.TextTaskRunConcrete;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutionException;

@Slf4j
@AllArgsConstructor
public class ElectronicCodebook implements Mode {
    private final CipherInterface cipher;

    @Override
    public byte[] encryption(byte[] block) throws IllegalArgumentException, ExecutionException, InterruptedException {
        if (block == null || block.length == 0) {
            throw new IllegalArgumentException("Empty block");
        }
        log.info("Start encryption mode ECB");

        return new TextTaskRunConcrete(new ThreadModeEcbEncrypt(cipher), cipher.getSizeBlock()).threadTextRun(block);
    }

    @Override
    public byte[] decoding(byte[] block) throws IllegalArgumentException, ExecutionException, InterruptedException {
        if (block == null || block.length == 0) {
            throw new IllegalArgumentException("Empty block");
        }
        log.info("Start decoding mode ECB");

        return new TextTaskRunConcrete(new ThreadModeEcbDecrypt(cipher), cipher.getSizeBlock()).threadTextRun(block);
    }
}
