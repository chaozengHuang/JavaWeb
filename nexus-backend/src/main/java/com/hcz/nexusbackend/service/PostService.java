package com.hcz.nexusbackend.service;

import com.hcz.nexusbackend.entity.Post;
import com.hcz.nexusbackend.mapper.PostMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PostService {

    @Autowired
    private PostMapper postMapper;

    public Post create(Integer userId, String title, String content) {
        Post post = new Post();
        post.setUserId(userId);
        post.setTitle(title);
        post.setContent(content);
        postMapper.insert(post);
        return post;
    }

    public List<Post> list() {
        return postMapper.findAll();
    }

    public Post detail(Integer postId) {
        return postMapper.findById(postId);
    }
}
