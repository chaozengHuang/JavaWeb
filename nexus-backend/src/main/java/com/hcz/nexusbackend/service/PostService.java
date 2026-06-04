package com.hcz.nexusbackend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hcz.nexusbackend.dto.PostCreateDTO;
import com.hcz.nexusbackend.dto.PostUpdateDTO;
import com.hcz.nexusbackend.entity.Comment;
import com.hcz.nexusbackend.entity.Post;
import com.hcz.nexusbackend.entity.PostFavorite;
import com.hcz.nexusbackend.entity.PostLike;
import com.hcz.nexusbackend.entity.User;
import com.hcz.nexusbackend.entity.UserBoardRelation;
import com.hcz.nexusbackend.exception.BusinessException;
import com.hcz.nexusbackend.mapper.CommentMapper;
import com.hcz.nexusbackend.mapper.PostFavoriteMapper;
import com.hcz.nexusbackend.mapper.PostLikeMapper;
import com.hcz.nexusbackend.mapper.PostMapper;
import com.hcz.nexusbackend.mapper.UserBoardRelationMapper;
import com.hcz.nexusbackend.mapper.UserMapper;
import com.hcz.nexusbackend.util.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PostService {

    @Autowired
    private PostMapper postMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PostLikeMapper postLikeMapper;

    @Autowired
    private PostFavoriteMapper postFavoriteMapper;

    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private UserBoardRelationMapper boardRelationMapper;

    @Autowired
    private PermissionService permissionService;

    public Post create(PostCreateDTO dto) {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            throw new BusinessException(401, "请先登录");
        }

        // 检查是否在目标吧被禁言
        Long boardId = dto.getBoardId() != null ? dto.getBoardId() : 1L;
        UserBoardRelation rel = boardRelationMapper.selectOne(
                new LambdaQueryWrapper<UserBoardRelation>()
                        .eq(UserBoardRelation::getUserId, userId)
                        .eq(UserBoardRelation::getBoardId, boardId));
        if (rel != null && "MUTED".equals(rel.getBoardRole())) {
            throw new BusinessException("您在该吧已被禁言，无法发帖");
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
        post.setStatus("ACTIVE");
        postMapper.insert(post);
        // 发帖+5活跃度
        if (rel != null) {
            UserBoardRelation update = new UserBoardRelation();
            update.setId(rel.getId());
            update.setActivityPoints((rel.getActivityPoints() != null ? rel.getActivityPoints() : 0) + 5);
            boardRelationMapper.updateById(update);
        }
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
            wrapper.ne(Post::getStatus, "HIDDEN");
            wrapper.ne(Post::getStatus, "BLOCKED");
        }
        if (type != null && !type.isEmpty()) {
            wrapper.eq(Post::getType, type);
        }

        wrapper.orderByDesc(Post::getIsGlobalPinned)
               .orderByDesc(Post::getIsPinned)
               .orderByDesc(Post::getId);

        IPage<Post> pageResult = postMapper.selectPage(pageParam, wrapper);
        // 填充作者用户名和统计数量
        for (Post post : pageResult.getRecords()) {
            User user = userMapper.selectById(post.getAuthorId());
            if (user != null) {
                post.setAuthorUsername(user.getUsername());
            }
            post.setLikeCount(Math.toIntExact(postLikeMapper.selectCount(
                    new LambdaQueryWrapper<PostLike>().eq(PostLike::getPostId, post.getId()))));
            post.setFavoriteCount(Math.toIntExact(postFavoriteMapper.selectCount(
                    new LambdaQueryWrapper<PostFavorite>().eq(PostFavorite::getPostId, post.getId()))));
            post.setCommentCount(Math.toIntExact(commentMapper.selectCount(
                    new LambdaQueryWrapper<Comment>().eq(Comment::getPostId, post.getId()).ne(Comment::getStatus, "DELETED"))));
        }
        return pageResult;
    }

    public Post detail(Long id) {
        Post post = postMapper.selectById(id);
        if (post == null || "DELETED".equals(post.getStatus())) {
            throw new BusinessException("帖子不存在");
        }
        if ("HIDDEN".equals(post.getStatus()) || "BLOCKED".equals(post.getStatus())) {
            throw new BusinessException("帖子已被隐藏");
        }
        // 填充作者用户名
        User user = userMapper.selectById(post.getAuthorId());
        if (user != null) {
            post.setAuthorUsername(user.getUsername());
        }
        // 填充统计数量
        post.setLikeCount(Math.toIntExact(postLikeMapper.selectCount(
                new LambdaQueryWrapper<PostLike>().eq(PostLike::getPostId, id))));
        post.setFavoriteCount(Math.toIntExact(postFavoriteMapper.selectCount(
                new LambdaQueryWrapper<PostFavorite>().eq(PostFavorite::getPostId, id))));
        post.setCommentCount(Math.toIntExact(commentMapper.selectCount(
                new LambdaQueryWrapper<Comment>().eq(Comment::getPostId, id).ne(Comment::getStatus, "DELETED"))));
        return post;
    }

    public IPage<Post> listByUser(Long userId, Integer page, Integer size) {
        Page<Post> pageParam = new Page<>(page != null ? page : 1, size != null ? size : 10);
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Post::getAuthorId, userId)
               .ne(Post::getStatus, "DELETED")
               .ne(Post::getStatus, "HIDDEN")
               .ne(Post::getStatus, "BLOCKED")
               .orderByDesc(Post::getId);
        IPage<Post> pageResult = postMapper.selectPage(pageParam, wrapper);
        // 填充作者用户名和统计数量
        for (Post post : pageResult.getRecords()) {
            User user = userMapper.selectById(post.getAuthorId());
            if (user != null) {
                post.setAuthorUsername(user.getUsername());
            }
            post.setLikeCount(Math.toIntExact(postLikeMapper.selectCount(
                    new LambdaQueryWrapper<PostLike>().eq(PostLike::getPostId, post.getId()))));
            post.setFavoriteCount(Math.toIntExact(postFavoriteMapper.selectCount(
                    new LambdaQueryWrapper<PostFavorite>().eq(PostFavorite::getPostId, post.getId()))));
            post.setCommentCount(Math.toIntExact(commentMapper.selectCount(
                    new LambdaQueryWrapper<Comment>().eq(Comment::getPostId, post.getId()).ne(Comment::getStatus, "DELETED"))));
        }
        return pageResult;
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
        Post post = postMapper.selectById(id);
        if (post == null) {
            throw new BusinessException("帖子不存在");
        }
        // 允许吧主/管理员/系统管理员操作
        permissionService.checkBoardPermission(post.getBoardId(), List.of("OWNER", "ADMIN"));
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
        Post post = postMapper.selectById(id);
        if (post == null) {
            throw new BusinessException("帖子不存在");
        }
        // 允许吧主/管理员/系统管理员操作
        permissionService.checkBoardPermission(post.getBoardId(), List.of("OWNER", "ADMIN"));
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
