package com.hcz.nexusbackend.controller;

import com.hcz.nexusbackend.entity.Post;
import com.hcz.nexusbackend.service.PostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/post")
public class PostController {

    @Autowired
    private PostService postService;

    @PostMapping("/create")
    public String create(@RequestParam Long userId,
                         @RequestParam String title,
                         @RequestParam String content,
                         @RequestParam(required = false) Long boardId,
                         @RequestParam(required = false) Integer rewardPoints) {
        return postService.create(userId, title, content, boardId, rewardPoints);
    }

    @GetMapping("/list")
    public List<Post> list(@RequestParam(required = false) Long boardId) {
        return postService.list(boardId);
    }

    @GetMapping("/detail")
    public Post detail(@RequestParam Long postId) {
        return postService.detail(postId);
    }

    @PostMapping("/update")
    public String update(@RequestParam Long postId,
                         @RequestParam Long userId,
                         @RequestParam String title,
                         @RequestParam String content) {
        return postService.update(postId, userId, title, content);
    }

    @PostMapping("/setTop")
    public String setTop(@RequestParam Long postId,
                         @RequestParam Long userId,
                         @RequestParam Integer status) {
        return postService.setTop(postId, userId, status);
    }

    @PostMapping("/setElite")
    public String setElite(@RequestParam Long postId,
                           @RequestParam Long userId,
                           @RequestParam Integer status) {
        return postService.setElite(postId, userId, status);
    }
}
