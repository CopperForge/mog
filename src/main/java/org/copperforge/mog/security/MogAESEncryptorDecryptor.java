package org.copperforge.mog.security;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import org.copperforge.mog.MogException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MogAESEncryptorDecryptor implements MogDecryptor, MogEncryptor {

    private Logger log = LoggerFactory.getLogger(MogAESEncryptorDecryptor.class);
    private final String password;

    public MogAESEncryptorDecryptor(String password) throws MogException {
        try {
            this.password = password;
        } catch (Exception e) {
            throw new MogException("Unable to generate AES key", e);
        }
    }

    @Override
    public String encrypt(String value) throws MogException {
        try {
            // Create salt
            byte[] salt = new byte[8];
            new SecureRandom().nextBytes(salt);

            // Create key
            byte[] passAndSalt = concat(password.getBytes(), salt);
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] key = md.digest(passAndSalt);

            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 65536, 256);
            // SecretKeySpec secretKey = new SecretKeySpec(key, "AES");
            SecretKey secretKey = new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");

            // Derive IV
            md.reset();
            byte[] iv = Arrays.copyOfRange(
                    md.digest(concat(key, passAndSalt)), 0, 16);
            // Encrypt
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec(iv));
            // Format output
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            bos.writeBytes("Salted__".getBytes(StandardCharsets.US_ASCII));
            bos.writeBytes(salt);
            bos.writeBytes(cipher.doFinal(
                    value.getBytes(StandardCharsets.UTF_8)));
            String cipherText = Base64.getEncoder()
                    .encodeToString(bos.toByteArray());
            return cipherText;
        } catch (Exception e) {
            throw new MogException("unable to encrypt", e);
        }
    }

    @Override
    public String decrypt(String value) throws MogException {
        try {
            log.info("Decrypting '" + value + "'");
            // Parse cipher text
            byte[] cipherBytes = Base64.getDecoder().decode(value);
            //byte[] cipherBytes = value.getBytes();
            byte[] salt = Arrays.copyOfRange(cipherBytes, 8, 16);
            cipherBytes = Arrays.copyOfRange(
                    cipherBytes, 16, cipherBytes.length);

            // Derive key
            byte[] passAndSalt = concat(password.getBytes(), salt);
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] key = md.digest(passAndSalt);

            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 65536, 256);
            // SecretKeySpec secretKey = new SecretKeySpec(key, "AES");
            SecretKey secretKey = new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");

            // Derive IV
            md.reset();
            byte[] iv = Arrays.copyOfRange(
                    md.digest(concat(key, passAndSalt)), 0, 16);
            // Decrypt
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey,
                    new IvParameterSpec(iv));
            String clearText = new String(cipher.doFinal(cipherBytes));
            return new String(clearText);
        } catch (Exception e) {
            throw new MogException("Unable to decrypt", e);
        }
    }

    private byte[] concat(byte[] a, byte[] b) {
        byte[] c = new byte[a.length + b.length];
        System.arraycopy(a, 0, c, 0, a.length);
        System.arraycopy(b, 0, c, a.length, b.length);
        return c;
    }

}
