package com.example.crypto.cipher.padding;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ZerosConcrete extends Padding {
    @Override
    protected byte[] getPadding(byte sizePadding) {
        log.info("Start get padding Zeros");

        byte[] padding = new byte[sizePadding];

        for (int i = 0; i < padding.length - 1; i++) {
            padding[i] = 0;
        }

        padding[padding.length - 1] = sizePadding;
        return padding;
    }

}
