package com.kakas.stockTrading.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.kakas.stockTrading.enums.Direction;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class Tick {
    @TableId(value="id", type= IdType.AUTO)
    private Long id;

    private Long sequenceId;

    private Long takerOrderId;

    private Long makerOrderId;

    /**
     * Bit for taker direction: 1=Buy, 0=Sell.
     */
    private Boolean takerDirection;

    private BigDecimal price;

    private BigDecimal quantity;

    private Long createdAt;

    public static Tick createTick(Long sequenceId, Long createdAt, MatchRecord record) {
        Tick tick = new Tick();
        tick.sequenceId = sequenceId;
        tick.takerOrderId = record.takerOrder().getOrderId();
        tick.makerOrderId = record.makerOrder().getOrderId();
        tick.takerDirection = record.takerOrder().getDirection() == Direction.BUY;
        tick.price = record.price();
        tick.quantity = record.quantity();
        tick.createdAt = createdAt;
        return tick;
    }
}
