package com.example.crypto.cipher.padding;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;

public abstract class Padding {
    protected abstract byte[] getPadding(byte sizePadding);


    public byte[] insertPadding(byte[] block, int blockSize) {
        if (block == null) {
            throw new IllegalArgumentException("empty block");
        }

        if (blockSize <= 0) {
            throw new IllegalArgumentException("incorrect size block");
        }


        byte sizePadding = (byte) (blockSize - block.length % blockSize);
        byte[] padding = getPadding(sizePadding);
        byte[] result = new byte[block.length + sizePadding];
        System.arraycopy(block, 0, result, 0, block.length);
        System.arraycopy(padding, 0, result, block.length, padding.length);
        return result;
    }

    public byte[] deletePadding(byte[] block) {
        if (block == null || block.length == 0) {
            throw new IllegalArgumentException("Incorrect block");
        }
        byte sizePadding = block[block.length - 1];
        if (sizePadding > block.length) {
            throw new IllegalArgumentException("Incorrect psdding size");
        }
        byte[] result = new byte[block.length - sizePadding];
        System.arraycopy(block, 0, result, 0, block.length - sizePadding);
        return result;
    }

    public String insertPadding(String fileWay, int sizeBlock) throws FileNotFoundException {
        String newFileWay = addPostfixToFileName(fileWay, "_padding");
        try {
            FileUtils.copyFile(new File(fileWay), new File(newFileWay));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try (RandomAccessFile file = new RandomAccessFile(fileWay, "r")) {
            RandomAccessFile newFile = new RandomAccessFile(newFileWay, "rw");
            newFile.seek(file.length());
            byte[] array = getPadding((byte) (sizeBlock - file.length() % sizeBlock));
            newFile.write(array);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return newFileWay;

    }

    public String deletePadding(String fileWay) {
        String newFileWay = addPostfixToFileName(fileWay, "_padding");
        byte[] buffer;
        byte sizePadding;
        try (RandomAccessFile inputFile = new RandomAccessFile(fileWay, "r")) {
            inputFile.seek(inputFile.length() - 1);
            sizePadding = inputFile.readByte();
            buffer = new byte[(int) (inputFile.length() - sizePadding)];
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try (RandomAccessFile inputFile = new RandomAccessFile(fileWay, "r");
             RandomAccessFile paddingFile = new RandomAccessFile(newFileWay, "rw")) {
            inputFile.read(buffer);
            paddingFile.write(buffer);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return newFileWay;
    }

    private String addPostfixToFileName(String fileName, String postfix) {
        int dotIndex = fileName.lastIndexOf('.');
        String baseName = fileName.substring(0, dotIndex);
        String extension = fileName.substring(dotIndex);
        return baseName + postfix + extension;
    }

}
