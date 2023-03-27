package com.kakas.stockTrading.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

public enum OrderStatus {
    // 挂起
    PENDING(false),

    // 完全完成
    FULLY_FILLED(true),

    // 完全取消
    FULLY_CANCELED(true),

    // 部分完成
    PARTIAL_FILLED(false),

    // 部分取消
    PARTIAL_CANCELED(true);

    public final boolean isFinalStatus;
    OrderStatus(boolean isFinalStatus) {
        this.isFinalStatus = isFinalStatus;
    }

}
