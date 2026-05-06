package com.hcz.nexusbackend.mapper;

import com.hcz.nexusbackend.entity.Comment;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface CommentMapper {

    @Insert("INSERT INTO comment (post_id, user_id, content) VALUES (#{postId}, #{userId}, #{content})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Comment comment);

    @Select("SELECT id, post_id, user_id, content, create_time FROM comment WHERE post_id = #{postId} ORDER BY create_time ASC")
    List<Comment> findByPostId(Integer postId);
}
