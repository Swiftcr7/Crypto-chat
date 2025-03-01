package com.example.crypto.cipher.mode;

import com.example.crypto.cipher.algoritm.CipherInterface;
import com.example.crypto.cipher.mode.modeInterface.ThreadMode;
import org.apache.commons.lang3.tuple.Pair;

import java.util.concurrent.ExecutionException;

public class ThreadModeOfb implements ThreadMode {
    private final byte[][] arrayEncryptionBlock;

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


    public ThreadModeOfb(byte[] vector, byte[] block, CipherInterface cipher) {
        arrayEncryptionBlock = new byte[block.length / cipher.getSizeBlock()][];
        byte[] temporary = vector;
        for (int i = 0; i < block.length / cipher.getSizeBlock(); i++) {
            arrayEncryptionBlock[i] = temporary.clone();
            temporary = cipher.encryption(temporary);
        }
    }

    @Override
    public Pair<Integer, byte[]> run(int countBlock, int blockSize, byte[] array, int index) throws ExecutionException, InterruptedException {
        byte[] encryptionRes = new byte[blockSize * countBlock];
        byte[] block = new byte[blockSize];
        for (int i = 0; i < countBlock; i++) {
            System.arraycopy(array, index + blockSize * i, block, 0, blockSize);
            byte[] encryptBlock = xor(block, arrayEncryptionBlock[(index + i * blockSize) / blockSize]);
            System.arraycopy(encryptBlock, 0, encryptionRes, blockSize * i, blockSize);
        }
        return Pair.of(index, encryptionRes);
    }
}
