package com.kakas.stockTrading.web.api;

import com.kakas.stockTrading.bean.OrderRequestBean;
import com.kakas.stockTrading.bean.TransferRequestBean;
import com.kakas.stockTrading.ctx.UserContext;
import com.kakas.stockTrading.enums.UserType;
import com.kakas.stockTrading.message.event.OrderRequestEvent;
import com.kakas.stockTrading.message.event.TransferEvent;
import com.kakas.stockTrading.service.SendEventService;
import com.kakas.stockTrading.util.IdUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/internal")
@Slf4j
public class TradingApiInternalController {
    @Autowired
    SendEventService sendEventService;

    @PostMapping(value = "/transfer", produces = "application/json")
    public Map<String, Boolean> transfer(@RequestBody TransferRequestBean transferRequest) {
        log.info("transfer request: transferId={}, fromUserId={}, toUserId={}, asset={}, amount={}",
                transferRequest.getTransferId(), transferRequest.getFromUserId(), transferRequest.getToUserId(),
                transferRequest.getAsset(), transferRequest.getAmount());
        transferRequest.validate();
        Long userId = UserContext.getRequiredUserId();
        // 创建转账的消息， 允许重复发送通过，通过uniqueId去重
        var event = new TransferEvent();
        event.setUniqueId(transferRequest.getTransferId());
        event.setFromUserId(transferRequest.getFromUserId());
        event.setToUserId(transferRequest.getToUserId());
        event.setAssertType(transferRequest.getAsset());
        event.setAmount(transferRequest.getAmount());
        event.setRoot(transferRequest.getFromUserId() == UserType.ROOT.getUserTypeId());
        // 发送消息
        sendEventService.sendEvent(event);
        log.info("transfer event sent : {}" , event);
        return Map.of("result", true);
    }
}
