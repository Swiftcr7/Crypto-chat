package com.example.crypto.cipher.mode;

import com.example.crypto.cipher.algoritm.CipherInterface;
import com.example.crypto.cipher.mode.modeInterface.ThreadMode;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;

import java.util.concurrent.ExecutionException;

@AllArgsConstructor
public class ThreadModeCbcDecrypt implements ThreadMode {
    private final byte[] vector;

    private final CipherInterface cipher;

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
    public Pair<Integer, byte[]> run(int countBlock, int blockSize, byte[] array, int index) throws ExecutionException, InterruptedException {
        byte[] initVector;
        if (index == 0) {
            initVector = vector;
        } else {
            initVector = new byte[blockSize];
            System.arraycopy(array, index - blockSize, initVector, 0, blockSize);
        }
        byte[] decryptionResult = new byte[countBlock * blockSize];
        byte[] block = new byte[blockSize];
        for (int i = 0; i < countBlock; i++) {
            System.arraycopy(array, index + i * blockSize, block, 0, blockSize);
            byte[] temporary = xor(initVector, cipher.decoding(block));
            initVector = block.clone();
            System.arraycopy(temporary, 0, decryptionResult, i * blockSize, blockSize);
        }
        return Pair.of(index, decryptionResult);
    }
}
