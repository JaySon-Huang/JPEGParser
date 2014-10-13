package com.jayson.utils;


import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Package : com.jayson
 * Author  : JaySon
 * Date    : 8/25/14
 */
public class AESUtils {
    public static byte[] encrypt(String key, String iv, byte[] plaintext) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        SecretKeySpec sKeySpec = new SecretKeySpec(key.getBytes(), "AES");
        IvParameterSpec ivSpec = new IvParameterSpec(iv.getBytes());
        javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("AES/CFB/NoPadding");
        cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, sKeySpec, ivSpec);
        byte[] encrypted = cipher.doFinal(plaintext);
        return encrypted;
    }

    public static byte[] decrypt(String key, String iv, byte[] ciphertext) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        SecretKeySpec sKeySpec = new SecretKeySpec(key.getBytes(), "AES");
        IvParameterSpec ivSpec = new IvParameterSpec(iv.getBytes());
        javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("AES/CFB/NoPadding");
        cipher.init(javax.crypto.Cipher.DECRYPT_MODE, sKeySpec, ivSpec);
        byte[] decrypted = cipher.doFinal(ciphertext);
        return decrypted;
    }

    public static void main(String[] args) throws NoSuchPaddingException, InvalidKeyException, NoSuchAlgorithmException, IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException {
        String key = "JaySon.//.noSyaJ";
        String iv  = "/.noSyaJJaySon./";
        byte[] plaintext = new byte[]{0x01,0x03,0x0a,0x0f, (byte) 0xff, (byte) 0xef, 0x00};
        byte[] encrypted = encrypt(key, iv, plaintext);
        for (byte b:encrypted){
            System.out.print(String.format("%02X,",b));
        }
        System.out.println();
        byte[] decrypted = decrypt(key, iv, encrypted);
        for (byte b:decrypted){
            System.out.print(String.format("%02X,",b));
        }
        System.out.println();
    }
}
