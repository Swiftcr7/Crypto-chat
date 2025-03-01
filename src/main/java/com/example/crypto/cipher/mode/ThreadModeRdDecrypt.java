package com.example.crypto.cipher.mode;

import com.example.crypto.cipher.algoritm.CipherInterface;
import com.example.crypto.cipher.mode.modeInterface.ThreadMode;
import org.apache.commons.lang3.tuple.Pair;

import java.util.concurrent.ExecutionException;

public class ThreadModeRdDecrypt implements ThreadMode {

    private final CipherInterface cipher;
    private final byte[][] counter;

    public ThreadModeRdDecrypt(byte[] vector, CipherInterface cipher, byte[] block) {
        this.cipher = cipher;
        counter = new byte[block.length / cipher.getSizeBlock()][cipher.getSizeBlock()];
        byte[] value = splitInHalf(vector).getRight();
        byte[] oneCounter = cipher.encryption(vector);
        for (int i = 0; i < block.length / cipher.getSizeBlock(); i++) {
            counter[i] = oneCounter;
            oneCounter = getNextCounter(oneCounter, value);
        }
    }

    private static Pair<byte[], byte[]> splitInHalf(byte[] bytes) {
        if (bytes != null) {
            byte[][] splitHalfParts = new byte[2][bytes.length / 2];

            System.arraycopy(bytes, 0, splitHalfParts[0], 0, bytes.length / 2);
            System.arraycopy(bytes, bytes.length / 2, splitHalfParts[1], 0, bytes.length / 2);

            return Pair.of(splitHalfParts[0], splitHalfParts[1]);
        }

        return null;
    }

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

    private byte[] getNextCounter(byte[] counter, byte[] delta) {
        byte[] result = new byte[counter.length];
        byte remind = 0;

        for (int i = 0; i < delta.length; i++) {
            result[i] = (byte) ((counter[counter.length - i - 1] + delta[delta.length - i - 1] + remind) % 256);
            remind = (byte) ((counter[counter.length - i - 1] + delta[delta.length - i - 1]) / 256);
        }

        if (remind != 0) {
            result[result.length - delta.length - 1] = (byte) ((counter[counter.length - delta.length - 1] + remind) % 256);
        }

        return result;
    }

    @Override
    public Pair<Integer, byte[]> run(int countBlock, int blockSize, byte[] array, int index) throws ExecutionException, InterruptedException {
        byte[] encryptionRes = new byte[blockSize * countBlock];
        byte[] block = new byte[blockSize];
        for (int i = 0; i < countBlock; i++) {
            System.arraycopy(array, index + blockSize * i, block, 0, blockSize);
            byte[] encryptBlock = (xor(cipher.decoding(block), counter[(index + i * blockSize) / blockSize]));
            System.arraycopy(encryptBlock, 0, encryptionRes, blockSize * i, blockSize);
        }
        return Pair.of(index, encryptionRes);
    }
}
