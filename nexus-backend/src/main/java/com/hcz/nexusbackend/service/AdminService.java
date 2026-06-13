package com.hcz.nexusbackend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hcz.nexusbackend.dto.ResetPasswordDTO;
import com.hcz.nexusbackend.dto.UserStatusDTO;
import com.hcz.nexusbackend.entity.AdminLog;
import com.hcz.nexusbackend.entity.Comment;
import com.hcz.nexusbackend.entity.Post;
import com.hcz.nexusbackend.entity.User;
import com.hcz.nexusbackend.exception.BusinessException;
import com.hcz.nexusbackend.mapper.AdminLogMapper;
import com.hcz.nexusbackend.entity.Board;
import com.hcz.nexusbackend.entity.UserBoardRelation;
import com.hcz.nexusbackend.mapper.BoardMapper;
import com.hcz.nexusbackend.mapper.CommentMapper;
import com.hcz.nexusbackend.mapper.UserBoardRelationMapper;
import com.hcz.nexusbackend.mapper.PostMapper;
import com.hcz.nexusbackend.mapper.UserMapper;
import com.hcz.nexusbackend.util.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
public class AdminService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PostMapper postMapper;

    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private AdminLogMapper adminLogMapper;

    @Autowired
    private BoardMapper boardMapper;

    @Autowired
    private UserBoardRelationMapper boardRelationMapper;

    @Autowired
    private MessageService messageService;

    @Autowired
    private NotificationService notificationService;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private void sendSystemNotification(Long targetUserId, String content) {
        try {
            Long notifierId = notificationService.getNotifyUserId();
            if (notifierId != null) {
                messageService.send(notifierId, targetUserId, content);
            }
        } catch (Exception ignored) {}
    }

    public Map<String, Object> getNotifyCredentials() {
        return notificationService.getNotifyCredentials();
    }

    // ==================== 吧管理 ====================

    public IPage<Board> adminListBoards(String keyword, String status, Integer page, Integer size) {
        getCurrentAdmin();
        Page<Board> pageParam = new Page<>(page != null ? page : 1, size != null ? size : 10);
        LambdaQueryWrapper<Board> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(Board::getName, keyword);
        }
        if (status != null && !status.isEmpty()) {
            wrapper.eq(Board::getStatus, status);
        }
        wrapper.orderByDesc(Board::getId);
        return boardMapper.selectPage(pageParam, wrapper);
    }

    public void updateBoardStatus(Long boardId, String status) {
        User admin = getCurrentAdmin();
        Board board = boardMapper.selectById(boardId);
        if (board == null) throw new BusinessException("贴吧不存在");
        String action = "DELETED".equals(status) ? "DELETE_BOARD" : "RESTORE_BOARD";
        logAction(admin, action, "BOARD", boardId,
                ("DELETED".equals(status) ? "删除贴吧 " : "恢复贴吧 ") + board.getName());
        if ("DELETED".equals(status)) {
            List<UserBoardRelation> members = boardRelationMapper.selectList(
                    new LambdaQueryWrapper<UserBoardRelation>().eq(UserBoardRelation::getBoardId, boardId));
            for (UserBoardRelation m : members) {
                sendSystemNotification(m.getUserId(), "贴吧「" + board.getName() + "」已被管理员删除");
            }
        }
        board.setStatus(status);
        boardMapper.updateById(board);
    }

    public void resetNotifyPassword() {
        // Not implemented - would need to encode new random password
    }

    // ==================== 权限校验 ====================

    private User getCurrentAdmin() {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            throw new BusinessException(401, "请先登录");
        }
        User user = userMapper.selectById(userId);
        if (user == null || !"SYS_ADMIN".equals(user.getGlobalRole())) {
            throw new BusinessException(403, "权限不足：需要管理员权限");
        }
        return user;
    }

    // ==================== 操作日志 ====================

    private void logAction(User admin, String action, String targetType, Long targetId, String detail) {
        AdminLog logEntry = new AdminLog();
        logEntry.setAdminId(admin.getId());
        logEntry.setAdminUsername(admin.getUsername());
        logEntry.setAction(action);
        logEntry.setTargetType(targetType);
        logEntry.setTargetId(targetId);
        logEntry.setDetail(detail);
        adminLogMapper.insert(logEntry);
    }

    // ==================== 用户管理 ====================

    public IPage<User> listUsers(String keyword, String status, String role, Integer page, Integer size) {
        getCurrentAdmin();
        Page<User> pageParam = new Page<>(page != null ? page : 1, size != null ? size : 10);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(User::getUsername, keyword).or().like(User::getEmail, keyword));
        }
        if (status != null && !status.isEmpty()) {
            wrapper.eq(User::getStatus, status);
        }
        if (role != null && !role.isEmpty()) {
            wrapper.eq(User::getGlobalRole, role);
        }
        wrapper.orderByDesc(User::getId);
        return userMapper.selectPage(pageParam, wrapper);
    }

    public Map<String, Object> getUserDetail(Long userId) {
        getCurrentAdmin();
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        int postCount = Math.toIntExact(postMapper.selectCount(
                new LambdaQueryWrapper<Post>().eq(Post::getAuthorId, userId)));
        int commentCount = Math.toIntExact(commentMapper.selectCount(
                new LambdaQueryWrapper<Comment>().eq(Comment::getAuthorId, userId)));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("user", user);
        result.put("postCount", postCount);
        result.put("commentCount", commentCount);
        return result;
    }

    public void resetUserPassword(Long userId, ResetPasswordDTO dto) {
        User admin = getCurrentAdmin();
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        userMapper.updateById(user);
        logAction(admin, "RESET_PASSWORD", "USER", userId, "重置用户 " + user.getUsername() + " 的密码");
        log.info("管理员 {} 重置了用户 {} 的密码", admin.getUsername(), user.getUsername());
    }

    public void updateUserStatus(Long userId, UserStatusDTO dto) {
        User admin = getCurrentAdmin();
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (!"ACTIVE".equals(dto.getStatus()) && !"BANNED".equals(dto.getStatus())) {
            throw new BusinessException("状态值无效，仅支持 ACTIVE 或 BANNED");
        }
        if (user.getGlobalRole() != null && user.getGlobalRole().equals("SYS_ADMIN")) {
            throw new BusinessException("不能封禁管理员账号");
        }
        user.setStatus(dto.getStatus());
        userMapper.updateById(user);
        String action = "BANNED".equals(dto.getStatus()) ? "BAN_USER" : "UNBAN_USER";
        String detail = ("BANNED".equals(dto.getStatus()) ? "封禁用户 " : "解封用户 ") + user.getUsername();
        logAction(admin, action, "USER", userId, detail);
        // 通知用户
        sendSystemNotification(userId, "系统管理员已" + ("BANNED".equals(dto.getStatus()) ? "封禁" : "解封") + "您的账号");
        log.info("管理员 {} {} 用户 {}", admin.getUsername(),
                "BANNED".equals(dto.getStatus()) ? "封禁" : "解封", user.getUsername());
    }

    // ==================== 帖子管理 ====================

    public IPage<Map<String, Object>> adminListPosts(String keyword, String status, String type,
                                                      Long boardId, Long authorId,
                                                      Integer page, Integer size) {
        getCurrentAdmin();
        Page<Post> pageParam = new Page<>(page != null ? page : 1, size != null ? size : 10);
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(Post::getTitle, keyword).or().like(Post::getContent, keyword));
        }
        if (status != null && !status.isEmpty()) {
            wrapper.eq(Post::getStatus, status);
        }
        if (type != null && !type.isEmpty()) {
            wrapper.eq(Post::getType, type);
        }
        if (boardId != null) {
            wrapper.eq(Post::getBoardId, boardId);
        }
        if (authorId != null) {
            wrapper.eq(Post::getAuthorId, authorId);
        }
        wrapper.orderByDesc(Post::getId);

        IPage<Post> postPage = postMapper.selectPage(pageParam, wrapper);
        return postPage.convert(post -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", post.getId());
            map.put("boardId", post.getBoardId());
            map.put("authorId", post.getAuthorId());
            map.put("title", post.getTitle());
            map.put("content", post.getContent());
            map.put("type", post.getType());
            map.put("rewardPoints", post.getRewardPoints());
            map.put("isPinned", post.getIsPinned());
            map.put("isGlobalPinned", post.getIsGlobalPinned());
            map.put("isFeatured", post.getIsFeatured());
            map.put("status", post.getStatus());
            map.put("createdAt", post.getCreatedAt());
            map.put("updatedAt", post.getUpdatedAt());
            User author = userMapper.selectById(post.getAuthorId());
            map.put("authorName", author != null ? author.getUsername() : "未知");
            Board board = boardMapper.selectById(post.getBoardId());
            map.put("boardName", board != null ? board.getName() : "未知");
            return map;
        });
    }

    public Post getPostDetail(Long postId) {
        getCurrentAdmin();
        Post post = postMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException("帖子不存在");
        }
        return post;
    }

    public void updatePostStatus(Long postId, String status) {
        User admin = getCurrentAdmin();
        if (!"ACTIVE".equals(status) && !"BLOCKED".equals(status) && !"DELETED".equals(status)) {
            throw new BusinessException("状态值无效，仅支持 ACTIVE、BLOCKED 或 DELETED");
        }
        Post post = postMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException("帖子不存在");
        }
        post.setStatus(status);
        postMapper.updateById(post);
        String detail = "将帖子 " + post.getTitle() + " 状态修改为 " + status;
        logAction(admin, "UPDATE_POST_STATUS", "POST", postId, detail);
        if ("DELETED".equals(status) || "BLOCKED".equals(status)) {
            sendSystemNotification(post.getAuthorId(), "系统管理员已将您的帖子「" + post.getTitle() + "」" + ("DELETED".equals(status) ? "删除" : "屏蔽"));
        }
        log.info("管理员 {} {}", admin.getUsername(), detail);
    }

    public void batchUpdatePostStatus(java.util.List<Long> ids, String status) {
        User admin = getCurrentAdmin();
        if (!"NORMAL".equals(status) && !"BLOCKED".equals(status) && !"DELETED".equals(status)) {
            throw new BusinessException("状态值无效");
        }
        for (Long id : ids) {
            Post post = postMapper.selectById(id);
            if (post != null) {
                post.setStatus(status);
                postMapper.updateById(post);
            }
        }
        logAction(admin, "BATCH_UPDATE_POST_STATUS", "POST", null,
                "批量将 " + ids.size() + " 个帖子状态修改为 " + status);
        log.info("管理员 {} 批量修改 {} 个帖子状态为 {}", admin.getUsername(), ids.size(), status);
    }

    // ==================== 评论管理 ====================

    public IPage<Map<String, Object>> adminListComments(String keyword, String status, Long postId,
                                                         Long authorId, Integer page, Integer size) {
        getCurrentAdmin();
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
        if (authorId != null) {
            wrapper.eq(Comment::getAuthorId, authorId);
        }
        wrapper.orderByDesc(Comment::getId);

        IPage<Comment> commentPage = commentMapper.selectPage(pageParam, wrapper);
        return commentPage.convert(comment -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", comment.getId());
            map.put("postId", comment.getPostId());
            map.put("authorId", comment.getAuthorId());
            map.put("content", comment.getContent());
            map.put("parentCommentId", comment.getParentCommentId());
            map.put("isAccepted", comment.getIsAccepted());
            map.put("status", comment.getStatus());
            map.put("createdAt", comment.getCreatedAt());
            map.put("updatedAt", comment.getUpdatedAt());
            User author = userMapper.selectById(comment.getAuthorId());
            map.put("authorName", author != null ? author.getUsername() : "未知");
            Post post = postMapper.selectById(comment.getPostId());
            map.put("postTitle", post != null ? post.getTitle() : "未知");
            if (post != null) {
                Board board = boardMapper.selectById(post.getBoardId());
                map.put("boardId", post.getBoardId());
                map.put("boardName", board != null ? board.getName() : "未知");
            }
            return map;
        });
    }

    public Comment getCommentDetail(Long commentId) {
        getCurrentAdmin();
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new BusinessException("评论不存在");
        }
        return comment;
    }

    public void updateCommentStatus(Long commentId, String status) {
        User admin = getCurrentAdmin();
        if (!"ACTIVE".equals(status) && !"BLOCKED".equals(status) && !"DELETED".equals(status)) {
            throw new BusinessException("状态值无效，仅支持 ACTIVE、BLOCKED 或 DELETED");
        }
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new BusinessException("评论不存在");
        }
        comment.setStatus(status);
        commentMapper.updateById(comment);
        String detail = "将评论#" + commentId + " 状态修改为 " + status;
        logAction(admin, "UPDATE_COMMENT_STATUS", "COMMENT", commentId, detail);
        if ("DELETED".equals(status) || "BLOCKED".equals(status)) {
            sendSystemNotification(comment.getAuthorId(), "系统管理员已将您的评论" + ("DELETED".equals(status) ? "删除" : "屏蔽"));
        }
        log.info("管理员 {} {}", admin.getUsername(), detail);
    }

    public void batchUpdateCommentStatus(java.util.List<Long> ids, String status) {
        User admin = getCurrentAdmin();
        if (!"NORMAL".equals(status) && !"BLOCKED".equals(status) && !"DELETED".equals(status)) {
            throw new BusinessException("状态值无效");
        }
        for (Long id : ids) {
            Comment comment = commentMapper.selectById(id);
            if (comment != null) {
                comment.setStatus(status);
                commentMapper.updateById(comment);
            }
        }
        logAction(admin, "BATCH_UPDATE_COMMENT_STATUS", "COMMENT", null,
                "批量将 " + ids.size() + " 条评论状态修改为 " + status);
        log.info("管理员 {} 批量修改 {} 条评论状态为 {}", admin.getUsername(), ids.size(), status);
    }

    // ==================== 操作日志查询 ====================

    public IPage<AdminLog> listLogs(Integer page, Integer size, Long adminId) {
        getCurrentAdmin();
        Page<AdminLog> pageParam = new Page<>(page != null ? page : 1, size != null ? size : 20);
        LambdaQueryWrapper<AdminLog> wrapper = new LambdaQueryWrapper<>();
        if (adminId != null) {
            wrapper.eq(AdminLog::getAdminId, adminId);
        }
        wrapper.orderByDesc(AdminLog::getId);
        return adminLogMapper.selectPage(pageParam, wrapper);
    }

    // ==================== 硬删除（物理清除） ====================

    public void hardDeleteUser(Long userId) {
        User admin = getCurrentAdmin();
        User user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException("用户不存在");
        userMapper.deleteById(userId);
        logAction(admin, "HARD_DELETE_USER", "USER", userId, "物理删除用户 " + user.getUsername());
    }

    public void hardDeletePost(Long postId) {
        User admin = getCurrentAdmin();
        Post post = postMapper.selectById(postId);
        if (post == null) throw new BusinessException("帖子不存在");
        postMapper.deleteById(postId);
        logAction(admin, "HARD_DELETE_POST", "POST", postId, "物理删除帖子 " + post.getTitle());
    }

    public void hardDeleteComment(Long commentId) {
        User admin = getCurrentAdmin();
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null) throw new BusinessException("评论不存在");
        commentMapper.deleteById(commentId);
        logAction(admin, "HARD_DELETE_COMMENT", "COMMENT", commentId, "物理删除评论#" + commentId);
    }

    public void hardDeleteBoard(Long boardId) {
        User admin = getCurrentAdmin();
        Board board = boardMapper.selectById(boardId);
        if (board == null) throw new BusinessException("贴吧不存在");
        boardMapper.deleteById(boardId);
        logAction(admin, "HARD_DELETE_BOARD", "BOARD", boardId, "物理删除贴吧 " + board.getName());
    }

    public void hardDeleteLog(Long logId) {
        getCurrentAdmin();
        adminLogMapper.deleteById(logId);
    }
}
