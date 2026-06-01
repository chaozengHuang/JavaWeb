package com.hcz.nexusbackend.controller;

import com.hcz.nexusbackend.common.Result;
import com.hcz.nexusbackend.entity.User;
import com.hcz.nexusbackend.service.ProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    @Autowired
    private ProfileService profileService;

    // ==================== 个人信息 ====================

    @GetMapping
    public Result<Map<String, Object>> getProfile() {
        return Result.success(profileService.getProfile());
    }

    @GetMapping("/public/{userId}")
    public Result<Map<String, Object>> getPublicProfile(@PathVariable Long userId) {
        return Result.success(profileService.getPublicProfile(userId));
    }

    @PutMapping("/bio")
    public Result<User> updateBio(@RequestBody Map<String, String> body) {
        String bio = body.getOrDefault("bio", "");
        return Result.success("修改成功", profileService.updateBio(bio));
    }

    @PostMapping("/avatar")
    public Result<String> uploadAvatar(@RequestParam("file") MultipartFile file) {
        String url = profileService.uploadAvatar(file);
        return Result.success("上传成功", url);
    }

    // ==================== 收藏 ====================

    @PostMapping("/favorites/{postId}")
    public Result<Void> toggleFavorite(@PathVariable Long postId) {
        profileService.toggleFavorite(postId);
        return Result.success();
    }

    @DeleteMapping("/favorites/{postId}")
    public Result<Void> removeFavorite(@PathVariable Long postId) {
        profileService.toggleFavorite(postId);
        return Result.success();
    }

    @GetMapping("/favorites")
    public Result<Map<String, Object>> getFavorites(
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size) {
        return Result.success(profileService.getFavorites(page, size));
    }

    @GetMapping("/favorites/{postId}/status")
    public Result<Boolean> isFavorited(@PathVariable Long postId) {
        return Result.success(profileService.isFavorited(postId));
    }

    // ==================== 点赞 ====================

    @PostMapping("/likes/{postId}")
    public Result<Void> toggleLike(@PathVariable Long postId) {
        profileService.toggleLike(postId);
        return Result.success();
    }

    @DeleteMapping("/likes/{postId}")
    public Result<Void> removeLike(@PathVariable Long postId) {
        profileService.toggleLike(postId);
        return Result.success();
    }

    @GetMapping("/likes")
    public Result<Map<String, Object>> getLikes(
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size) {
        return Result.success(profileService.getLikes(page, size));
    }

    @GetMapping("/likes/{postId}/status")
    public Result<Boolean> isLiked(@PathVariable Long postId) {
        return Result.success(profileService.isLiked(postId));
    }

    // ==================== 浏览历史 ====================

    @PostMapping("/history/{postId}")
    public Result<Void> recordBrowse(@PathVariable Long postId) {
        profileService.recordBrowse(postId);
        return Result.success();
    }

    @GetMapping("/history")
    public Result<Map<String, Object>> getHistory(
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size) {
        return Result.success(profileService.getHistory(page, size));
    }
}
