package com.hcz.nexusbackend.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.hcz.nexusbackend.common.Result;
import com.hcz.nexusbackend.entity.Comment;
import com.hcz.nexusbackend.entity.Post;
import com.hcz.nexusbackend.service.CommentService;
import com.hcz.nexusbackend.service.PostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private PostService postService;

    @Autowired
    private CommentService commentService;

    @GetMapping("/post")
    public Result<IPage<Post>> listPosts(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "boardId", required = false) Long boardId,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size) {
        IPage<Post> result = postService.adminList(keyword, status, type, boardId, page, size);
        return Result.success(result);
    }

    @PutMapping("/post/{id}/status")
    public Result<Void> updatePostStatus(@PathVariable Long id, @RequestParam("status") String status) {
        postService.adminUpdateStatus(id, status);
        return Result.success("操作成功", null);
    }

    @DeleteMapping("/post/{id}")
    public Result<Void> deletePost(@PathVariable Long id) {
        postService.adminDelete(id);
        return Result.success("删除成功", null);
    }

    @GetMapping("/comment")
    public Result<IPage<Comment>> listComments(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "postId", required = false) Long postId,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size) {
        IPage<Comment> result = commentService.adminList(keyword, status, postId, page, size);
        return Result.success(result);
    }

    @DeleteMapping("/comment/{id}")
    public Result<Void> deleteComment(@PathVariable Long id) {
        commentService.adminDelete(id);
        return Result.success("删除成功", null);
    }
}
