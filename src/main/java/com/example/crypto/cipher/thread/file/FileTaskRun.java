package com.example.crypto.cipher.thread.file;

import java.io.FileNotFoundException;
import java.io.IOException;

public interface FileTaskRun {
    enum activity {
        ENCRYPTION,
        DECODING
    }

    public String run(String inputFile, String OutputFile, activity act) throws IOException;
}
