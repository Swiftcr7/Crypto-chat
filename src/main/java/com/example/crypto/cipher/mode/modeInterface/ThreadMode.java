package com.example.crypto.cipher.mode.modeInterface;

import org.apache.commons.lang3.tuple.Pair;

import java.util.concurrent.ExecutionException;

public interface ThreadMode {
    public Pair<Integer, byte[]> run(int countBlock, int blockSize, byte[] array, int index) throws ExecutionException, InterruptedException;

}
