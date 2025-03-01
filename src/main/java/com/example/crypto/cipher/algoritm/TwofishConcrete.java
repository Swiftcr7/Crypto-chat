package com.example.crypto.cipher.algoritm;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TwofishConcrete implements CipherInterface {
    private final int blockSize = 128;

    private final long[] boxS;

    private final long[] roundKey;

    private static final int irreduciblePolynomial = 0x11B;

    private static final long[][] t_for_q0 = {
            {0x8, 0x1, 0x7, 0xD, 0x6, 0xF, 0x3, 0x2, 0x0, 0xB, 0x5, 0x9, 0xE, 0xC, 0xA, 0x4},  //t0
            {0xE, 0xC, 0xB, 0x8, 0x1, 0x2, 0x3, 0x5, 0xF, 0x4, 0xA, 0x6, 0x7, 0x0, 0x9, 0xD},  //t1
            {0xB, 0xA, 0x5, 0xE, 0x6, 0xD, 0x9, 0x0, 0xC, 0x8, 0xF, 0x3, 0x2, 0x4, 0x7, 0x1},  //t2
            {0xD, 0x7, 0xF, 0x4, 0x1, 0x2, 0x6, 0xE, 0x9, 0xB, 0x3, 0x0, 0x8, 0x5, 0xC, 0xA}   // t3
    };

    private static final long[][] t_for_q1 = {
            {0x2, 0x8, 0xB, 0xD, 0xF, 0x7, 0x6, 0xE, 0x3, 0x1, 0x9, 0x4, 0x0, 0xA, 0xC, 0x5},  //t0
            {0x1, 0xE, 0x2, 0xB, 0x4, 0xC, 0x3, 0x7, 0x6, 0xD, 0xA, 0x5, 0xF, 0x9, 0x0, 0x8},  //t1
            {0x4, 0xC, 0x7, 0x5, 0x1, 0x6, 0x9, 0xA, 0x0, 0xE, 0xD, 0x8, 0x2, 0xB, 0x3, 0xF},  //t2
            {0xB, 0x9, 0x5, 0x1, 0xC, 0x3, 0xD, 0xE, 0x6, 0x4, 0x7, 0xF, 0x2, 0x0, 0x8, 0xA} // t3
    };

    private static final long[][] MDS = {
            {0x01, 0xEF, 0x5B, 0x5B},
            {0x5B, 0xEF, 0xEF, 0x01},
            {0xEF, 0x5B, 0x01, 0xEF},
            {0xEF, 0x01, 0xEF, 0x5B}
    };


    public TwofishConcrete(byte[] key, int keySize) {
        if (keySize != 128 && keySize != 198 && keySize != 256) {
            throw new IllegalArgumentException("incorrect key size");
        }

        boxS = new keyGenerator(key).generateSboxes();
        roundKey = new keyGenerator(key).keyGenerate();

    }

    private static long addModulo(long a, long b, int bitLength) {
        long result = 0;
        long remainder = 0;

        for (int i = 0; i < bitLength; i++) {

            //Сумма с учётом переноса с прошлого разряда
            long temp = ((a >> i) & 1) ^ ((b >> i) & 1) ^ remainder;
            remainder = (((a >> i) & 1) + ((b >> i) & 1) + remainder) >> 1;

            result |= temp << i;
        }

        return result;
    }

    private long g(long value, long[] matrix) {
        long[] words = new long[4];
        for (int i = 0; i < 4; i++) {
            words[i] = (value >>> (8 * (3 - i))) & 0xFF;
        }
        for (int i = matrix.length - 1; i > -1; i--) {
            if (i == 3) {
                words[0] = q(words[0], t_for_q1) ^ matrix[i];
                words[1] = q(words[1], t_for_q0) ^ matrix[i];
                words[2] = q(words[2], t_for_q0) ^ matrix[i];
                words[3] = q(words[3], t_for_q1) ^ matrix[i];
            } else if (i == 2) {
                words[0] = q(words[0], t_for_q1) ^ matrix[i];
                words[1] = q(words[1], t_for_q1) ^ matrix[i];
                words[2] = q(words[2], t_for_q0) ^ matrix[i];
                words[3] = q(words[3], t_for_q0) ^ matrix[i];
            } else if (i == 1) {
                words[0] = q(words[0], t_for_q0) ^ matrix[i];
                words[1] = q(words[1], t_for_q1) ^ matrix[i];
                words[2] = q(words[2], t_for_q0) ^ matrix[i];
                words[3] = q(words[3], t_for_q1) ^ matrix[i];
            } else if (i == 0) {
                words[0] = q(q(words[0], t_for_q0) ^ matrix[i], t_for_q1);
                words[1] = q(q(words[1], t_for_q0) ^ matrix[i], t_for_q0);
                words[2] = q(q(words[2], t_for_q1) ^ matrix[i], t_for_q1);
                words[3] = q(q(words[3], t_for_q1) ^ matrix[i], t_for_q0);

            }
        }
        long[] z = multiplicationByMDS(words);
        return (z[0] & 0xFF) |
                ((z[1] & 0xFF) << 8) |
                ((z[2] & 0xFF) << 16) |
                ((z[3] & 0xFF) << 24);
    }

    private static byte transferLongToByte(long value) {
        return (byte) (value & 0xFF);
    }

    private static byte multiplicationInGaloisField(byte a, byte b) {
        byte p = 0;
        byte carry;
        for (int i = 0; i < 8; i++) {
            if ((b & 1) != 0) {
                p ^= a;
            }
            carry = (byte) (a & 0x80);
            a <<= 1;
            if (carry != 0) {
                a ^= irreduciblePolynomial;
            }
            b >>= 1;
        }
        return p;

    }

    private static long[] multiplicationByMDS(long[] array) {
        long[] result = new long[4];
        for (int i = 0; i < 4; i++) {
            result[i] = 0;
            for (int j = 0; j < 4; j++) {
                byte mdsValue = transferLongToByte(MDS[i][j]);
                byte one = transferLongToByte(array[i]);
                result[i] ^= multiplicationInGaloisField(mdsValue, one);

            }
        }
        return result;
    }

    private static long transferBytesToLong(byte[] bytes) {
        long res = 0;
        for (byte b : bytes) {
            long b_long = transferOneByteToLong(b);
            res = (res << Byte.SIZE) | b_long;

        }
        return res;
    }


    private static long rightCyclicShift(long num, int bitLength, long shift) {
        long controlCycle = shift % bitLength;
        if (controlCycle < 0) {
            controlCycle += bitLength;
        }
        long mask = (1L << controlCycle) - 1;
        long extractedBits = (num & mask) << (bitLength - controlCycle);
        return (num >>> controlCycle) | extractedBits;
    }

    private static long leftCyclicShift(long num, int bitLength, long shift) {
        long controlCycle = shift % bitLength;
        if (controlCycle < 0) {
            controlCycle += bitLength;
        }
        long mask = (1L << controlCycle) - 1;
        long extractedBits = (num >>> (bitLength - controlCycle)) & mask;
        return ((num << controlCycle) | extractedBits) & ((1L << bitLength) - 1);
    }


    private static long transferOneByteToLong(byte b) {
        int valueByte = (b >> (Byte.SIZE - 1)) & 1;
        long res = b & ((1 << (Byte.SIZE - 1)) - 1);
        if (valueByte == 1) {
            res |= (long) (1 << (Byte.SIZE - 1));
        }
        return res;
    }

    private static long q(long value, long[][] t) {
        long[] pmt = divisionInto4(value);
        long a0 = pmt[0];
        long b0 = pmt[1];
        long a1 = a0 ^ b0;
        long b1 = a0 ^ (rightCyclicShift(b0, 4, 1)) ^ ((a0 << 3) % 16);
        long a2 = t[0][(int) a1];
        long b2 = t[1][(int) b1];
        long a3 = a2 ^ b2;
        long b3 = a2 ^ (rightCyclicShift(b2, 4, 1)) ^ ((a2 << 3) % 16);
        long a4 = t[2][(int) a3];
        long b4 = t[3][(int) b3];
        return (b4 << 4) | a4;

    }

    private static long[] divisionInto4(long value) {
        value = value & 0xFF;
        long highBits = (value >> 4) & 0xF;
        long lowBits = value & 0xF;
        long[] result = new long[2];
        result[0] = highBits;
        result[1] = lowBits;
        return result;
    }


    @AllArgsConstructor
    public static class keyGenerator {
        private final byte[] key;

        private final int irreduciblePolynomial = 0x11B;

        private final long MOD_2_32 = 0xFFFFFFFF;

        private static final long[][] RS_MATRIX = {
                {0x01, 0xA4, 0x55, 0x87, 0x5A, 0x58, 0xDB, 0x9E},
                {0xA4, 0x56, 0x82, 0xF3, 0x1E, 0xC6, 0x68, 0xE5},
                {0x02, 0xA1, 0xFC, 0xC1, 0x47, 0xAE, 0x3D, 0x19},
                {0xA4, 0x55, 0x87, 0x5A, 0x58, 0xDB, 0x9E, 0x03}
        };


        private final long RHO = 0x01010101;

        public long[] keyGenerate() {
            long[] wordsFromKey = new long[key.length / 4];
            byte[] oneLong;
            for (int i = 0; i < key.length / 4; i++) {
                oneLong = new byte[4];
                System.arraycopy(key, i * 4, oneLong, 0, 4);
                reverseByte(oneLong);
                wordsFromKey[i] = transferBytesToLong(oneLong);
            }
            long[] Me = new long[wordsFromKey.length / 2];
            long[] Mo = new long[wordsFromKey.length / 2];
            for (int i = 0; i < wordsFromKey.length; i++) {
                if (i % 2 == 0) {
                    Me[i / 2] = wordsFromKey[i];
                } else {
                    Mo[i / 2] = wordsFromKey[i];
                }
            }

            long[] result = new long[40];
            for (int i = 0; i < (result.length / 2); i++) {
                long Ai = h(i * 2 * RHO, Me);
                long Bi = leftCyclicShift(h((i * 2 + 1) * RHO, Mo), 32, 8);
                long K2i = addModulo(Ai, Bi, 32) & MOD_2_32;
                long K2ip1 = leftCyclicShift(addModulo(Ai, 2 * Bi, 33) & MOD_2_32, 32, 9);
                result[2 * i] = K2i;
                result[2 * i + 1] = K2ip1;

            }
            return result;
        }


        public long[] generateSboxes() {
            long[][] matrixS = new long[key.length / 8][4];
            long[] boxS = new long[key.length / 8];
            for (int i = 0; i < key.length / 8; i++) {
                long[] pmt = new long[8];
                for (int j = 0; j < 8; j++) {
                    pmt[j] = key[j + (i * 8)];
                }
                matrixS[i] = multiplicationByRS(pmt);
            }
            for (int k = 0; k < key.length / 8; k++) {
                boxS[k] = (matrixS[k][0] & 0xFF) |
                        ((matrixS[k][1] & 0xFF) << 8) |
                        ((matrixS[k][2] & 0xFF) << 16) |
                        ((matrixS[k][3] & 0xFF) << 24);
            }
            return boxS;
        }


        private long[] multiplicationByRS(long[] input) {
            long[] result = new long[RS_MATRIX.length];

            for (int i = 0; i < RS_MATRIX.length; i++) {
                result[i] = 0;
                for (int j = 0; j < RS_MATRIX[i].length; j++) {
                    result[i] ^= multiplicationInGaloisField((byte) RS_MATRIX[i][j], (byte) input[j]);
                }
            }

            return result;
        }

        private long h(long value, long[] matrix) {
            long[] words = new long[4];
            for (int i = 0; i < 4; i++) {
                words[i] = (value >>> (8 * (3 - i))) & 0xFF;
            }
            for (int i = matrix.length - 1; i > -1; i--) {
                if (i == 3) {
                    words[0] = q(words[0], t_for_q1) ^ matrix[i];
                    words[1] = q(words[1], t_for_q0) ^ matrix[i];
                    words[2] = q(words[2], t_for_q0) ^ matrix[i];
                    words[3] = q(words[3], t_for_q1) ^ matrix[i];
                } else if (i == 2) {
                    words[0] = q(words[0], t_for_q1) ^ matrix[i];
                    words[1] = q(words[1], t_for_q1) ^ matrix[i];
                    words[2] = q(words[2], t_for_q0) ^ matrix[i];
                    words[3] = q(words[3], t_for_q0) ^ matrix[i];
                } else if (i == 1) {
                    words[0] = q(words[0], t_for_q0) ^ matrix[i];
                    words[1] = q(words[1], t_for_q1) ^ matrix[i];
                    words[2] = q(words[2], t_for_q0) ^ matrix[i];
                    words[3] = q(words[3], t_for_q1) ^ matrix[i];
                } else if (i == 0) {
                    words[0] = q(q(words[0], t_for_q0) ^ matrix[i], t_for_q1);
                    words[1] = q(q(words[1], t_for_q0) ^ matrix[i], t_for_q0);
                    words[2] = q(q(words[2], t_for_q1) ^ matrix[i], t_for_q1);
                    words[3] = q(q(words[3], t_for_q1) ^ matrix[i], t_for_q0);

                }
            }
            long[] z = multiplicationByMDS(words);
            return (z[0] & 0xFF) |
                    ((z[1] & 0xFF) << 8) |
                    ((z[2] & 0xFF) << 16) |
                    ((z[3] & 0xFF) << 24);
        }


        private long[] keyToWords(byte[] key) {
            long[] wordsFromKey = new long[key.length / 4];
            byte[] oneLong;
            for (int i = 0; i < key.length / 4; i++) {
                oneLong = new byte[4];
                System.arraycopy(key, i * 4, oneLong, 0, 4);
                reverseByte(oneLong);
                wordsFromKey[i] = transferBytesToLong(oneLong);
            }
            return wordsFromKey;
        }

        private void reverseByte(byte[] block) {
            if (block == null || block.length == 1) {
                return;
            }
            byte tmp;
            for (int i = 0; i < block.length / 2; i++) {
                tmp = block[i];
                block[i] = block[block.length - 1 - i];
                block[block.length - 1 - i] = tmp;
            }
        }


    }

    @Override
    public String getName() {
        return "Twofish";
    }

    @Override
    public int getSizeBlock() {
        return blockSize / Byte.SIZE;
    }

    @Override
    public byte[] encryption(byte[] block) {
//        log.info("Start encryption Twofish");
        long[] seperatedWords = new long[block.length / 4];
        byte[] tmp;
        for (int i = 0; i < seperatedWords.length; i++) {
            tmp = new byte[4];
            System.arraycopy(block, i * 4, tmp, 0, 4);
            seperatedWords[i] = transferBytesToLong(tmp);
        }
        for (int i = 0; i < 4; i++) {
            seperatedWords[i] ^= roundKey[i];
        }

        for (int i = 0; i < 16; i++) {
            long[] phtResult = pht(g(seperatedWords[0], boxS), g(leftCyclicShift(seperatedWords[1], 32, 8), boxS));

            long F0 = addModulo(phtResult[0], roundKey[2 * i + 8], 32);
            long F1 = addModulo(phtResult[1], roundKey[2 * i + 9], 32);

            long C2 = rightCyclicShift(F0 ^ seperatedWords[2], 32, 1);
            long C3 = F1 ^ leftCyclicShift(seperatedWords[3], 32, 1);


            seperatedWords[2] = seperatedWords[0];
            seperatedWords[3] = seperatedWords[1];
            seperatedWords[0] = C2;
            seperatedWords[1] = C3;

        }

        for (int i = 0; i < 4; i++) {
            seperatedWords[i] ^= roundKey[i + 4];
        }
        byte[] byteArray = new byte[seperatedWords.length * 4];

        for (int i = 0; i < seperatedWords.length; i++) {
            long word = seperatedWords[i];
            byteArray[i * 4] = ((byte) ((word >> 24) & 0xFF));
            byteArray[i * 4 + 1] = (byte) ((word >> 16) & 0xFF);
            byteArray[i * 4 + 2] = (byte) ((word >> 8) & 0xFF);
            byteArray[i * 4 + 3] = (byte) (word & 0xFF);
        }
//        log.info("Finish encryption Twofish");
        return byteArray;

    }

    @Override
    public byte[] decoding(byte[] block) {
        //log.info("Start decoding Twofish");
        long[] seperatedWords = new long[block.length / 4];
        byte[] tmp;
        for (int i = 0; i < block.length / 4; i++) {
            tmp = new byte[4];
            System.arraycopy(block, i * 4, tmp, 0, 4);
            seperatedWords[i] = transferBytesToLong(tmp);
        }
        for (int i = 0; i < 4; i++) {
            seperatedWords[i] ^= roundKey[i + 4];
        }

        for (int i = 15; i >= 0; i--) {
            long C2 = seperatedWords[0];
            long C3 = seperatedWords[1];
            long[] phtResult = pht(g(seperatedWords[2], boxS), g(leftCyclicShift(seperatedWords[3], 32, 8), boxS));

            long F0 = addModulo(phtResult[0], roundKey[2 * i + 8], 32);
            long F1 = addModulo(phtResult[1], roundKey[2 * i + 9], 32);


            long ex1 = F0 ^ leftCyclicShift(C2, 32, 1);
            long ex2 = rightCyclicShift(C3 ^ F1, 32, 1);

            seperatedWords[0] = seperatedWords[2];
            seperatedWords[1] = seperatedWords[3];
            seperatedWords[2] = ex1;
            seperatedWords[3] = ex2;

        }

        for (int i = 0; i < 4; i++) {
            seperatedWords[i] ^= roundKey[i];
        }
        byte[] byteArray = new byte[seperatedWords.length * 4];

        for (int i = 0; i < seperatedWords.length; i++) {
            long word = seperatedWords[i];
            byteArray[i * 4] = ((byte) ((word >> 24) & 0xFF));
            byteArray[i * 4 + 1] = (byte) ((word >> 16) & 0xFF);
            byteArray[i * 4 + 2] = (byte) ((word >> 8) & 0xFF);
            byteArray[i * 4 + 3] = (byte) (word & 0xFF);
        }
        //log.info("Finish decoding Twofish");
        return byteArray;
    }


    private long[] pht(long a, long b) {
        long x = addModulo(a, b, 32) & 0xFFFFFFFFL;
        long y = addModulo(a, b, 33) & 0xFFFFFFFFL;
        return new long[]{x, y};
    }
}
