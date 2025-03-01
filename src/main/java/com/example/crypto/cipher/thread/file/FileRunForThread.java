package com.example.crypto.cipher.thread.file;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

public interface FileRunForThread {
    public byte[] run(String pathToFile, long value, long sizeThread, FileTaskRun.activity act) throws IOException, ExecutionException, InterruptedException;
}
