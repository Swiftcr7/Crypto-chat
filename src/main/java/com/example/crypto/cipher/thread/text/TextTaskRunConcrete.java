package com.example.crypto.cipher.thread.text;

import com.example.crypto.cipher.mode.modeInterface.ThreadMode;
import com.example.crypto.cipher.thread.text.TextTaskRun;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

@AllArgsConstructor
public class TextTaskRunConcrete implements TextTaskRun {
    private ThreadMode threadMode;
    private int blockSize;

    @Override
    public byte[] threadTextRun(byte[] block) throws ExecutionException, InterruptedException {
        if (block == null) {
            throw new IllegalArgumentException("Empty block");
        }
        byte[] result = new byte[block.length];
        List<Future<Pair<Integer, byte[]>>> futuresList = new ArrayList<>();
        int processors = Runtime.getRuntime().availableProcessors();
        ExecutorService service = Executors.newFixedThreadPool(processors);
        int numberBlocksForThread = (block.length / blockSize + processors - 1) / processors;

        for (int i = 0; i < block.length; i += numberBlocksForThread * blockSize) {
            int countBlock;
            if (numberBlocksForThread * blockSize + i < block.length) {
                countBlock = numberBlocksForThread;
            } else {
                countBlock = (block.length - i) / blockSize;
            }
            int finalI = i;
            futuresList.add(service.submit(() -> threadMode.run(countBlock, blockSize, block, finalI)));
        }
        try {
            while (!futuresList.isEmpty()) {
                List<Future<Pair<Integer, byte[]>>> futuresListNotDone = futuresList.stream().filter(future -> !future.isDone()).toList();
                for (var future : futuresList) {
                    if (futuresListNotDone.contains(future)) {
                        continue;
                    }
                    var res = future.get();
                    System.arraycopy(res.getRight(), 0, result, res.getLeft(), res.getRight().length);
                }
                futuresList = futuresListNotDone;
            }
        } catch (InterruptedException ex) {
            throw new InterruptedException(ex.getMessage());
        } catch (ExecutionException ex) {
            throw new ExecutionException(ex);
        } finally {
            service.shutdown();

            try {
                if (!service.awaitTermination(1000, TimeUnit.MILLISECONDS)) {
                    service.shutdownNow();
                }
            } catch (InterruptedException e) {
                service.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }


        return result;
    }
}
