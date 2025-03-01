package com.example.crypto.cipher.padding;

import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;

@Slf4j
public class PKCS7Concrete extends Padding {
    @Override
    protected byte[] getPadding(byte sizePadding) {
        log.info("Start get padding PKCS7");

        byte[] padding = new byte[sizePadding];
        Arrays.fill(padding, sizePadding);
        return padding;
    }

}
