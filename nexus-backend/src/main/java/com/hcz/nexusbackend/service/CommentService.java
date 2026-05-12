package com.hcz.nexusbackend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hcz.nexusbackend.entity.Comment;
import com.hcz.nexusbackend.entity.Post;
import com.hcz.nexusbackend.entity.User;
import com.hcz.nexusbackend.mapper.CommentMapper;
import com.hcz.nexusbackend.mapper.PostMapper;
import com.hcz.nexusbackend.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CommentService {

    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private PostMapper postMapper;

    @Autowired
    private UserMapper userMapper;

    public Comment create(Long postId, Long userId, String content) {
        Comment comment = new Comment();
        comment.setPostId(postId);
        comment.setAuthorId(userId);
        comment.setContent(content);
        commentMapper.insert(comment);
        return comment;
    }

    public List<Comment> list(Long postId) {
        return commentMapper.selectList(
                new LambdaQueryWrapper<Comment>().eq(Comment::getPostId, postId)
                        .orderByAsc(Comment::getId));
    }

    public String accept(Long commentId) {
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            return "评论不存在";
        }
        Post post = postMapper.selectById(comment.getPostId());
        if (post == null) {
            return "帖子不存在";
        }

        Comment update = new Comment();
        update.setId(commentId);
        update.setIsAccepted(1);
        commentMapper.updateById(update);

        if (post.getRewardPoints() != null && post.getRewardPoints() > 0) {
            User author = userMapper.selectById(comment.getAuthorId());
            if (author != null) {
                author.setPoints(author.getPoints() + post.getRewardPoints());
                userMapper.updateById(author);
            }
            Post reset = new Post();
            reset.setId(post.getId());
            reset.setRewardPoints(0);
            postMapper.updateById(reset);
        }
        return "采纳成功";
    }
}
