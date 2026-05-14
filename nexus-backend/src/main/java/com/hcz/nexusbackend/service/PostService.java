package com.hcz.nexusbackend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hcz.nexusbackend.dto.PostCreateDTO;
import com.hcz.nexusbackend.dto.PostUpdateDTO;
import com.hcz.nexusbackend.entity.Post;
import com.hcz.nexusbackend.entity.User;
import com.hcz.nexusbackend.exception.BusinessException;
import com.hcz.nexusbackend.mapper.PostMapper;
import com.hcz.nexusbackend.mapper.UserMapper;
import com.hcz.nexusbackend.util.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PostService {

    @Autowired
    private PostMapper postMapper;

    @Autowired
    private UserMapper userMapper;

    public Post create(PostCreateDTO dto) {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            throw new BusinessException(401, "请先登录");
        }

        if (dto.getRewardPoints() != null && dto.getRewardPoints() > 0) {
            User user = userMapper.selectById(userId);
            if (user == null || user.getPoints() < dto.getRewardPoints()) {
                throw new BusinessException("积分不足");
            }
            user.setPoints(user.getPoints() - dto.getRewardPoints());
            userMapper.updateById(user);
        }

        Post post = new Post();
        post.setAuthorId(userId);
        post.setTitle(dto.getTitle());
        post.setContent(dto.getContent());
        post.setBoardId(dto.getBoardId() != null ? dto.getBoardId() : 1L);
        post.setType(dto.getType());
        post.setRewardPoints(dto.getRewardPoints() != null ? dto.getRewardPoints() : 0);
        post.setIsPinned(0);
        post.setIsGlobalPinned(0);
        post.setIsFeatured(0);
        post.setStatus("NORMAL");
        postMapper.insert(post);
        return post;
    }

    public IPage<Post> list(Long boardId, String keyword, String status, String type, Integer page, Integer size) {
        Page<Post> pageParam = new Page<>(page != null ? page : 1, size != null ? size : 10);
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<>();

        if (boardId != null) {
            wrapper.eq(Post::getBoardId, boardId);
        }
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(Post::getTitle, keyword).or().like(Post::getContent, keyword));
        }
        if (status != null && !status.isEmpty()) {
            wrapper.eq(Post::getStatus, status);
        } else {
            wrapper.ne(Post::getStatus, "DELETED");
        }
        if (type != null && !type.isEmpty()) {
            wrapper.eq(Post::getType, type);
        }

        wrapper.orderByDesc(Post::getIsGlobalPinned)
               .orderByDesc(Post::getIsPinned)
               .orderByDesc(Post::getId);

        return postMapper.selectPage(pageParam, wrapper);
    }

    public Post detail(Long id) {
        Post post = postMapper.selectById(id);
        if (post == null || "DELETED".equals(post.getStatus())) {
            throw new BusinessException("帖子不存在");
        }
        return post;
    }

    public void update(Long id, PostUpdateDTO dto) {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            throw new BusinessException(401, "请先登录");
        }

        Post post = postMapper.selectById(id);
        if (post == null) {
            throw new BusinessException("帖子不存在");
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        if (!post.getAuthorId().equals(userId) && !"SYS_ADMIN".equals(user.getGlobalRole())) {
            throw new BusinessException(403, "无权修改");
        }

        Post update = new Post();
        update.setId(id);
        update.setTitle(dto.getTitle());
        update.setContent(dto.getContent());
        postMapper.updateById(update);
    }

    public void delete(Long id) {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            throw new BusinessException(401, "请先登录");
        }

        Post post = postMapper.selectById(id);
        if (post == null) {
            throw new BusinessException("帖子不存在");
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        if (!post.getAuthorId().equals(userId) && !"SYS_ADMIN".equals(user.getGlobalRole())) {
            throw new BusinessException(403, "无权删除");
        }

        Post update = new Post();
        update.setId(id);
        update.setStatus("DELETED");
        postMapper.updateById(update);
    }

    public void setPin(Long id, Integer pinnedStatus) {
        checkAdminPermission();
        Post post = postMapper.selectById(id);
        if (post == null) {
            throw new BusinessException("帖子不存在");
        }
        Post update = new Post();
        update.setId(id);
        update.setIsPinned(pinnedStatus != null ? pinnedStatus : 1);
        postMapper.updateById(update);
    }

    public void setGlobalPin(Long id, Integer pinnedStatus) {
        checkAdminPermission();
        Post post = postMapper.selectById(id);
        if (post == null) {
            throw new BusinessException("帖子不存在");
        }
        Post update = new Post();
        update.setId(id);
        update.setIsGlobalPinned(pinnedStatus != null ? pinnedStatus : 1);
        postMapper.updateById(update);
    }

    public void setFeature(Long id, Integer featuredStatus) {
        checkAdminPermission();
        Post post = postMapper.selectById(id);
        if (post == null) {
            throw new BusinessException("帖子不存在");
        }
        Post update = new Post();
        update.setId(id);
        update.setIsFeatured(featuredStatus != null ? featuredStatus : 1);
        postMapper.updateById(update);
    }

    public IPage<Post> adminList(String keyword, String status, String type, Long boardId, Integer page, Integer size) {
        checkAdminPermission();
        Page<Post> pageParam = new Page<>(page != null ? page : 1, size != null ? size : 10);
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<>();

        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(Post::getTitle, keyword).or().like(Post::getContent, keyword));
        }
        if (status != null && !status.isEmpty()) {
            wrapper.eq(Post::getStatus, status);
        }
        if (type != null && !type.isEmpty()) {
            wrapper.eq(Post::getType, type);
        }
        if (boardId != null) {
            wrapper.eq(Post::getBoardId, boardId);
        }

        wrapper.orderByDesc(Post::getId);
        return postMapper.selectPage(pageParam, wrapper);
    }

    public void adminUpdateStatus(Long id, String status) {
        checkAdminPermission();
        Post post = postMapper.selectById(id);
        if (post == null) {
            throw new BusinessException("帖子不存在");
        }
        Post update = new Post();
        update.setId(id);
        update.setStatus(status);
        postMapper.updateById(update);
    }

    public void adminDelete(Long id) {
        checkAdminPermission();
        Post post = postMapper.selectById(id);
        if (post == null) {
            throw new BusinessException("帖子不存在");
        }
        postMapper.deleteById(id);
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
