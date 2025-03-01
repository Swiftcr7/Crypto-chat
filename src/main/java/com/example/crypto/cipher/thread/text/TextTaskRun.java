package com.example.crypto.cipher.thread.text;

import java.util.concurrent.ExecutionException;

public interface TextTaskRun {
    public byte[] threadTextRun(byte[] block) throws ExecutionException, InterruptedException;
}
