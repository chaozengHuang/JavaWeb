package com.hcz.nexusbackend.service;

import com.hcz.nexusbackend.entity.User;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

public interface ProfileService {

    // 个人信息
    Map<String, Object> getProfile();

    User updateBio(String bio);

    String uploadAvatar(MultipartFile file);

    // 收藏
    void toggleFavorite(Long postId);

    boolean isFavorited(Long postId);

    Map<String, Object> getFavorites(Integer page, Integer size);

    // 点赞
    void toggleLike(Long postId);

    boolean isLiked(Long postId);

    Map<String, Object> getLikes(Integer page, Integer size);

    // 浏览历史
    void recordBrowse(Long postId);

    Map<String, Object> getHistory(Integer page, Integer size);
}
