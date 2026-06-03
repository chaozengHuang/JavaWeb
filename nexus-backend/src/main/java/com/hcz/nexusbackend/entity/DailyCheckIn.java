package com.hcz.nexusbackend.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("daily_check_in")
public class DailyCheckIn {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private LocalDate checkInDate;
    private Integer pointsAwarded;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
