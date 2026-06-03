package com.hcz.nexusbackend.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("recharge_order")
public class RechargeOrder {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private String orderNo;
    private BigDecimal amount;
    private Integer points;
    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    private LocalDateTime completedAt;
}
