package com.hcz.nexusbackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hcz.nexusbackend.entity.PostLike;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface PostLikeMapper extends BaseMapper<PostLike> {

    @Select("SELECT post_id FROM post_like WHERE user_id = #{userId} ORDER BY create_time DESC")
    List<Long> selectLikePostIds(@Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM post_like WHERE post_id = #{postId}")
    int countByPostId(@Param("postId") Long postId);
}
