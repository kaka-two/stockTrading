package com.kakas.stockTrading.message.event;

import com.kakas.stockTrading.enums.AssertType;
import com.kakas.stockTrading.message.Message;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 转账事件，发送给交易引擎
 */
@Data
public class TransferEvent  extends Event  {

    private Long fromUserId;

    private Long toUserId;

    private AssertType assertType;

    private BigDecimal amount;

    private boolean isRoot;
}
