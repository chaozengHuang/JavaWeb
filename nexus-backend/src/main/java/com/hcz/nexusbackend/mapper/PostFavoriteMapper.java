package com.hcz.nexusbackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hcz.nexusbackend.entity.PostFavorite;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface PostFavoriteMapper extends BaseMapper<PostFavorite> {

    @Select("SELECT post_id FROM post_favorite WHERE user_id = #{userId} ORDER BY create_time DESC")
    List<Long> selectFavoritePostIds(@Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM post_favorite WHERE post_id = #{postId}")
    int countByPostId(@Param("postId") Long postId);
}
