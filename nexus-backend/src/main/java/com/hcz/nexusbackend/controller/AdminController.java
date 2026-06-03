package com.hcz.nexusbackend.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.hcz.nexusbackend.annotation.RequireAdmin;
import com.hcz.nexusbackend.common.Result;
import com.hcz.nexusbackend.dto.ResetPasswordDTO;
import com.hcz.nexusbackend.dto.UserStatusDTO;
import com.hcz.nexusbackend.entity.AdminLog;
import com.hcz.nexusbackend.entity.Comment;
import com.hcz.nexusbackend.entity.Post;
import com.hcz.nexusbackend.entity.User;
import com.hcz.nexusbackend.service.AdminService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")
@RequireAdmin
public class AdminController {

    @Autowired
    private AdminService adminService;

    // ==================== 用户管理 ====================

    /**
     * 查询用户列表（分页、条件筛选）
     * GET /admin/users?keyword=&status=&role=&page=1&size=10
     */
    @GetMapping("/users")
    public Result<IPage<User>> listUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return Result.success(adminService.listUsers(keyword, status, role, page, size));
    }

    /**
     * 查看用户详情（含发帖数、评论数）
     * GET /admin/users/{userId}
     */
    @GetMapping("/users/{userId}")
    public Result<Map<String, Object>> getUserDetail(@PathVariable Long userId) {
        return Result.success(adminService.getUserDetail(userId));
    }

    /**
     * 管理员重置用户密码
     * PUT /admin/users/{userId}/password
     */
    @PutMapping("/users/{userId}/password")
    public Result<Void> resetUserPassword(@PathVariable Long userId,
                                           @Valid @RequestBody ResetPasswordDTO dto) {
        adminService.resetUserPassword(userId, dto);
        return Result.success("密码重置成功", null);
    }

    /**
     * 封禁/解封用户
     * PUT /admin/users/{userId}/status
     * Body: { "status": "BANNED" } 或 { "status": "ACTIVE" }
     */
    @PutMapping("/users/{userId}/status")
    public Result<Void> updateUserStatus(@PathVariable Long userId,
                                          @Valid @RequestBody UserStatusDTO dto) {
        adminService.updateUserStatus(userId, dto);
        return Result.success("操作成功", null);
    }

    // ==================== 帖子管理 ====================

    /**
     * 管理员查询帖子列表（含作者名、支持所有状态筛选）
     * GET /admin/posts?keyword=&status=&type=&boardId=&authorId=&page=1&size=10
     */
    @GetMapping("/posts")
    public Result<IPage<Map<String, Object>>> listPosts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Long boardId,
            @RequestParam(required = false) Long authorId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return Result.success(adminService.adminListPosts(keyword, status, type, boardId, authorId, page, size));
    }

    /**
     * 查看帖子详情
     * GET /admin/posts/{postId}
     */
    @GetMapping("/posts/{postId}")
    public Result<Post> getPostDetail(@PathVariable Long postId) {
        return Result.success(adminService.getPostDetail(postId));
    }

    /**
     * 修改帖子状态（NORMAL/BLOCKED/DELETED）
     * PUT /admin/posts/{postId}/status?status=BLOCKED
     */
    @PutMapping("/posts/{postId}/status")
    public Result<Void> updatePostStatus(@PathVariable Long postId,
                                          @RequestParam String status) {
        adminService.updatePostStatus(postId, status);
        return Result.success("操作成功", null);
    }

    /**
     * 批量修改帖子状态
     * PUT /admin/posts/batch-status
     * Body: { "ids": [1,2,3], "status": "BLOCKED" }
     */
    @PutMapping("/posts/batch-status")
    public Result<Void> batchUpdatePostStatus(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Integer> rawIds = (List<Integer>) body.get("ids");
        List<Long> ids = rawIds.stream().map(Long::valueOf).toList();
        String status = (String) body.get("status");
        adminService.batchUpdatePostStatus(ids, status);
        return Result.success("批量操作成功", null);
    }

    // ==================== 评论管理 ====================

    /**
     * 管理员查询评论列表（含作者名、帖子标题、支持所有状态筛选）
     * GET /admin/comments?keyword=&status=&postId=&authorId=&page=1&size=10
     */
    @GetMapping("/comments")
    public Result<IPage<Map<String, Object>>> listComments(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long postId,
            @RequestParam(required = false) Long authorId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return Result.success(adminService.adminListComments(keyword, status, postId, authorId, page, size));
    }

    /**
     * 查看评论详情
     * GET /admin/comments/{commentId}
     */
    @GetMapping("/comments/{commentId}")
    public Result<Comment> getCommentDetail(@PathVariable Long commentId) {
        return Result.success(adminService.getCommentDetail(commentId));
    }

    /**
     * 修改评论状态（NORMAL/BLOCKED/DELETED）
     * PUT /admin/comments/{commentId}/status?status=BLOCKED
     */
    @PutMapping("/comments/{commentId}/status")
    public Result<Void> updateCommentStatus(@PathVariable Long commentId,
                                             @RequestParam String status) {
        adminService.updateCommentStatus(commentId, status);
        return Result.success("操作成功", null);
    }

    /**
     * 批量修改评论状态
     * PUT /admin/comments/batch-status
     * Body: { "ids": [1,2,3], "status": "BLOCKED" }
     */
    @PutMapping("/comments/batch-status")
    public Result<Void> batchUpdateCommentStatus(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Integer> rawIds = (List<Integer>) body.get("ids");
        List<Long> ids = rawIds.stream().map(Long::valueOf).toList();
        String status = (String) body.get("status");
        adminService.batchUpdateCommentStatus(ids, status);
        return Result.success("批量操作成功", null);
    }

    // ==================== 操作日志 ====================

    /**
     * 查询管理员操作日志
     * GET /admin/logs?page=1&size=20&adminId=
     */
    @GetMapping("/logs")
    public Result<IPage<AdminLog>> listLogs(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) Long adminId) {
        return Result.success(adminService.listLogs(page, size, adminId));
    }

    // ==================== 吧管理 ====================

    @GetMapping("/boards")
    public Result<IPage<com.hcz.nexusbackend.entity.Board>> listBoards(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return Result.success(adminService.adminListBoards(keyword, status, page, size));
    }

    @PutMapping("/boards/{boardId}/status")
    public Result<Void> updateBoardStatus(@PathVariable Long boardId,
                                           @RequestParam String status) {
        adminService.updateBoardStatus(boardId, status);
        return Result.success("操作成功", null);
    }

    // ==================== 通知管理员 ====================

    @GetMapping("/notify-admin")
    public Result<Map<String, Object>> getNotifyAdmin() {
        return Result.success(adminService.getNotifyCredentials());
    }
}
