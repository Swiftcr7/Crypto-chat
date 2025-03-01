package com.example.crypto.cipher.padding;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ANSIX923Concrete extends Padding {
    @Override
    protected byte[] getPadding(byte sizePadding) {
        log.info("Start get padding ANSIX923");

        byte[] padding = new byte[sizePadding];
        for (int i = 0; i < padding.length - 1; i++) {
            padding[i] = 0;
        }
        padding[padding.length - 1] = sizePadding;
        return padding;
    }
}
