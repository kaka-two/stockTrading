package com.kakas.stockTrading.bean;

import com.kakas.stockTrading.util.SignatureUtil;

/**
 * 为内部用户进行安全验证。
 * @param userId
 * @param expireAt
 */
public record AuthToken(Long userId, Long expireAt) {
    public boolean isExpired() {
        return System.currentTimeMillis() > expireAt();
    }

    public boolean isAboutToExpire() {
        return expireAt() - System.currentTimeMillis() < 1800_000;
    }

    public AuthToken refresh() {
        return new AuthToken(this.userId(), System.currentTimeMillis() + 3600_000);
    }

    // 将内部用户信息转为安全字符串
    public String toSecureString(String hmacKey) {
        String payload = userId() + ":" + expireAt();
        String hash = SignatureUtil.sign(payload, hmacKey);
        String token = payload + ":" + hash;
        return token;
    }

    // 校验内部用户信息。
    public static AuthToken fromSecureString(String token, String hmacKey) {
        String[] ss = token.split(":");
        if (ss.length != 3 || ss[0].isEmpty() || ss[1].isEmpty() || ss[2].isEmpty()) {
            throw new IllegalArgumentException("Invalid token.");
        }
        String userId = ss[0];
        String expireAt = ss[1];
        String sig = ss[2];
        if (!SignatureUtil.valid(userId + ":" + expireAt, hmacKey, sig)) {
            throw new IllegalArgumentException("Invalid token.");
        }
        return new AuthToken(Long.parseLong(userId), Long.parseLong(expireAt));
    }
}
