package com.hcz.nexusbackend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hcz.nexusbackend.dto.CommentCreateDTO;
import com.hcz.nexusbackend.entity.Comment;
import com.hcz.nexusbackend.entity.Post;
import com.hcz.nexusbackend.entity.User;
import com.hcz.nexusbackend.entity.UserBoardRelation;
import com.hcz.nexusbackend.exception.BusinessException;
import com.hcz.nexusbackend.mapper.CommentMapper;
import com.hcz.nexusbackend.mapper.PostMapper;
import com.hcz.nexusbackend.mapper.UserBoardRelationMapper;
import com.hcz.nexusbackend.mapper.UserMapper;
import com.hcz.nexusbackend.util.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CommentService {

    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private PostMapper postMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserBoardRelationMapper relationMapper;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private MessageService messageService;

    private void sendReplyNotify(Long senderId, Long receiverId, Long postId, Long parentCommentId) {
        try {
            Long notifierId = notificationService.getNotifyUserId();
            if (notifierId == null || senderId.equals(receiverId)) return;
            User sender = userMapper.selectById(senderId);
            String senderName = sender != null ? sender.getUsername() : "有人";
            String link = "/post/" + postId;
            if (parentCommentId != null) link += "?comment=" + parentCommentId;
            messageService.send(notifierId, receiverId, senderName + " 回复了您的评论，点击查看：" + link);
        } catch (Exception ignored) {}
    }

    public Comment create(CommentCreateDTO dto) {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            throw new BusinessException(401, "请先登录");
        }

        Post post = postMapper.selectById(dto.getPostId());
        if (post == null || "DELETED".equals(post.getStatus())) {
            throw new BusinessException("帖子不存在");
        }

        if (dto.getParentCommentId() != null) {
            Comment parent = commentMapper.selectById(dto.getParentCommentId());
            if (parent == null) {
                throw new BusinessException("回复的评论不存在");
            }
        }

        // 检查在该吧是否被禁言或未加入
        if (post.getBoardId() != null) {
            UserBoardRelation rel = relationMapper.selectOne(
                    new LambdaQueryWrapper<UserBoardRelation>()
                            .eq(UserBoardRelation::getUserId, userId)
                            .eq(UserBoardRelation::getBoardId, post.getBoardId()));
            if (rel == null) {
                throw new BusinessException(403, "REQUIRE_JOIN:请先加入该吧后再评论");
            }
            if ("MUTED".equals(rel.getBoardRole())) {
                throw new BusinessException("您在该吧已被禁言，无法评论");
            }
        }

        Comment comment = new Comment();
        comment.setPostId(dto.getPostId());
        comment.setAuthorId(userId);
        comment.setContent(dto.getContent());
        comment.setParentCommentId(dto.getParentCommentId());
        comment.setIsAccepted(0);
        comment.setStatus("ACTIVE");
        commentMapper.insert(comment);
        if (dto.getParentCommentId() != null) {
            Comment parent = commentMapper.selectById(dto.getParentCommentId());
            if (parent != null) {
                sendReplyNotify(userId, parent.getAuthorId(), post.getId(), dto.getParentCommentId());
            }
        } else {
            sendReplyNotify(userId, post.getAuthorId(), post.getId(), null);
        }
        // 评论+2活跃度
        Long boardId = post.getBoardId();
        if (boardId != null) {
            UserBoardRelation rel = relationMapper.selectOne(
                    new LambdaQueryWrapper<UserBoardRelation>()
                            .eq(UserBoardRelation::getUserId, userId)
                            .eq(UserBoardRelation::getBoardId, boardId));
            if (rel != null) {
                UserBoardRelation update = new UserBoardRelation();
                update.setId(rel.getId());
                update.setActivityPoints((rel.getActivityPoints() != null ? rel.getActivityPoints() : 0) + 2);
                relationMapper.updateById(update);
            }
        }
        return comment;
    }

    public List<Comment> list(Long postId) {
        List<Comment> all = commentMapper.selectList(
                new LambdaQueryWrapper<Comment>()
                        .eq(Comment::getPostId, postId)
                        .ne(Comment::getStatus, "DELETED")
                        .ne(Comment::getStatus, "BLOCKED")
                        .orderByAsc(Comment::getId));
        // 填充作者名和父评论作者名
        for (Comment c : all) {
            User author = userMapper.selectById(c.getAuthorId());
            if (author != null) c.setAuthorUsername(author.getUsername());
            if (c.getParentCommentId() != null) {
                Comment parent = all.stream().filter(p -> p.getId().equals(c.getParentCommentId())).findFirst().orElse(null);
                if (parent != null && parent.getAuthorUsername() != null) {
                    c.setParentAuthorUsername(parent.getAuthorUsername());
                }
            }
        }
        // 构建树：父评论 + children
        Map<Long, Comment> map = new java.util.LinkedHashMap<>();
        for (Comment c : all) map.put(c.getId(), c);
        for (Comment c : all) {
            if (c.getParentCommentId() != null) {
                Comment parent = map.get(c.getParentCommentId());
                if (parent != null) {
                    if (parent.getChildren() == null) parent.setChildren(new ArrayList<>());
                    parent.getChildren().add(c);
                }
            }
        }
        // 只返回顶层评论
        return all.stream().filter(c -> c.getParentCommentId() == null).collect(Collectors.toList());
    }

    public void delete(Long id) {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            throw new BusinessException(401, "请先登录");
        }

        Comment comment = commentMapper.selectById(id);
        if (comment == null) {
            throw new BusinessException("评论不存在");
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        if (!comment.getAuthorId().equals(userId) && !"SYS_ADMIN".equals(user.getGlobalRole())) {
            throw new BusinessException(403, "无权删除");
        }

        Comment update = new Comment();
        update.setId(id);
        update.setStatus("DELETED");
        commentMapper.updateById(update);
    }

    @Transactional
    public int accept(Long id, Integer points) {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            throw new BusinessException(401, "请先登录");
        }

        Comment comment = commentMapper.selectById(id);
        if (comment == null) {
            throw new BusinessException("评论不存在");
        }
        if (comment.getIsAccepted() == 1) {
            throw new BusinessException("该评论已被采纳");
        }

        Post post = postMapper.selectById(comment.getPostId());
        if (post == null) {
            throw new BusinessException("帖子不存在");
        }
        if (!"REWARD".equals(post.getType())) {
            throw new BusinessException("只有悬赏帖才可采纳");
        }
        if (post.getRewardPoints() == null || post.getRewardPoints() <= 0) {
            throw new BusinessException("该悬赏帖积分已被领取");
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        if (!post.getAuthorId().equals(userId) && !"SYS_ADMIN".equals(user.getGlobalRole())) {
            throw new BusinessException(403, "只有帖子作者或管理员可以采纳评论");
        }
        if (comment.getAuthorId().equals(userId)) {
            throw new BusinessException("不能采纳自己的评论");
        }

        if (points == null || points <= 0 || points > post.getRewardPoints()) {
            throw new BusinessException("悬赏积分无效");
        }

        Comment update = new Comment();
        update.setId(id);
        update.setIsAccepted(1);
        commentMapper.updateById(update);

        // 转移指定金额积分
        User commentAuthor = userMapper.selectById(comment.getAuthorId());
        if (commentAuthor != null) {
            commentAuthor.setPoints((commentAuthor.getPoints() != null ? commentAuthor.getPoints() : 0) + points);
            userMapper.updateById(commentAuthor);
        }
        Post reset = new Post();
        reset.setId(post.getId());
        reset.setRewardPoints(post.getRewardPoints() - points);
        postMapper.updateById(reset);
        return points;
    }

    public IPage<Comment> adminList(String keyword, String status, Long postId, Integer page, Integer size) {
        checkAdminPermission();
        Page<Comment> pageParam = new Page<>(page != null ? page : 1, size != null ? size : 10);
        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<>();

        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(Comment::getContent, keyword);
        }
        if (status != null && !status.isEmpty()) {
            wrapper.eq(Comment::getStatus, status);
        }
        if (postId != null) {
            wrapper.eq(Comment::getPostId, postId);
        }

        wrapper.orderByDesc(Comment::getId);
        return commentMapper.selectPage(pageParam, wrapper);
    }

    public void adminDelete(Long id) {
        checkAdminPermission();
        Comment comment = commentMapper.selectById(id);
        if (comment == null) {
            throw new BusinessException("评论不存在");
        }
        commentMapper.deleteById(id);
    }

    private void checkAdminPermission() {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            throw new BusinessException(401, "请先登录");
        }
        User user = userMapper.selectById(userId);
        if (user == null || !"SYS_ADMIN".equals(user.getGlobalRole())) {
            throw new BusinessException(403, "无管理员权限");
        }
    }
}
