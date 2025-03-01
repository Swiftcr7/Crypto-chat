package com.example.crypto.cipher.padding;

import lombok.extern.slf4j.Slf4j;

import java.util.Random;

@Slf4j
public class ISO10126PaddingConcrete extends Padding {
    private final Random ran = new Random();

    @Override
    protected byte[] getPadding(byte sizePadding) {
        log.info("Start get padding IS010126");

        byte[] padding = new byte[sizePadding];
        for (int i = 0; i < sizePadding - 1; i++) {
            padding[i] = (byte) (ran.nextInt(Byte.MAX_VALUE - Byte.MIN_VALUE) + Byte.MIN_VALUE);
        }
        padding[sizePadding - 1] = sizePadding;
        return padding;
    }
}
