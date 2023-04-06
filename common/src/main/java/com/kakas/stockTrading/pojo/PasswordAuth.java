package com.kakas.stockTrading.pojo;

import lombok.Data;

@Data
public class PasswordAuth {
    /**
     * 关联至用户ID.
     */
    private Long userId;

    /**
     * 随机字符串用于创建Hmac-SHA256.
     */
    private String random;

    /**
     * 存储HmacSHA256哈希 password = HmacSHA256(原始口令, key=random).
     */
    private String passwd;

    public static PasswordAuth createPasswordAuth(Long userId, String random, String passwd) {
        PasswordAuth passwordAuth = new PasswordAuth();
        passwordAuth.setUserId(userId);
        passwordAuth.setRandom(random);
        passwordAuth.setPasswd(passwd);
        return passwordAuth;
    }
}
