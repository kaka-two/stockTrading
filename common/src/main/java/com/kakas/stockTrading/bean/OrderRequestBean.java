package com.kakas.stockTrading.bean;

import com.kakas.stockTrading.enums.ApiError;
import com.kakas.stockTrading.enums.Direction;
import lombok.Data;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Data
public class OrderRequestBean {

    private Direction direction;

    private BigDecimal price;

    private BigDecimal quantity;

    public void validate() {
        // direction:
        if (this.direction == null) {
            throw new RuntimeException(ApiError.CANCEL_ORDER_FAILED.name() + "direction" + "direction is required.");
        }
        // price:
        if (this.price == null) {
            throw new RuntimeException(ApiError.CANCEL_ORDER_FAILED.name() + "price" + "price is required.");
        }
        this.price = this.price.setScale(2, RoundingMode.DOWN);
        if (this.price.signum() <= 0) {
            throw new RuntimeException(ApiError.CANCEL_ORDER_FAILED.name() + "price" + "price must be positive.");
        }
        // quantity:
        if (this.quantity == null) {
            throw new RuntimeException(ApiError.CANCEL_ORDER_FAILED.name() +  "quantity" + "quantity is required.");
        }
        this.quantity = this.quantity.setScale(2, RoundingMode.DOWN);
        if (this.quantity.signum() <= 0) {
            throw new RuntimeException(ApiError.CANCEL_ORDER_FAILED.name() +  "quantity" + "quantity must be positive.");
        }
    }
}
