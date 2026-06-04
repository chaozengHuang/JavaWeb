package com.hcz.nexusbackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hcz.nexusbackend.entity.Board;
import com.hcz.nexusbackend.entity.BrowseHistory;
import com.hcz.nexusbackend.entity.Post;
import com.hcz.nexusbackend.entity.PostFavorite;
import com.hcz.nexusbackend.entity.PostLike;
import com.hcz.nexusbackend.entity.User;
import com.hcz.nexusbackend.entity.UserBoardRelation;
import com.hcz.nexusbackend.exception.BusinessException;
import com.hcz.nexusbackend.mapper.BoardMapper;
import com.hcz.nexusbackend.mapper.BrowseHistoryMapper;
import com.hcz.nexusbackend.mapper.PostFavoriteMapper;
import com.hcz.nexusbackend.mapper.PostLikeMapper;
import com.hcz.nexusbackend.mapper.PostMapper;
import com.hcz.nexusbackend.mapper.UserBoardRelationMapper;
import com.hcz.nexusbackend.mapper.UserMapper;
import com.hcz.nexusbackend.service.ProfileService;
import com.hcz.nexusbackend.util.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ProfileServiceImpl implements ProfileService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PostMapper postMapper;

    @Autowired
    private PostFavoriteMapper favoriteMapper;

    @Autowired
    private PostLikeMapper likeMapper;

    @Autowired
    private BrowseHistoryMapper historyMapper;

    @Autowired
    private UserBoardRelationMapper boardRelationMapper;

    @Autowired
    private BoardMapper boardMapper;

    @Value("${file.upload.dir:uploads}")
    private String uploadDir;

    private Long getCurrentUserId() {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            throw new BusinessException(401, "请先登录");
        }
        return userId;
    }

    // ==================== 个人信息 ====================

    @Override
    public Map<String, Object> getProfile() {
        Long userId = getCurrentUserId();
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        int favoriteCount = Math.toIntExact(favoriteMapper.selectCount(
                new LambdaQueryWrapper<PostFavorite>().eq(PostFavorite::getUserId, userId)));
        int likeCount = Math.toIntExact(likeMapper.selectCount(
                new LambdaQueryWrapper<PostLike>().eq(PostLike::getUserId, userId)));
        int postCount = Math.toIntExact(postMapper.selectCount(
                new LambdaQueryWrapper<Post>().eq(Post::getAuthorId, userId).ne(Post::getStatus, "DELETED")));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("user", user);
        result.put("stats", Map.of(
                "favoriteCount", favoriteCount,
                "likeCount", likeCount,
                "postCount", postCount
        ));
        return result;
    }

    @Override
    public Map<String, Object> getPublicProfile(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        int postCount = Math.toIntExact(postMapper.selectCount(
                new LambdaQueryWrapper<Post>().eq(Post::getAuthorId, userId).ne(Post::getStatus, "DELETED")));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("user", user);
        result.put("stats", Map.of(
                "favoriteCount", 0,
                "likeCount", 0,
                "postCount", postCount
        ));
        return result;
    }

    @Override
    public User updateBio(String bio) {
        Long userId = getCurrentUserId();
        User user = new User();
        user.setId(userId);
        user.setBio(bio);
        userMapper.updateById(user);
        return userMapper.selectById(userId);
    }

    @Override
    public User updateProfile(Map<String, String> fields) {
        Long userId = getCurrentUserId();
        User user = new User();
        user.setId(userId);
        if (fields.containsKey("email")) user.setEmail(fields.get("email"));
        if (fields.containsKey("phone")) user.setPhone(fields.get("phone"));
        if (fields.containsKey("jobNature")) user.setJobNature(fields.get("jobNature"));
        if (fields.containsKey("location")) user.setLocation(fields.get("location"));
        if (fields.containsKey("bio")) user.setBio(fields.get("bio"));
        userMapper.updateById(user);
        return userMapper.selectById(userId);
    }

    @Override
    public String uploadAvatar(MultipartFile file) {
        Long userId = getCurrentUserId();
        if (file.isEmpty()) {
            throw new BusinessException("文件不能为空");
        }
        String originalName = file.getOriginalFilename();
        String ext = "";
        if (originalName != null && originalName.contains(".")) {
            ext = originalName.substring(originalName.lastIndexOf("."));
        }
        String fileName = "avatar_" + userId + "_" + UUID.randomUUID().toString().substring(0, 8) + ext;

        // 使用绝对路径
        File dir = new File(uploadDir + "/avatars");
        if (!dir.exists()) {
            boolean created = dir.mkdirs();
            if (!created) {
                log.error("创建头像目录失败: {}", dir.getAbsolutePath());
                throw new BusinessException("头像上传失败：无法创建目录");
            }
        }
        log.info("头像上传目录: {}", dir.getAbsolutePath());

        File destFile = new File(dir, fileName);
        try {
            file.transferTo(destFile);
            log.info("头像文件保存成功: {}", destFile.getAbsolutePath());
        } catch (IOException e) {
            log.error("头像上传失败, dest={}", destFile.getAbsolutePath(), e);
            throw new BusinessException("头像上传失败: " + e.getMessage());
        }

        String avatarUrl = "/uploads/avatars/" + fileName;
        User user = new User();
        user.setId(userId);
        user.setAvatar(avatarUrl);
        userMapper.updateById(user);

        log.info("用户 {} 更新头像: {}", userId, avatarUrl);
        return avatarUrl;
    }

    // ==================== 收藏 ====================

    @Override
    public void toggleFavorite(Long postId) {
        Long userId = getCurrentUserId();
        Post post = postMapper.selectById(postId);
        if (post == null || "DELETED".equals(post.getStatus())) {
            throw new BusinessException("帖子不存在");
        }

        PostFavorite existing = favoriteMapper.selectOne(
                new LambdaQueryWrapper<PostFavorite>()
                        .eq(PostFavorite::getUserId, userId)
                        .eq(PostFavorite::getPostId, postId));
        if (existing != null) {
            favoriteMapper.deleteById(existing.getId());
            log.info("用户 {} 取消收藏帖子 {}", userId, postId);
        } else {
            PostFavorite fav = new PostFavorite();
            fav.setUserId(userId);
            fav.setPostId(postId);
            favoriteMapper.insert(fav);
            // 收藏+1活跃度
            addActivity(userId, post.getBoardId(), 1);
            log.info("用户 {} 收藏帖子 {}", userId, postId);
        }
    }

    @Override
    public boolean isFavorited(Long postId) {
        Long userId = getCurrentUserId();
        return favoriteMapper.selectCount(
                new LambdaQueryWrapper<PostFavorite>()
                        .eq(PostFavorite::getUserId, userId)
                        .eq(PostFavorite::getPostId, postId)) > 0;
    }

    @Override
    public Map<String, Object> getFavorites(Integer page, Integer size) {
        Long userId = getCurrentUserId();
        List<Long> postIds = favoriteMapper.selectFavoritePostIds(userId);
        return buildPostListResult(postIds, page, size);
    }

    // ==================== 点赞 ====================

    @Override
    public void toggleLike(Long postId) {
        Long userId = getCurrentUserId();
        Post post = postMapper.selectById(postId);
        if (post == null || "DELETED".equals(post.getStatus())) {
            throw new BusinessException("帖子不存在");
        }

        PostLike existing = likeMapper.selectOne(
                new LambdaQueryWrapper<PostLike>()
                        .eq(PostLike::getUserId, userId)
                        .eq(PostLike::getPostId, postId));
        if (existing != null) {
            likeMapper.deleteById(existing.getId());
            log.info("用户 {} 取消点赞帖子 {}", userId, postId);
        } else {
            PostLike like = new PostLike();
            like.setUserId(userId);
            like.setPostId(postId);
            likeMapper.insert(like);
            // 点赞+1活跃度
            addActivity(userId, post.getBoardId(), 1);
            log.info("用户 {} 点赞帖子 {}", userId, postId);
        }
    }

    @Override
    public boolean isLiked(Long postId) {
        Long userId = getCurrentUserId();
        return likeMapper.selectCount(
                new LambdaQueryWrapper<PostLike>()
                        .eq(PostLike::getUserId, userId)
                        .eq(PostLike::getPostId, postId)) > 0;
    }

    @Override
    public Map<String, Object> getLikes(Integer page, Integer size) {
        Long userId = getCurrentUserId();
        List<Long> postIds = likeMapper.selectLikePostIds(userId);
        return buildPostListResult(postIds, page, size);
    }

    // ==================== 浏览历史 ====================

    @Override
    public void recordBrowse(Long postId) {
        Long userId = getCurrentUserId();
        Post post = postMapper.selectById(postId);
        if (post == null || "DELETED".equals(post.getStatus())) {
            throw new BusinessException("帖子不存在");
        }
        BrowseHistory history = new BrowseHistory();
        history.setUserId(userId);
        history.setPostId(postId);
        historyMapper.insert(history);
    }

    @Override
    public Map<String, Object> getHistory(Integer page, Integer size) {
        Long userId = getCurrentUserId();
        List<Long> postIds = historyMapper.selectHistoryPostIds(userId);
        return buildPostListResult(postIds, page, size);
    }

    @Override
    public void deleteHistoryByPostId(Long postId) {
        Long userId = getCurrentUserId();
        historyMapper.delete(
                new LambdaQueryWrapper<BrowseHistory>()
                        .eq(BrowseHistory::getUserId, userId)
                        .eq(BrowseHistory::getPostId, postId));
        log.info("用户 {} 删除了帖子 {} 的浏览记录", userId, postId);
    }

    @Override
    public void clearHistory() {
        Long userId = getCurrentUserId();
        historyMapper.delete(
                new LambdaQueryWrapper<BrowseHistory>().eq(BrowseHistory::getUserId, userId));
        log.info("用户 {} 清空了浏览历史", userId);
    }

    // ==================== 通用 ====================

    private void addActivity(Long userId, Long boardId, int points) {
        UserBoardRelation rel = boardRelationMapper.selectOne(
                new LambdaQueryWrapper<UserBoardRelation>()
                        .eq(UserBoardRelation::getUserId, userId)
                        .eq(UserBoardRelation::getBoardId, boardId));
        if (rel != null) {
            UserBoardRelation update = new UserBoardRelation();
            update.setId(rel.getId());
            update.setActivityPoints((rel.getActivityPoints() != null ? rel.getActivityPoints() : 0) + points);
            boardRelationMapper.updateById(update);
        }
    }

    private Map<String, Object> buildPostListResult(List<Long> postIds, Integer page, Integer size) {
        int pageNum = page != null ? page : 1;
        int pageSize = size != null ? size : 10;
        int total = postIds.size();
        int fromIndex = (pageNum - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, total);

        List<Post> posts = new ArrayList<>();
        if (fromIndex < total) {
            List<Long> pageIds = postIds.subList(fromIndex, toIndex);
            if (!pageIds.isEmpty()) {
                posts = postMapper.selectList(
                        new LambdaQueryWrapper<Post>().in(Post::getId, pageIds));
                Map<Long, Post> postMap = posts.stream().collect(Collectors.toMap(Post::getId, p -> p));
                posts = pageIds.stream()
                        .map(postMap::get)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
            }
        }

        // 填充 boardName
        for (Post p : posts) {
            if (p.getBoardId() != null) {
                Board b = boardMapper.selectById(p.getBoardId());
                if (b != null) {
                    p.setBoardName(b.getName());
                }
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("records", posts);
        result.put("total", total);
        result.put("page", pageNum);
        result.put("size", pageSize);
        return result;
    }
}
