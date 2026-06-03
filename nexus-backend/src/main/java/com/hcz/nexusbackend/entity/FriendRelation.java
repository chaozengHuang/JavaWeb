package com.hcz.nexusbackend.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("friend_relation")
public class FriendRelation {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private Long friendId;
    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    // 非数据库字段 - 仅用于返回好友信息
    @TableField(exist = false)
    private String friendUsername;

    @TableField(exist = false)
    private String friendAvatar;

    @TableField(exist = false)
    private Boolean online;
}
