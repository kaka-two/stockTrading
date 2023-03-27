package com.kakas.stockTrading.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

@Data
public class EventDetail {
    @TableId(value="id", type= IdType.AUTO)
    private Long Id;

    private Long sequenceId;

    private Long previousId;

    private String data;

    private Long createAt;


}
