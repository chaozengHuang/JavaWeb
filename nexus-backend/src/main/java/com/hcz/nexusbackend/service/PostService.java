package com.hcz.nexusbackend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hcz.nexusbackend.entity.Post;
import com.hcz.nexusbackend.entity.User;
import com.hcz.nexusbackend.mapper.PostMapper;
import com.hcz.nexusbackend.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PostService {

    @Autowired
    private PostMapper postMapper;

    @Autowired
    private UserMapper userMapper;

    public String create(Long userId, String title, String content, Long boardId, Integer rewardPoints) {
        if (rewardPoints != null && rewardPoints > 0) {
            User user = userMapper.selectById(userId);
            if (user == null || user.getPoints() < rewardPoints) {
                return "积分不足";
            }
            user.setPoints(user.getPoints() - rewardPoints);
            userMapper.updateById(user);
        }
        Post post = new Post();
        post.setAuthorId(userId);
        post.setTitle(title);
        post.setContent(content);
        post.setBoardId(boardId != null ? boardId : 1L);
        post.setRewardPoints(rewardPoints != null ? rewardPoints : 0);
        postMapper.insert(post);
        return "发帖成功";
    }

    public List<Post> list(Long boardId) {
        if (boardId != null) {
            return postMapper.selectList(
                    new LambdaQueryWrapper<Post>().eq(Post::getBoardId, boardId)
                            .orderByDesc(Post::getIsPinned)
                            .orderByDesc(Post::getId));
        }
        return postMapper.selectList(
                new LambdaQueryWrapper<Post>()
                        .orderByDesc(Post::getIsPinned)
                        .orderByDesc(Post::getId));
    }

    public Post detail(Long postId) {
        return postMapper.selectById(postId);
    }

    public String update(Long postId, Long userId, String title, String content) {
        Post post = postMapper.selectById(postId);
        if (post == null) {
            return "帖子不存在";
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            return "用户不存在";
        }
        if (!post.getAuthorId().equals(userId) && !"SYS_ADMIN".equals(user.getGlobalRole())) {
            return "无权修改";
        }
        Post update = new Post();
        update.setId(postId);
        update.setTitle(title);
        update.setContent(content);
        postMapper.updateById(update);
        return "修改成功";
    }

    public String setTop(Long postId, Long userId, Integer status) {
        User user = userMapper.selectById(userId);
        if (user == null || !"SYS_ADMIN".equals(user.getGlobalRole())) {
            return "无管理员权限";
        }
        Post update = new Post();
        update.setId(postId);
        update.setIsPinned(status != null ? status : 1);
        postMapper.updateById(update);
        return "操作成功";
    }

    public String setElite(Long postId, Long userId, Integer status) {
        User user = userMapper.selectById(userId);
        if (user == null || !"SYS_ADMIN".equals(user.getGlobalRole())) {
            return "无管理员权限";
        }
        Post update = new Post();
        update.setId(postId);
        update.setIsFeatured(status != null ? status : 1);
        postMapper.updateById(update);
        return "操作成功";
    }
}
