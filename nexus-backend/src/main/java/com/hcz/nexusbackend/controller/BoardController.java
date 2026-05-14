package com.hcz.nexusbackend.controller;

import com.hcz.nexusbackend.entity.Board;
import com.hcz.nexusbackend.service.BoardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/board")
public class BoardController {

    @Autowired
    private BoardService boardService;

    @GetMapping("/list")
    public List<Board> list() {
        return boardService.list();
    }

    @PostMapping("/create")
    public Board create(@RequestParam("name") String name,
                        @RequestParam(value = "description", required = false) String description,
                        @RequestParam("creatorId") Long creatorId) {
        return boardService.create(name, description, creatorId);
    }
}
