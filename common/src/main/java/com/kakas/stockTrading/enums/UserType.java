package com.kakas.stockTrading.enums;

public enum UserType {
    ROOT(1),
    TRADER(0);

    private final long userId;

    public long getUserTypeId() {
        return this.userId;
    }

    UserType(long userId) {
        this.userId = userId;
    }

}
