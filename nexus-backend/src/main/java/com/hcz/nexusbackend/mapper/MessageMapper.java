package com.hcz.nexusbackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hcz.nexusbackend.entity.Message;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface MessageMapper extends BaseMapper<Message> {

    @Select("SELECT * FROM message WHERE " +
            "((sender_id = #{userId1} AND receiver_id = #{userId2}) OR " +
            "(sender_id = #{userId2} AND receiver_id = #{userId1})) " +
            "ORDER BY create_time ASC")
    List<Message> selectHistory(@Param("userId1") Long userId1, @Param("userId2") Long userId2);

    @Select("SELECT COUNT(*) FROM message WHERE receiver_id = #{userId} AND status = 0")
    int countUnread(@Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM message WHERE receiver_id = #{receiverId} AND sender_id = #{senderId} AND status = 0")
    int countUnreadBySender(@Param("receiverId") Long receiverId, @Param("senderId") Long senderId);

    @Select("SELECT DISTINCT CASE " +
            "WHEN sender_id = #{userId} THEN receiver_id " +
            "ELSE sender_id END AS chat_user_id " +
            "FROM message " +
            "WHERE sender_id = #{userId} OR receiver_id = #{userId}")
    List<Long> selectChatUserIds(@Param("userId") Long userId);

    @Update("UPDATE message SET status = 1 WHERE receiver_id = #{receiverId} AND sender_id = #{senderId} AND status = 0")
    int markAsRead(@Param("senderId") Long senderId, @Param("receiverId") Long receiverId);
}
