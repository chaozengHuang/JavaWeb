package com.hcz.nexusbackend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hcz.nexusbackend.dto.CommentCreateDTO;
import com.hcz.nexusbackend.entity.Comment;
import com.hcz.nexusbackend.entity.Post;
import com.hcz.nexusbackend.entity.User;
import com.hcz.nexusbackend.exception.BusinessException;
import com.hcz.nexusbackend.mapper.CommentMapper;
import com.hcz.nexusbackend.mapper.PostMapper;
import com.hcz.nexusbackend.mapper.UserMapper;
import com.hcz.nexusbackend.util.SecurityUtils;
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

    public Comment create(CommentCreateDTO dto) {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            throw new BusinessException(401, "请先登录");
        }

        Post post = postMapper.selectById(dto.getPostId());
        if (post == null || "DELETED".equals(post.getStatus())) {
            throw new BusinessException("帖子不存在");
        }

        if (dto.getParentCommentId() != null) {
            Comment parent = commentMapper.selectById(dto.getParentCommentId());
            if (parent == null) {
                throw new BusinessException("回复的评论不存在");
            }
        }

        Comment comment = new Comment();
        comment.setPostId(dto.getPostId());
        comment.setAuthorId(userId);
        comment.setContent(dto.getContent());
        comment.setParentCommentId(dto.getParentCommentId());
        comment.setIsAccepted(0);
        comment.setStatus("NORMAL");
        commentMapper.insert(comment);
        return comment;
    }

    public List<Comment> list(Long postId) {
        return commentMapper.selectList(
                new LambdaQueryWrapper<Comment>()
                        .eq(Comment::getPostId, postId)
                        .ne(Comment::getStatus, "DELETED")
                        .orderByAsc(Comment::getId));
    }

    public void delete(Long id) {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            throw new BusinessException(401, "请先登录");
        }

        Comment comment = commentMapper.selectById(id);
        if (comment == null) {
            throw new BusinessException("评论不存在");
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        if (!comment.getAuthorId().equals(userId) && !"SYS_ADMIN".equals(user.getGlobalRole())) {
            throw new BusinessException(403, "无权删除");
        }

        Comment update = new Comment();
        update.setId(id);
        update.setStatus("DELETED");
        commentMapper.updateById(update);
    }

    public void accept(Long id) {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            throw new BusinessException(401, "请先登录");
        }

        Comment comment = commentMapper.selectById(id);
        if (comment == null) {
            throw new BusinessException("评论不存在");
        }

        Post post = postMapper.selectById(comment.getPostId());
        if (post == null) {
            throw new BusinessException("帖子不存在");
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        if (!post.getAuthorId().equals(userId) && !"SYS_ADMIN".equals(user.getGlobalRole())) {
            throw new BusinessException(403, "只有帖子作者或管理员可以采纳评论");
        }

        Comment update = new Comment();
        update.setId(id);
        update.setIsAccepted(1);
        commentMapper.updateById(update);

        if (post.getRewardPoints() != null && post.getRewardPoints() > 0) {
            User commentAuthor = userMapper.selectById(comment.getAuthorId());
            if (commentAuthor != null) {
                commentAuthor.setPoints(commentAuthor.getPoints() + post.getRewardPoints());
                userMapper.updateById(commentAuthor);
            }
            Post reset = new Post();
            reset.setId(post.getId());
            reset.setRewardPoints(0);
            postMapper.updateById(reset);
        }
    }

    public IPage<Comment> adminList(String keyword, String status, Long postId, Integer page, Integer size) {
        checkAdminPermission();
        Page<Comment> pageParam = new Page<>(page != null ? page : 1, size != null ? size : 10);
        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<>();

        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(Comment::getContent, keyword);
        }
        if (status != null && !status.isEmpty()) {
            wrapper.eq(Comment::getStatus, status);
        }
        if (postId != null) {
            wrapper.eq(Comment::getPostId, postId);
        }

        wrapper.orderByDesc(Comment::getId);
        return commentMapper.selectPage(pageParam, wrapper);
    }

    public void adminDelete(Long id) {
        checkAdminPermission();
        Comment comment = commentMapper.selectById(id);
        if (comment == null) {
            throw new BusinessException("评论不存在");
        }
        commentMapper.deleteById(id);
    }

    private void checkAdminPermission() {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            throw new BusinessException(401, "请先登录");
        }
        User user = userMapper.selectById(userId);
        if (user == null || !"SYS_ADMIN".equals(user.getGlobalRole())) {
            throw new BusinessException(403, "无管理员权限");
        }
    }
}
