package com.example.crypto.cipher.mode;

import com.example.crypto.cipher.algoritm.CipherInterface;
import com.example.crypto.cipher.mode.modeInterface.Mode;
import com.example.crypto.cipher.thread.text.TextTaskRunConcrete;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutionException;

@AllArgsConstructor
@Slf4j
public class CipherBlockChaining implements Mode {
    private final CipherInterface cipher;
    private final byte[] vector;

    private static byte[] xor(byte[] first, byte[] second) {
        int maxLength = Integer.max(first.length, second.length);
        byte[] result = new byte[maxLength];

        for (int i = 0; i < maxLength; i++) {
            byte firstByte = first.length - i - 1 >= 0 ? first[first.length - i - 1] : 0;
            byte secondByte = second.length - i - 1 >= 0 ? second[second.length - i - 1] : 0;
            result[maxLength - i - 1] = (byte) (firstByte ^ secondByte);
        }

        return result;
    }

    @Override
    public byte[] encryption(byte[] block) throws IllegalArgumentException, ExecutionException, InterruptedException {
        if (block == null || block.length == 0) {
            throw new IllegalArgumentException("Empty block");
        }
        log.info("Start encryption mode CBC");
        byte[] encryptionResult = new byte[block.length];
        byte[] InitVector = vector;
        byte[] temporaryBlock = new byte[cipher.getSizeBlock()];
        for (int i = 0; i < block.length; i += cipher.getSizeBlock()) {
            System.arraycopy(block, i, temporaryBlock, 0, cipher.getSizeBlock());
            byte[] pmt = cipher.encryption(xor(temporaryBlock, InitVector));
            System.arraycopy(pmt, 0, encryptionResult, i, cipher.getSizeBlock());
            InitVector = pmt;
        }
        return encryptionResult;
    }

    @Override
    public byte[] decoding(byte[] block) throws IllegalArgumentException, ExecutionException, InterruptedException {
        if (block == null || block.length == 0) {
            throw new IllegalArgumentException("Empty block");
        }
        log.info("Start decoding mode CBC");

        return new TextTaskRunConcrete(new ThreadModeCbcDecrypt(vector, cipher), cipher.getSizeBlock()).threadTextRun(block);
    }
}
