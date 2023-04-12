package com.kakas.stockTrading.bean;

import com.kakas.stockTrading.enums.ApiError;
import com.kakas.stockTrading.enums.AssertType;
import lombok.Data;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Data
public class TransferRequestBean {
    private String transferId;

    private AssertType asset;

    private Long fromUserId;

    private Long toUserId;

    private BigDecimal amount;

    public void validate() {
        if (fromUserId == null || fromUserId.longValue() <= 0) {
            throw new RuntimeException(ApiError.TRANSFER_FAILED.name() + "fromUserId" + " Must specify fromUserId.");
        }
        if (toUserId == null || toUserId.longValue() <= 0) {
            throw new RuntimeException(ApiError.TRANSFER_FAILED.name() + "toUserId" + " Must specify toUserId.");
        }
        if (fromUserId.longValue() == toUserId.longValue()) {
            throw new RuntimeException(ApiError.TRANSFER_FAILED.name() + "toUserId" + " Must be different with fromUserId");
        }
        if (asset == null) {
            throw new RuntimeException(ApiError.TRANSFER_FAILED.name() + "asset" + "Must specify asset.");
        }
        if (amount == null) {
            throw new RuntimeException(ApiError.TRANSFER_FAILED.name() + "amount" + "Must specify amount.");
        }
        amount = amount.setScale(2, RoundingMode.FLOOR);
        if (amount.signum() <= 0) {
            throw new RuntimeException(ApiError.TRANSFER_FAILED.name() + "amount" + "Must specify positive amount.");
        }
    }
}
