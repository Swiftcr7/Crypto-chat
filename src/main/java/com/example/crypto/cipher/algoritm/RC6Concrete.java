package com.example.crypto.cipher.algoritm;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import lombok.AllArgsConstructor;

import java.util.Map;

@Slf4j
public class RC6Concrete implements CipherInterface {
    private final int blockSize;
    private final int roundsCount;

    private long[] S;


    public RC6Concrete(int blockSize, int roundsCount, int keySize, byte[] key) {
        log.info("block size {}, rounds count {}, size key {}, key size {}", blockSize, roundsCount, keySize, key.length);
        if (blockSize != 128 && blockSize != 192 && blockSize != 256) {
            throw new IllegalArgumentException("Illegal block size");
        }

        if (roundsCount < 16 || roundsCount > 32) {
            throw new IllegalArgumentException("Illegal count rounds");
        }

        if (keySize != 128 && keySize != 160 && keySize != 192 && keySize != 224 && keySize != 256) {
            throw new IllegalArgumentException("Illegal count rounds");
        }
        log.info(new String(key));

        this.blockSize = blockSize;
        this.roundsCount = roundsCount;
        this.S = new KeyGenerated(blockSize, roundsCount, keySize, key).generateKey();
    }

    @AllArgsConstructor
    public static class KeyGenerated {
        private final int blockSize;
        private final int roundsCount;
        private final int keySize;
        private final byte[] key;
        private final Map<Integer, Pair<Long, Long>> RC6_number = Map.of(
                16, Pair.of(0xB7E1L, 0x9E37L),
                32, Pair.of(0xB7E15163L, 0x9E3779B9L),
                64, Pair.of(0xB7E151628AED2A6BL, 0x9E3779B97F4A7C15L)
        );

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

        private long getWordFromKey(byte[] key, int numByte, int sizeWord) {
            byte[] wordByte = new byte[(sizeWord + Byte.SIZE - 1) / Byte.SIZE];
            for (int i = 0; i < sizeWord; i++) {
                if (i + sizeWord >= Byte.SIZE * key.length) {
                    putBit(wordByte, i / sizeWord, 0);

                } else {
                    int bit = getBit(key, numByte + i);
                    putBit(wordByte, i, bit);

                }
            }
            return transferBytesToLong(wordByte);
        }

        private int getBit(byte[] bytes, int numBit) {
            return (bytes[numBit / Byte.SIZE] >> (Byte.SIZE - (numBit % Byte.SIZE) - 1)) & 1;
        }

        private void putBit(byte[] bytes, int numBit, int value) {
            if (value == 1) {
                bytes[numBit / Byte.SIZE] &= (byte) (1 << (Byte.SIZE - 1 - numBit % Byte.SIZE));
            } else if (value == 0) {
                bytes[numBit / Byte.SIZE] |= (byte) (~(1 << (Byte.SIZE - 1 - numBit % Byte.SIZE)));
            }
        }


        private long[] keyToWords(byte[] key) {
            int wordsSize = blockSize / 4;
            int wordsCount = (keySize - 1 + wordsSize) / wordsSize;
            long[] words = new long[wordsCount];
            for (int i = 0; i < wordsCount; i++) {
                getWordFromKey(key, i * wordsSize, wordsSize);

            }
            return words;
        }

        private long[] SArray() {
            int w = blockSize / 4;
            int count = 2 * (roundsCount + 2);
            long p = RC6_number.get(w).getLeft();
            long q = RC6_number.get(w).getRight();
            long[] Sarray = new long[count];
            Sarray[0] = p;
            for (int i = 1; i < count; i++) {
                Sarray[i] = addModulo(Sarray[i - 1], q, w);
            }
            return Sarray;
        }

        public long[] generateKey() {
            long[] Sarray = SArray();
            long[] words = keyToWords(key);
            int w = blockSize / 4;
            int i = 0, j = 0;
            long A = 0, B = 0;

            for (int k = 0; k < 3 * Math.max(Sarray.length, words.length); k++) {
                A = Sarray[i] = leftCyclicShift(addModulo(Sarray[i], addModulo(A, B, w), w), w, 3);
                B = words[j] = leftCyclicShift(addModulo(words[j], addModulo(A, B, w), w), w, addModulo(A, B, w));
                i = (i + 1) % Sarray.length;
                j = (j + 1) % words.length;
            }

            return Sarray;

        }

    }

    private static long leftCyclicShift(long number, int numBits, long k) {
        long valueShift = Math.abs(k % numBits);
        return (number << valueShift) | ((number & (((1L << valueShift) - 1) << (numBits - valueShift))) >>> (numBits - valueShift));
    }

    private long[] initializeRegisters(byte[] P, int w) {
        long[] registers = new long[4];
        for (int i = 0; i < 4; i++) {
            registers[i] = 0;
            for (int j = 0; j < w / 8; j++) {
                registers[i] |= (P[i * (w / 8) + j] & 0xFFL) << (j * 8);
            }
        }

        return registers;
    }

    private static long addModulo(long first, long second, int numBits) {
        long result = 0;
        long reminder = 0;

        for (int i = 0; i < numBits; i++) {
            long tempSum = ((first >> i) & 1) ^ ((second >> i) & 1) ^ reminder;
            reminder = (((first >> i) & 1) + ((second >> i) & 1) + reminder) >> 1;
            result |= tempSum << i;
        }

        return result;
    }

    private long multiplyModulo(long a, long b, int w) {
        long result = (a * b) & ((1L << w) - 1);
        return result;
    }

    private byte[] convertWordsToBytes(long[] words, int w) {
        int bytesPerWord = w / 8;
        byte[] result = new byte[words.length * bytesPerWord];

        for (int i = 0; i < words.length; i++) {
            long word = words[i];
            for (int j = 0; j < bytesPerWord; j++) {
                result[i * bytesPerWord + j] = (byte) (word >> (j * 8) & 0xFF);
            }
        }

        return result;
    }

    public static long subModulo(long first, long second, int numBits) {
        return addModulo(first, ~second + 1, numBits);
    }


    @Override
    public String getName() {
        return "RC6";
    }

    @Override
    public int getSizeBlock() {
        return blockSize / Byte.SIZE;
    }

    @Override
    public byte[] encryption(byte[] block) {
        long[] registers = initializeRegisters(block, blockSize / 4);
        int w = blockSize / 4;
        registers[1] = addModulo(registers[1], S[0], w);
        registers[3] = addModulo(registers[3], S[1], w);
        long t, u, tempA;
        for (int i = 1; i <= roundsCount; i++) {
            //t = leftCyclicShift(multiplyModulo(registers[1], addModulo(multiplyModulo(2, registers[1], w), 1, w) , w), w, (long) Math.log10(w));
            t = leftCyclicShift(multiplyModulo(registers[1], ((2 * registers[1]) + 1), w), w, (long) Math.log10(w));
            //u = leftCyclicShift(multiplyModulo(registers[3], addModulo(multiplyModulo(2, registers[3], w), 1, w) , w), w, (long) Math.log10(w));
            u = leftCyclicShift(multiplyModulo(registers[3], (registers[3] * 2) + 1, w), w, (long) Math.log10(w));

            registers[0] = addModulo(leftCyclicShift((registers[0] ^ t), w, u), S[2 * i], w);
            registers[2] = addModulo(leftCyclicShift((registers[2] ^ u), w, t), S[(2 * i) + 1], w);
            tempA = registers[0];
            registers[0] = registers[1];  // A = B
            registers[1] = registers[2];  // B = C
            registers[2] = registers[3];  // C = D
            registers[3] = tempA;

        }
        registers[0] = addModulo(registers[0], S[(2 * roundsCount) + 2], w);
        registers[2] = addModulo(registers[2], S[(2 * roundsCount) + 3], w);
//        log.info("Finish encryption RC6");

        return convertWordsToBytes(registers, w);
    }

    private long cyclicRightShift(long number, int numBits, long k) {
        long valueShift = Math.abs(k % numBits);
        return (number >>> valueShift) | ((number & ((1L << valueShift) - 1)) << (numBits - valueShift));
    }

    @Override
    public byte[] decoding(byte[] block) {
//        log.info("Start decoding RC6");
        long[] registers = initializeRegisters(block, blockSize / 4);
        int w = blockSize / 4;
        registers[2] = subModulo(registers[2], S[(2 * roundsCount) + 3], w);
        registers[0] = subModulo(registers[0], S[(2 * roundsCount) + 2], w);
        long t, u, tempA;
        for (int i = roundsCount; i >= 1; i--) {
            tempA = registers[0];
            registers[0] = registers[3];
            registers[3] = registers[2];
            registers[2] = registers[1];
            registers[1] = tempA;

            //u = leftCyclicShift(multiplyModulo(registers[3], addModulo(multiplyModulo(2, registers[3], w), 1, w) , w), w, (long) Math.log10(w));
            u = leftCyclicShift(multiplyModulo(registers[3], (2 * registers[3]) + 1, w), w, (long) Math.log10(w));

            //t = leftCyclicShift(multiplyModulo(registers[1], addModulo(multiplyModulo(2, registers[1], w), 1, w) , w), w, (long) Math.log10(w));
            t = leftCyclicShift(multiplyModulo(registers[1], (2 * registers[1]) + 1, w), w, (long) Math.log10(w));

            registers[2] = (cyclicRightShift(subModulo(registers[2], S[(2 * i) + 1], w), w, t) ^ u);
            registers[0] = (cyclicRightShift(subModulo(registers[0], S[2 * i], w), w, u) ^ t);


        }
        registers[3] = subModulo(registers[3], S[1], w);
        registers[1] = subModulo(registers[1], S[0], w);
//        log.info("Finish decoding RC6");

        return convertWordsToBytes(registers, w);

    }
}
