package com.kakas.stockTrading.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

/**
 * 事件唯一标识
 */
@Data
public class EventUnique {
    @TableId(value="id", type= IdType.AUTO)
    private Long Id;

    private String uniqueId;

    private Long sequenceId;

    private Long createdAt;

    public static EventUnique create(String uniqueId, Long sequenceId, Long createdAt) {
        EventUnique eventUnique = new EventUnique();
        eventUnique.setUniqueId(uniqueId);
        eventUnique.setSequenceId(sequenceId);
        eventUnique.setCreatedAt(createdAt);
        return eventUnique;
    }
}
