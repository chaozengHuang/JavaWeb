package com.hcz.nexusbackend.controller;

import com.hcz.nexusbackend.entity.Comment;
import com.hcz.nexusbackend.service.CommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/comment")
public class CommentController {

    @Autowired
    private CommentService commentService;

    @PostMapping("/create")
    public Comment create(@RequestParam Integer postId,
                          @RequestParam Integer userId,
                          @RequestParam String content) {
        return commentService.create(postId, userId, content);
    }

    @GetMapping("/list")
    public List<Comment> list(@RequestParam Integer postId) {
        return commentService.list(postId);
    }

    @PostMapping("/accept")
    public String accept(@RequestParam Integer commentId) {
        return commentService.accept(commentId);
    }
}
