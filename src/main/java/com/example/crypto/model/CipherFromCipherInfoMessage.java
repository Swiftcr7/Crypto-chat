package com.example.crypto.model;

import com.example.crypto.cipher.CipherManagement;
import com.example.crypto.cipher.algoritm.CipherInterface;
import com.example.crypto.cipher.algoritm.RC6Concrete;
import com.example.crypto.cipher.algoritm.TwofishConcrete;
import lombok.extern.slf4j.Slf4j;

import java.math.BigInteger;

@Slf4j
public class CipherFromCipherInfoMessage {
    private CipherFromCipherInfoMessage() {

    }

    public static CipherManagement createCipher(CipherInfoMassage cipherInfoMassage, BigInteger key, BigInteger modulo) {
        BigInteger publicKey = new BigInteger(cipherInfoMassage.getPublicKey());
        BigInteger readyKey = publicKey.modPow(key, modulo);
        byte[] tempolararyKey = readyKey.toByteArray();
        byte[] byteKey = new byte[cipherInfoMassage.getSizeKeyInBits() / Byte.SIZE];
        System.arraycopy(tempolararyKey, 0, byteKey, 0, cipherInfoMassage.getSizeKeyInBits() / Byte.SIZE);
        CipherInterface cipher = switch (cipherInfoMassage.getNameAlgorithm()) {
            case "RC6" ->
                    new RC6Concrete(cipherInfoMassage.getSizeBlockInBits(), 20, cipherInfoMassage.getSizeKeyInBits(), byteKey);
            case "Twofish" -> new TwofishConcrete(byteKey, cipherInfoMassage.getSizeKeyInBits());
            default -> throw new IllegalStateException("Unexpected value: " + cipherInfoMassage.getNameAlgorithm());
        };
        CipherManagement.padding padding = switch (cipherInfoMassage.getNamePadding()) {
            case "ANSIX923" -> CipherManagement.padding.ANSIX923;
            case "ISO10126" -> CipherManagement.padding.ISO10126;
            case "PKCS7" -> CipherManagement.padding.PKCS7;
            case "Zeros" -> CipherManagement.padding.Zeros;

            default -> throw new IllegalStateException("Unexpected value: " + cipherInfoMassage.getNamePadding());
        };

        CipherManagement.mode mode = switch (cipherInfoMassage.getEncryptionMode()) {
            case "CBC" -> CipherManagement.mode.CipherBlockChaining;
            case "CFB" -> CipherManagement.mode.CipherFeedback;
            case "CTR" -> CipherManagement.mode.CounterMode;
            case "ECB" -> CipherManagement.mode.ElectronicCodebook;
            case "OFB" -> CipherManagement.mode.OutputFeedback;
            case "PCBC" -> CipherManagement.mode.PropagatingCipherBlockChaining;
            case "RD" -> CipherManagement.mode.RandomDelta;

            default -> throw new IllegalStateException("Unexpected value: " + cipherInfoMassage.getEncryptionMode());
        };
        return new CipherManagement(cipher, padding, mode, cipherInfoMassage.getInitializationVector());

    }
}
