package com.hcz.nexusbackend.controller;

import com.hcz.nexusbackend.common.Result;
import com.hcz.nexusbackend.entity.Board;
import com.hcz.nexusbackend.service.BoardService;
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

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/board")
public class BoardController {

    @Autowired
    private BoardService boardService;

    // ==================== 基础 ====================

    @GetMapping("/list")
    public Result<List<Board>> list() {
        return Result.success(boardService.list());
    }

    @PostMapping("/create")
    public Result<Board> create(@RequestParam("name") String name,
                                @RequestParam(value = "description", required = false) String description,
                                @RequestParam("creatorId") Long creatorId) {
        return Result.success("创建成功", boardService.create(name, description, creatorId));
    }

    // ==================== 吧详情 ====================

    @GetMapping("/{boardId}")
    public Result<Map<String, Object>> getBoardDetail(@PathVariable Long boardId) {
        return Result.success(boardService.getBoardDetail(boardId));
    }

    @PutMapping("/{boardId}")
    public Result<Void> updateBoard(@PathVariable Long boardId,
                                     @RequestBody Map<String, String> body) {
        boardService.updateBoard(boardId, body.getOrDefault("description", ""));
        return Result.success();
    }

    @DeleteMapping("/{boardId}")
    public Result<Void> deleteBoard(@PathVariable Long boardId) {
        boardService.deleteBoard(boardId);
        return Result.success();
    }

    @PostMapping("/{boardId}/avatar")
    public Result<String> uploadBoardAvatar(@PathVariable Long boardId,
                                             @RequestParam("file") MultipartFile file) {
        String url = boardService.uploadBoardAvatar(boardId, file);
        return Result.success("上传成功", url);
    }

    // ==================== 帖子管理 ====================

    @PutMapping("/{boardId}/posts/{postId}/hide")
    public Result<Void> hidePost(@PathVariable Long boardId, @PathVariable Long postId) {
        boardService.hidePost(boardId, postId);
        return Result.success();
    }

    @PutMapping("/{boardId}/posts/{postId}/show")
    public Result<Void> showPost(@PathVariable Long boardId, @PathVariable Long postId) {
        boardService.showPost(boardId, postId);
        return Result.success();
    }

    @DeleteMapping("/{boardId}/posts/{postId}")
    public Result<Void> deleteBoardPost(@PathVariable Long boardId, @PathVariable Long postId) {
        boardService.deleteBoardPost(boardId, postId);
        return Result.success();
    }

    // ==================== 用户管理 ====================

    @PutMapping("/{boardId}/mute/{userId}")
    public Result<Void> muteUser(@PathVariable Long boardId, @PathVariable Long userId) {
        boardService.muteUser(boardId, userId);
        return Result.success();
    }

    @DeleteMapping("/{boardId}/mute/{userId}")
    public Result<Void> unmuteUser(@PathVariable Long boardId, @PathVariable Long userId) {
        boardService.unmuteUser(boardId, userId);
        return Result.success();
    }

    @GetMapping("/{boardId}/admins")
    public Result<List<Map<String, Object>>> getAdmins(@PathVariable Long boardId) {
        return Result.success(boardService.getAdmins(boardId));
    }

    @PutMapping("/{boardId}/admins/{userId}")
    public Result<Void> addAdmin(@PathVariable Long boardId, @PathVariable Long userId) {
        boardService.addAdmin(boardId, userId);
        return Result.success();
    }

    @DeleteMapping("/{boardId}/admins/{userId}")
    public Result<Void> removeAdmin(@PathVariable Long boardId, @PathVariable Long userId) {
        boardService.removeAdmin(boardId, userId);
        return Result.success();
    }

    @GetMapping("/{boardId}/members")
    public Result<Map<String, Object>> getMembers(
            @PathVariable Long boardId,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size) {
        return Result.success(boardService.getMembers(boardId, keyword, page, size));
    }

    @GetMapping("/{boardId}/my-role")
    public Result<Map<String, Object>> getMyRole(@PathVariable Long boardId) {
        return Result.success(boardService.getUserBoardRole(boardId));
    }

    // ==================== 加入/离开 ====================

    @PostMapping("/{boardId}/join")
    public Result<Void> joinBoard(@PathVariable Long boardId) {
        boardService.joinBoard(boardId);
        return Result.success();
    }

    @DeleteMapping("/{boardId}/leave")
    public Result<Void> leaveBoard(@PathVariable Long boardId) {
        boardService.leaveBoard(boardId);
        return Result.success();
    }

    // ==================== 排行榜 ====================

    @GetMapping("/{boardId}/leaderboard")
    public Result<List<Map<String, Object>>> getLeaderboard(@PathVariable Long boardId) {
        return Result.success(boardService.getLeaderboard(boardId));
    }

    // ==================== 用户相关 ====================

    @GetMapping("/my-boards")
    public Result<List<Map<String, Object>>> getMyBoards() {
        return Result.success(boardService.getUserBoards(boardService.getCurrentUserId()));
    }

    // ==================== 帖子/评论回收站 ====================

    @GetMapping("/{boardId}/trash")
    public Result<Map<String, Object>> getTrashPosts(@PathVariable Long boardId) {
        return Result.success(boardService.getHiddenDeletedPosts(boardId));
    }

    @PutMapping("/{boardId}/posts/{postId}/recover")
    public Result<Void> recoverPost(@PathVariable Long boardId, @PathVariable Long postId) {
        boardService.recoverPost(boardId, postId);
        return Result.success();
    }

    @GetMapping("/{boardId}/posts/{postId}/comments-manage")
    public Result<List<com.hcz.nexusbackend.entity.Comment>> getPostCommentsManage(
            @PathVariable Long boardId, @PathVariable Long postId) {
        return Result.success(boardService.getPostComments(boardId, postId));
    }

    @DeleteMapping("/{boardId}/comments/{commentId}")
    public Result<Void> deleteCommentOnPost(@PathVariable Long boardId, @PathVariable Long commentId) {
        boardService.deleteCommentOnPost(boardId, commentId);
        return Result.success();
    }

    @PutMapping("/{boardId}/comments/{commentId}/recover")
    public Result<Void> recoverCommentOnPost(@PathVariable Long boardId, @PathVariable Long commentId) {
        boardService.recoverCommentOnPost(boardId, commentId);
        return Result.success();
    }

    @GetMapping("/{boardId}/trash-comments")
    public Result<List<Map<String, Object>>> getTrashComments(@PathVariable Long boardId) {
        return Result.success(boardService.getTrashComments(boardId));
    }
}
