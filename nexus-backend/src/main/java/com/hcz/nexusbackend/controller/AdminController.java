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
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Long boardId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        IPage<Post> result = postService.adminList(keyword, status, type, boardId, page, size);
        return Result.success(result);
    }

    @PutMapping("/post/{id}/status")
    public Result<Void> updatePostStatus(@PathVariable Long id, @RequestParam String status) {
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
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long postId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        IPage<Comment> result = commentService.adminList(keyword, status, postId, page, size);
        return Result.success(result);
    }

    @DeleteMapping("/comment/{id}")
    public Result<Void> deleteComment(@PathVariable Long id) {
        commentService.adminDelete(id);
        return Result.success("删除成功", null);
    }
}
