package com.example.crypto.cipher.mode;

import com.example.crypto.cipher.algoritm.CipherInterface;
import com.example.crypto.cipher.mode.modeInterface.ThreadMode;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;

import java.util.concurrent.ExecutionException;

@AllArgsConstructor
public class ThreadModeEcbEncrypt implements ThreadMode {
    private final CipherInterface cipher;

    @Override
    public Pair<Integer, byte[]> run(int countBlock, int blockSize, byte[] array, int index) throws ExecutionException, InterruptedException {
        byte[] encryptionRes = new byte[blockSize * countBlock];
        byte[] block = new byte[blockSize];
        for (int i = 0; i < countBlock; i++) {
            System.arraycopy(array, index + blockSize * i, block, 0, blockSize);
            byte[] encryptBlock = cipher.encryption(block);
            System.arraycopy(encryptBlock, 0, encryptionRes, blockSize * i, blockSize);
        }
        return Pair.of(index, encryptionRes);

    }
}
