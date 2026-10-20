package com.muniv24.volumebar;

import android.content.Context;
import android.util.Base64;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.Cipher;

/**
 * The app's own debugging key. The TV asks "Allow debugging?" the first time it sees it; with
 * "Always allow" it is trusted from then on. Made once and kept in the app's private files.
 */
final class AdbKey {
    /** ASN.1 header that marks the 20 signed bytes as a SHA-1 digest, as adb expects. */
    private static final byte[] SHA1_PREFIX = {
            0x30, 0x21, 0x30, 0x09, 0x06, 0x05, 0x2b, 0x0e, 0x03, 0x02, 0x1a, 0x05, 0x00, 0x04, 0x14};
    private static final int KEY_BITS = 2048, WORDS = KEY_BITS / 32;
    private static final String NAME = "volumebar@tv";

    private final KeyPair pair;

    private AdbKey(KeyPair pair) {
        this.pair = pair;
    }

    static AdbKey load(Context c) throws IOException, GeneralSecurityException {
        File priv = new File(c.getFilesDir(), "adbkey");
        File pub = new File(c.getFilesDir(), "adbkey.pub");
        KeyFactory rsa = KeyFactory.getInstance("RSA");
        if (priv.exists() && pub.exists()) {
            return new AdbKey(new KeyPair(
                    rsa.generatePublic(new X509EncodedKeySpec(Files.readAllBytes(pub.toPath()))),
                    rsa.generatePrivate(new PKCS8EncodedKeySpec(Files.readAllBytes(priv.toPath())))));
        }
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(KEY_BITS);
        KeyPair made = gen.generateKeyPair();
        write(priv, made.getPrivate().getEncoded());
        write(pub, made.getPublic().getEncoded());
        return new AdbKey(made);
    }

    /** Signs the TV's 20-byte challenge. */
    byte[] sign(byte[] token) throws GeneralSecurityException {
        Cipher rsa = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        rsa.init(Cipher.ENCRYPT_MODE, pair.getPrivate());
        rsa.update(SHA1_PREFIX);
        return rsa.doFinal(token);
    }

    /**
     * The public key in adb's own format: word count, n0inv, modulus, R^2 mod n, exponent, all
     * little-endian 32-bit words, then base64 and a name.
     */
    byte[] publicKeyForTv() {
        RSAPublicKey key = (RSAPublicKey) pair.getPublic();
        BigInteger n = key.getModulus();
        BigInteger word = BigInteger.ONE.shiftLeft(32);
        ByteBuffer b = ByteBuffer.allocate(4 + 4 + 4 * WORDS * 2 + 4).order(ByteOrder.LITTLE_ENDIAN);
        b.putInt(WORDS);
        b.putInt(n.mod(word).modInverse(word).negate().mod(word).intValue());
        putWords(b, n, word);
        putWords(b, BigInteger.ONE.shiftLeft(2 * KEY_BITS).mod(n), word);
        b.putInt(key.getPublicExponent().intValue());
        String line = Base64.encodeToString(b.array(), Base64.NO_WRAP) + " " + NAME + "\0";
        return line.getBytes(StandardCharsets.UTF_8);
    }

    private static void putWords(ByteBuffer b, BigInteger value, BigInteger word) {
        for (int i = 0; i < WORDS; i++) {
            b.putInt(value.mod(word).intValue());
            value = value.shiftRight(32);
        }
    }

    private static void write(File f, byte[] data) throws IOException {
        try (OutputStream out = new FileOutputStream(f)) {
            out.write(data);
        }
    }
}
