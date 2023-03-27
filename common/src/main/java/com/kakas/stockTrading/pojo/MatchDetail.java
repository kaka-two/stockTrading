package com.kakas.stockTrading.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.kakas.stockTrading.enums.Direction;
import com.kakas.stockTrading.enums.MatchType;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class MatchDetail implements Comparable<MatchDetail>{
    @TableId(value="id", type= IdType.AUTO)
    private Long id;

    private Long sequenceId;

    private Long orderId;

    private Long counterOrderId;

    private Long userId;

    private Long counterUserId;

    private MatchType type;

    private Direction direction;

    private BigDecimal price;

    private BigDecimal quantity;

    private Long createdAt;

    @Override
    public int compareTo(MatchDetail o) {
        int cmp = orderId.compareTo(o.orderId);
        if (cmp == 0) {
            cmp = counterOrderId.compareTo(o.counterOrderId);
        }
        return cmp;
    }

    public static MatchDetail createMatchDetail(Long sequenceId, Long createdAt, MatchRecord record, Boolean forTaker) {
        MatchDetail matchDetail = new MatchDetail();
        matchDetail.sequenceId = sequenceId;
        matchDetail.orderId = forTaker ? record.takerOrder().getOrderId() : record.makerOrder().getOrderId();
        matchDetail.counterOrderId = forTaker ? record.makerOrder().getOrderId() : record.takerOrder().getOrderId();
        matchDetail.userId = forTaker ? record.takerOrder().getUserId() : record.makerOrder().getUserId();
        matchDetail.counterUserId = forTaker ? record.makerOrder().getUserId() : record.takerOrder().getUserId();
        matchDetail.type = forTaker ? MatchType.TAKER : MatchType.MAKER;
        matchDetail.direction = forTaker ? record.takerOrder().getDirection() :record.makerOrder().getDirection();
        matchDetail.price = record.price();
        matchDetail.quantity = record.quantity();
        matchDetail.createdAt = createdAt;
        return matchDetail;
    }
}
