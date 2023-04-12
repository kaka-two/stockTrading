package com.kakas.stockTrading.util;

import java.util.UUID;

public class IdUtil {
    public static String generateUniqueId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
