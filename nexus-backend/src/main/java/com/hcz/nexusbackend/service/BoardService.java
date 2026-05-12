package com.hcz.nexusbackend.service;

import com.hcz.nexusbackend.entity.Board;
import com.hcz.nexusbackend.mapper.BoardMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BoardService {

    @Autowired
    private BoardMapper boardMapper;

    public List<Board> list() {
        return boardMapper.selectList(null);
    }

    public Board create(String name, String description, Long creatorId) {
        Board board = new Board();
        board.setName(name);
        board.setDescription(description);
        board.setCreatorId(creatorId);
        boardMapper.insert(board);
        return board;
    }
}
