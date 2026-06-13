package com.hcz.nexusbackend.controller;

import com.hcz.nexusbackend.common.Result;
import com.hcz.nexusbackend.dto.CommentCreateDTO;
import com.hcz.nexusbackend.entity.Comment;
import com.hcz.nexusbackend.service.CommentService;
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

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/comment")
public class CommentController {

    @Autowired
    private CommentService commentService;

    @PostMapping
    public Result<Comment> create(@RequestBody @Valid CommentCreateDTO dto) {
        Comment comment = commentService.create(dto);
        return Result.success("评论成功", comment);
    }

    @GetMapping
    public Result<List<Comment>> list(@RequestParam("postId") Long postId) {
        List<Comment> comments = commentService.list(postId);
        return Result.success(comments);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        commentService.delete(id);
        return Result.success("删除成功", null);
    }

    @PutMapping("/{id}/accept")
    public Result<Map<String, Object>> accept(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Integer points = body.get("points") != null ? Integer.valueOf(body.get("points").toString()) : null;
        int awarded = commentService.accept(id, points);
        return Result.success("采纳成功，已转移" + awarded + "积分", Map.of("awarded", awarded));
    }
}
