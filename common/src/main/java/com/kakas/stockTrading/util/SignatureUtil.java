package com.kakas.stockTrading.util;

import lombok.extern.slf4j.Slf4j;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Slf4j
public class SignatureUtil {
    private static final String ALGORITHM = "HmacSHA256";

    public static boolean valid(String message, String secret, String signature) {
        return signature != null && signature.equals(sign(message, secret));
    }

    public static String sign(String message, String secret) {
        try {
            Mac hmac = Mac.getInstance(ALGORITHM);
            SecretKeySpec secret_key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM);
            hmac.init(secret_key);
            byte[] bytes = hmac.doFinal(message.getBytes());
            log.info("service sign is "+byteArrayToHexString(bytes));
            return byteArrayToHexString(bytes);
        } catch (Exception ex) {
            log.error("签名错误：", ex);
        }
        return null;
    }

    private static String byteArrayToHexString(byte[] bytes) {
        StringBuilder hs = new StringBuilder();
        String tempStr;
        for (int index = 0; bytes != null && index < bytes.length; index++) {
            tempStr = Integer.toHexString(bytes[index] & 0XFF);
            if (tempStr.length() == 1)
                hs.append('0');
            hs.append(tempStr);
        }
        return hs.toString().toLowerCase();
    }

    /**
     * Generate SHA-256 as hex string (all lower-case).
     *.
     * @return Hex string.
     */
    public static String sha256(String str) {
        byte[] input = str.getBytes(StandardCharsets.UTF_8);
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        md.update(input);
        byte[] digest = md.digest();
        return HexFormat.of().formatHex(digest);
    }
}


