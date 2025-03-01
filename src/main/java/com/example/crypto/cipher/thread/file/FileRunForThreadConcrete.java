package com.example.crypto.cipher.thread.file;

import com.example.crypto.cipher.mode.modeInterface.Mode;
import lombok.AllArgsConstructor;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.concurrent.ExecutionException;

@AllArgsConstructor
public class FileRunForThreadConcrete implements FileRunForThread {
    private Mode cipherMode;

    @Override
    public byte[] run(String pathToFile, long value, long sizeThread, FileTaskRun.activity act) throws IOException, ExecutionException, InterruptedException {
        byte[] preliminaryResult = new byte[(int) sizeThread];
        try (RandomAccessFile file = new RandomAccessFile(pathToFile, "r")) {
            file.seek(value);
            int countByte = file.read(preliminaryResult);
            if (countByte != sizeThread) {
                byte[] block = new byte[countByte];
                System.arraycopy(preliminaryResult, 0, block, 0, countByte);
                preliminaryResult = block;
            }

        } catch (IOException e) {
            throw new IOException(e);
        }
        return switch (act) {
            case DECODING -> cipherMode.decoding(preliminaryResult);
            case ENCRYPTION -> cipherMode.encryption(preliminaryResult);
        };
    }
}
