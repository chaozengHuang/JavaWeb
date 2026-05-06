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
    public Post create(@RequestParam Integer userId,
                       @RequestParam String title,
                       @RequestParam String content) {
        return postService.create(userId, title, content);
    }

    @GetMapping("/list")
    public List<Post> list() {
        return postService.list();
    }

    @GetMapping("/detail")
    public Post detail(@RequestParam Integer postId) {
        return postService.detail(postId);
    }
}
