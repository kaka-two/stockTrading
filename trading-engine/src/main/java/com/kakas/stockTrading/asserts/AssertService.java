package com.kakas.stockTrading.asserts;

import com.kakas.stockTrading.enums.AssertType;
import com.kakas.stockTrading.enums.TransferType;
import com.kakas.stockTrading.pojo.Assert;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class AssertService {
    // 用户id,资产类型,资产的数据结构表
    ConcurrentMap<Long, ConcurrentMap<AssertType, Assert>> userAsserts = new ConcurrentHashMap<>();

    // 转账
    public boolean transfer(Long fromUserId, Long toUserId, AssertType assertType, TransferType transferType, BigDecimal num) {
        return tryTransfer(fromUserId, toUserId, assertType, transferType, num, false);
    }
    // root用户转账
    public boolean tryTransfer(Long fromUserId, Long toUserId, AssertType assertType, TransferType transferType, BigDecimal num, boolean isRoot){
        if (num.signum() < 0) {
            throw new IllegalArgumentException("Transfer negative amount");
        }
        Assert fromUserAssert = getAssert(fromUserId, assertType);
        if (fromUserAssert == null) {
            fromUserAssert = initAssert(fromUserId, assertType);
        }
        Assert toUserAssert = getAssert(toUserId, assertType);
        if (toUserAssert == null) {
            toUserAssert = initAssert(toUserId, assertType);
        }
        boolean isSuccess = switch (transferType) {
            case AVAILABLE_TO_AVAILABLE -> {
                if (!isRoot && fromUserAssert.getAvailable().compareTo(num) < 0) {
                    yield false;
                }
                fromUserAssert.setAvailable(fromUserAssert.getAvailable().subtract(num));
                toUserAssert.setAvailable(toUserAssert.getAvailable().add(num));
                yield true;
            }
            case AVAILABLE_TO_FROZEN-> {
                if (!isRoot && fromUserAssert.getAvailable().compareTo(num) < 0) {
                    yield false;
                }
                fromUserAssert.setAvailable(fromUserAssert.getAvailable().subtract(num));
                toUserAssert.setFrozen(toUserAssert.getFrozen().add(num));
                yield true;
            }
            case FROZEN_TO_AVAILABLE -> {
                if (!isRoot && fromUserAssert.getAvailable().compareTo(num) < 0) {
                    yield false;
                }
                fromUserAssert.setFrozen(fromUserAssert.getFrozen().subtract(num));
                toUserAssert.setAvailable(toUserAssert.getAvailable().add(num));
                yield true;
            }
            default -> {
                throw new IllegalArgumentException("Unknown transfer type");
            }
        };
        return isSuccess;
    }

    // 冻结
    public boolean freeze(Long userId, AssertType assertType, BigDecimal num) {
        if (num.signum() < 0) {
            throw new IllegalArgumentException("Transfer negative amount");
        }
        return transfer(userId, userId, assertType, TransferType.AVAILABLE_TO_FROZEN, num);
    }

    // 解冻
    public boolean unfreeze(Long userId, AssertType assertType, BigDecimal num) {
        if (num.signum() < 0) {
            throw new IllegalArgumentException("Transfer negative amount");
        }
        return transfer(userId, userId, assertType, TransferType.FROZEN_TO_AVAILABLE, num);
    }

    // 初始化用户资产
    public Assert initAssert(Long userId, AssertType assertType) {
        ConcurrentMap<AssertType, Assert> map = getAsserts(userId);
        if (map == null) {
            map = new ConcurrentHashMap<>();
            userAsserts.put(userId, map);
        }
        Assert zeroAssert = new Assert();
        map.put(assertType, zeroAssert);
        return zeroAssert;
    }

    // 获取用户某个类型的资产
    public Assert getAssert(Long userId, AssertType assertType) {
        ConcurrentMap<AssertType, Assert> map = getAsserts(userId);
        if (map == null) {
            return null;
        }
        return map.get(assertType);
    }

    // 获取用户所有资产
    public ConcurrentMap<AssertType, Assert> getAsserts(Long userId) {
        return userAsserts.get(userId);
    }

}
