package com.example.crypto.cipher.mode;

import com.example.crypto.cipher.algoritm.CipherInterface;
import com.example.crypto.cipher.mode.modeInterface.Mode;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutionException;

@Slf4j
@AllArgsConstructor
public class PropagatingCipherBlockChaining implements Mode {

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
        log.info("Start encryption mode PCBC");

        byte[] decriptyonRes = new byte[block.length];
        byte[] tempolaryBlock = new byte[cipher.getSizeBlock()];
        byte[] previousBlock = new byte[cipher.getSizeBlock()];
        byte[] initVector = vector;
        for (int i = 0; i < block.length; i += cipher.getSizeBlock()) {
            System.arraycopy(block, i, tempolaryBlock, 0, cipher.getSizeBlock());
            initVector = cipher.encryption(xor(xor(tempolaryBlock, previousBlock), initVector));
            previousBlock = tempolaryBlock.clone();
            System.arraycopy(initVector, 0, decriptyonRes, i, cipher.getSizeBlock());
        }
        return decriptyonRes;
    }

    @Override
    public byte[] decoding(byte[] block) throws IllegalArgumentException, ExecutionException, InterruptedException {
        if (block == null || block.length == 0) {
            throw new IllegalArgumentException("Empty block");
        }
        log.info("Start decoding mode PCBC");

        byte[] decriptyonRes = new byte[block.length];
        byte[] tempolaryBlock = new byte[cipher.getSizeBlock()];
        byte[] previousBlock = new byte[cipher.getSizeBlock()];
        byte[] initVector = vector;
        for (int i = 0; i < block.length; i += cipher.getSizeBlock()) {
            System.arraycopy(block, i, tempolaryBlock, 0, cipher.getSizeBlock());
            initVector = xor(xor(cipher.decoding(tempolaryBlock), previousBlock), initVector);
            previousBlock = tempolaryBlock.clone();
            System.arraycopy(initVector, 0, decriptyonRes, i, cipher.getSizeBlock());
        }
        return decriptyonRes;
    }
}
