package com.hcz.nexusbackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hcz.nexusbackend.entity.FriendRelation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface FriendRelationMapper extends BaseMapper<FriendRelation> {

    @Select("SELECT fr.* FROM friend_relation fr " +
            "WHERE fr.status = 'ACCEPTED' " +
            "AND (fr.user_id = #{userId} OR fr.friend_id = #{userId})")
    List<FriendRelation> selectAcceptedFriends(@Param("userId") Long userId);

    @Select("SELECT fr.* FROM friend_relation fr " +
            "WHERE fr.friend_id = #{userId} AND fr.status = 'PENDING'")
    List<FriendRelation> selectPendingRequests(@Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM friend_relation " +
            "WHERE friend_id = #{userId} AND status = 'PENDING'")
    int countPendingRequests(@Param("userId") Long userId);
}
