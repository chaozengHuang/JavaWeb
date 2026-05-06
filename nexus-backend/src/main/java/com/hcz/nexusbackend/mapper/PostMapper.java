package com.hcz.nexusbackend.mapper;

import com.hcz.nexusbackend.entity.Post;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface PostMapper {

    @Insert("INSERT INTO post (user_id, title, content) VALUES (#{userId}, #{title}, #{content})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Post post);

    @Select("SELECT id, user_id, title, content, view_count, like_count, create_time FROM post ORDER BY create_time DESC")
    List<Post> findAll();

    @Select("SELECT id, user_id, title, content, view_count, like_count, create_time FROM post WHERE id = #{id}")
    Post findById(Integer id);
}
