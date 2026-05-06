package com.hcz.nexusbackend.service;

import com.hcz.nexusbackend.entity.Comment;
import com.hcz.nexusbackend.mapper.CommentMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CommentService {

    @Autowired
    private CommentMapper commentMapper;

    public Comment create(Integer postId, Integer userId, String content) {
        Comment comment = new Comment();
        comment.setPostId(postId);
        comment.setUserId(userId);
        comment.setContent(content);
        commentMapper.insert(comment);
        return comment;
    }

    public List<Comment> list(Integer postId) {
        return commentMapper.findByPostId(postId);
    }
}
