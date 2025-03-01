package com.example.crypto.cipher;

import com.example.crypto.cipher.algoritm.CipherInterface;
import com.example.crypto.cipher.mode.*;
import com.example.crypto.cipher.mode.modeInterface.Mode;
import com.example.crypto.cipher.padding.*;
import com.example.crypto.cipher.thread.file.FileRunForThreadConcrete;
import com.example.crypto.cipher.thread.file.FileTaskRun;
import com.example.crypto.cipher.thread.file.FileTaskRunConcrete;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutionException;

@Slf4j
public class CipherManagement {
    public enum padding {
        ANSIX923,
        ISO10126,
        PKCS7,
        Zeros
    }

    public enum mode {
        CipherBlockChaining,
        CipherFeedback,
        CounterMode,
        ElectronicCodebook,
        OutputFeedback,
        PropagatingCipherBlockChaining,
        RandomDelta
    }

    private final byte[] vector;
    private final CipherInterface cipher;
    private final Mode mode;
    private final Padding padding;


    public CipherManagement(CipherInterface cipher, padding padding, mode mode, byte[] vector) {
        if (vector.length != cipher.getSizeBlock()) {
            throw new IllegalArgumentException("no equals size vector");
        }
        this.vector = vector;
        this.cipher = cipher;
        this.padding = getPadding(padding);
        this.mode = getMode(mode);
    }

    private Padding getPadding(padding p) {
        return switch (p) {
            case ANSIX923 -> new ANSIX923Concrete();
            case ISO10126 -> new ISO10126PaddingConcrete();
            case PKCS7 -> new PKCS7Concrete();
            case Zeros -> new ZerosConcrete();
        };
    }

    private Mode getMode(mode m) {
        return switch (m) {
            case CipherFeedback -> new CipherFeedback(this.cipher, this.vector);
            case CipherBlockChaining -> new CipherBlockChaining(this.cipher, this.vector);
            case CounterMode -> new CounterMode(this.vector, this.cipher);
            case RandomDelta -> new RandomDelta(this.cipher, vector);
            case PropagatingCipherBlockChaining -> new PropagatingCipherBlockChaining(this.cipher, this.vector);
            case OutputFeedback -> new OutputFeedback(this.vector, this.cipher);
            case ElectronicCodebook -> new ElectronicCodebook(this.cipher);
        };
    }

    public byte[] encrypt(byte[] block) {
        try {
            log.info("Encrypt with cipher {}", cipher.getName());
            return mode.encryption(padding.insertPadding(block, cipher.getSizeBlock()));
        } catch (ExecutionException e) {
            log.error(e.getMessage());
            Thread.currentThread().interrupt();
        } catch (InterruptedException e) {
            log.error(e.getMessage());
        }
        return new byte[0];
    }

    public String encryptFile(String pathToFile) {
        String PathEncryptionFile = null;
        try {
            String PathToFileWithPadding = padding.insertPadding(pathToFile, cipher.getSizeBlock());
            PathEncryptionFile = new FileTaskRunConcrete(cipher.getSizeBlock(),
                    new FileRunForThreadConcrete(mode)).run(PathToFileWithPadding, addPostfixToFileName(pathToFile, "_encrypt"), FileTaskRun.activity.ENCRYPTION);
            Files.delete(Path.of(PathToFileWithPadding));
        } catch (FileNotFoundException e) {
            log.error(e.getMessage());
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            log.error(e.getMessage());
        }
        return PathEncryptionFile;

    }

    public byte[] TextDecoding(byte[] block) {
        try {
            log.info("Decoding with cipher {}", cipher.getName());

            return padding.deletePadding(mode.decoding(block));
        } catch (ExecutionException e) {
            log.error(e.getMessage());
            Thread.currentThread().interrupt();
        } catch (InterruptedException e) {
            log.error(e.getMessage());
        }
        return new byte[0];
    }

    public String decodingFile(String pathToFile) {
        String PathDecodingFileWithPadding = null;
        try {
            PathDecodingFileWithPadding = new FileTaskRunConcrete(cipher.getSizeBlock(),
                    new FileRunForThreadConcrete(mode)).run(pathToFile, addPostfixToFileName(pathToFile, "_decode"), FileTaskRun.activity.DECODING);
            String PathDecodingFile = padding.deletePadding(PathDecodingFileWithPadding);
            if (!(new File(PathDecodingFile).renameTo(new File(PathDecodingFileWithPadding)))) {
                log.error("Error while renaming file");
            }
        } catch (FileNotFoundException e) {
            log.error(e.getMessage());
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            log.error(e.getMessage());
        }
        return PathDecodingFileWithPadding;

    }

    private String addPostfixToFileName(String pathToInputFile, String postfix) {
        int dotIndex = pathToInputFile.lastIndexOf('.');
        String baseName = pathToInputFile.substring(0, dotIndex);
        String extension = pathToInputFile.substring(dotIndex);
        return baseName + postfix + extension;
    }
}
