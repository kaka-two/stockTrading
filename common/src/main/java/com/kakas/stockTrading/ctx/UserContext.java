package com.kakas.stockTrading.ctx;

public class UserContext implements AutoCloseable {
    final static ThreadLocal<Long> THREAD_LOCAL_CTX = new ThreadLocal<>();

    public UserContext(Long userId) {
        THREAD_LOCAL_CTX.set(userId);
    }

    public static Long getUserId() {
        return THREAD_LOCAL_CTX.get();
    }

    public static Long getRequiredUserId() {
        Long userId = getUserId();
        if (userId == null) {
            throw new RuntimeException("Need signin first.");
        }
        return userId;
    }

    @Override
    public void close() {
        THREAD_LOCAL_CTX.remove();
    }

}
