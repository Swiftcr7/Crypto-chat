package com.example.crypto.cipher.thread.file;

import lombok.AllArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.processing.Filer;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import java.util.RandomAccess;
import java.util.concurrent.*;

@AllArgsConstructor
public class FileTaskRunConcrete implements FileTaskRun {
    private int sizeBlock;
    private FileRunForThread fileTaskRun;

    @Override
    public String run(String inputFile, String OutputFile, activity act) throws IOException {
        List<Future<byte[]>> futuresList = new ArrayList<>();
        int processors = Runtime.getRuntime().availableProcessors();
        ExecutorService service = Executors.newFixedThreadPool(processors);
        try (RandomAccessFile file = new RandomAccessFile(inputFile, "r")) {
            long index = 0;
            long numberBlocksForThread = (file.length() / sizeBlock + processors - 1) / processors;
            while (index < file.length()) {
                long finalIndex = index;
                futuresList.add(service.submit(() -> fileTaskRun.run(inputFile, finalIndex, numberBlocksForThread, act)));
                index += numberBlocksForThread * sizeBlock;
            }

        } catch (IOException e) {
            throw new IOException(e);
        }

        try (RandomAccessFile file = new RandomAccessFile(OutputFile, "rw")) {
            for (var future : futuresList) {
                byte[] text = future.get();
                file.write(text);
            }

        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new IOException(e);
        }
        service.shutdown();

        try {
            if (!service.awaitTermination(1000, TimeUnit.MILLISECONDS)) {
                service.shutdownNow();
            }
        } catch (InterruptedException e) {
            service.shutdownNow();
            Thread.currentThread().interrupt();
        }
        return OutputFile;
    }
}
