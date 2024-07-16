package org.copperforge.mog.security;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;
import org.copperforge.mog.MogException;

public class MogAESEncryptorDecryptor implements MogDecryptor, MogEncryptor {

    private final SecretKeySpec key;
    private final Cipher cipher;

    public MogAESEncryptorDecryptor(String password) throws MogException {
        try {
            this.cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            this.key = new SecretKeySpec(password.getBytes(), "AES");
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new MogException(e);
        }

    }

    @Override
    public String encrypt(String value) throws MogException {
        try {
            byte[] cipherText = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            cipher.init(Cipher.ENCRYPT_MODE, key);
            String encodedTxt = Base64.encodeBase64URLSafeString(cipherText);
            System.out.println("Encrypted String  : " + encodedTxt);
            return encodedTxt;
        } catch (IllegalBlockSizeException | BadPaddingException | InvalidKeyException e) {
            throw new MogException(e);
        }
    }

    @Override
    public String decrypt(String value) throws MogException {
        try {
            byte[] cipherText = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            String encodedTxt = Base64.encodeBase64URLSafeString(cipherText);
            System.out.println("Encrypted String  : " + encodedTxt);
            return encodedTxt;
        } catch (IllegalBlockSizeException | BadPaddingException e) {
            throw new MogException(e);
        }
    }

}
