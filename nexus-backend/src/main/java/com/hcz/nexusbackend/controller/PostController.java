package com.hcz.nexusbackend.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.hcz.nexusbackend.common.Result;
import com.hcz.nexusbackend.dto.PostCreateDTO;
import com.hcz.nexusbackend.dto.PostUpdateDTO;
import com.hcz.nexusbackend.entity.Post;
import com.hcz.nexusbackend.service.PostService;
import jakarta.validation.Valid;
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

@RestController
@RequestMapping("/post")
public class PostController {

    @Autowired
    private PostService postService;

    @PostMapping
    public Result<Post> create(@RequestBody @Valid PostCreateDTO dto) {
        Post post = postService.create(dto);
        return Result.success("发帖成功", post);
    }

    @GetMapping
    public Result<IPage<Post>> list(
            @RequestParam(required = false) Long boardId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        IPage<Post> result = postService.list(boardId, keyword, status, type, page, size);
        return Result.success(result);
    }

    @GetMapping("/{id}")
    public Result<Post> detail(@PathVariable Long id) {
        Post post = postService.detail(id);
        return Result.success(post);
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody @Valid PostUpdateDTO dto) {
        postService.update(id, dto);
        return Result.success("修改成功", null);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        postService.delete(id);
        return Result.success("删除成功", null);
    }

    @PutMapping("/{id}/pin")
    public Result<Void> setPin(@PathVariable Long id, @RequestParam Integer status) {
        postService.setPin(id, status);
        return Result.success("操作成功", null);
    }

    @PutMapping("/{id}/global-pin")
    public Result<Void> setGlobalPin(@PathVariable Long id, @RequestParam Integer status) {
        postService.setGlobalPin(id, status);
        return Result.success("操作成功", null);
    }

    @PutMapping("/{id}/feature")
    public Result<Void> setFeature(@PathVariable Long id, @RequestParam Integer status) {
        postService.setFeature(id, status);
        return Result.success("操作成功", null);
    }
}
