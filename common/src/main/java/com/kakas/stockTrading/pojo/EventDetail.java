package com.kakas.stockTrading.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.kakas.stockTrading.message.event.Event;
import lombok.Data;

/**
 * 事件详情
 */
@Data
public class EventDetail {
    @TableId(value="id", type= IdType.AUTO)
    private Long id;

    private Long sequenceId;

    private Long previousId;

    private String data;

    private Long createdAt;

    public static EventDetail create(Event event, String data) {
        EventDetail eventDetail = new EventDetail();
        eventDetail.setSequenceId(event.getSequenceId());
        eventDetail.setPreviousId(event.getPreviousId());
        eventDetail.setData(data);
        eventDetail.setCreatedAt(event.getCreatedAt());
        return eventDetail;
    }

}
