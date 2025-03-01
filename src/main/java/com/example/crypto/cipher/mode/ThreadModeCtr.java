package com.example.crypto.cipher.mode;

import com.example.crypto.cipher.algoritm.CipherInterface;
import com.example.crypto.cipher.mode.modeInterface.ThreadMode;
import org.apache.commons.lang3.tuple.Pair;

import java.util.concurrent.ExecutionException;

public class ThreadModeCtr implements ThreadMode {
    private final CipherInterface cipher;
    private final byte[][] arrayEncryptionBlock;

    private long transferBytesToLong(byte[] bytes) {
        long res = 0;
        for (byte b : bytes) {
            long b_long = transferOneByteToLong(b);
            res = (res << Byte.SIZE) | b_long;

        }
        return res;
    }

    private long transferOneByteToLong(byte b) {
        int valueByte = (b >> (Byte.SIZE - 1)) & 1;
        long res = b & ((1 << (Byte.SIZE - 1)) - 1);
        if (valueByte == 1) {
            res |= (long) (1 << (Byte.SIZE - 1));
        }
        return res;
    }

    private static byte[] longToBytes(long number, int countBytes) {
        byte[] result = new byte[countBytes];

        for (int i = countBytes - 1; i >= 0; i--) {
            result[i] = (byte) (number & ((1 << Long.BYTES) - 1));
            number >>= Byte.SIZE;
        }

        return result;
    }

    private static byte[] mergePart(byte[] left, byte[] right) {
        if (left != null && right != null) {
            byte[] result = new byte[left.length + right.length];

            System.arraycopy(left, 0, result, 0, left.length);
            System.arraycopy(right, 0, result, left.length, right.length);

            return result;
        }

        return new byte[0];
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


    public ThreadModeCtr(byte[] vector, byte[] block, CipherInterface cipher) {
        this.cipher = cipher;
        arrayEncryptionBlock = new byte[block.length / cipher.getSizeBlock()][cipher.getSizeBlock()];
        byte[] temporary = vector.clone();
        for (int i = 0; i < block.length / cipher.getSizeBlock(); i++) {
            arrayEncryptionBlock[i] = temporary.clone();
            temporary = increment(temporary);
        }
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

    private byte[] increment(byte[] value) {
        Pair<byte[], byte[]> twoPart = splitInHalf(value);
        byte[] partRight = longToBytes(transferBytesToLong(twoPart.getRight()) + 1, twoPart.getRight().length);
        return mergePart(twoPart.getLeft(), partRight);
    }

    @Override
    public Pair<Integer, byte[]> run(int countBlock, int blockSize, byte[] array, int index) throws ExecutionException, InterruptedException {
        byte[] encryptionRes = new byte[blockSize * countBlock];
        byte[] block = new byte[blockSize];
        for (int i = 0; i < countBlock; i++) {
            System.arraycopy(array, index + blockSize * i, block, 0, blockSize);
            byte[] encryptBlock = xor(block, cipher.encryption(arrayEncryptionBlock[(index + i * blockSize) / blockSize]));
            System.arraycopy(encryptBlock, 0, encryptionRes, blockSize * i, blockSize);
        }
        return Pair.of(index, encryptionRes);
    }
}
