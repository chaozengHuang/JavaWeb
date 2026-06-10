package com.hcz.nexusbackend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hcz.nexusbackend.entity.Comment;
import com.hcz.nexusbackend.entity.Board;
import com.hcz.nexusbackend.entity.Post;
import com.hcz.nexusbackend.entity.User;
import com.hcz.nexusbackend.entity.UserBoardRelation;
import com.hcz.nexusbackend.exception.BusinessException;
import com.hcz.nexusbackend.mapper.BoardMapper;
import com.hcz.nexusbackend.mapper.CommentMapper;
import com.hcz.nexusbackend.mapper.PostMapper;
import com.hcz.nexusbackend.mapper.UserBoardRelationMapper;
import com.hcz.nexusbackend.mapper.UserMapper;
import com.hcz.nexusbackend.util.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class BoardService {

    @Autowired
    private BoardMapper boardMapper;

    @Autowired
    private UserBoardRelationMapper relationMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PostMapper postMapper;

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private MessageService messageService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private CommentMapper commentMapper;

    public Long getCurrentUserId() {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            throw new BusinessException(401, "请先登录");
        }
        return userId;
    }

    private void sendSystemNotification(Long targetUserId, String content) {
        try {
            Long notifierId = notificationService.getNotifyUserId();
            if (notifierId != null) {
                messageService.send(notifierId, targetUserId, content);
            }
        } catch (Exception ignored) {}
    }

    // ==================== 基础 ====================

    public List<Board> list() {
        return boardMapper.selectList(
                new LambdaQueryWrapper<Board>().ne(Board::getStatus, "DELETED"));
    }

    @Transactional
    public Board create(String name, String description, Long creatorId) {
        Board board = new Board();
        board.setName(name);
        board.setDescription(description);
        board.setCreatorId(creatorId);
        boardMapper.insert(board);

        // 自动设置创建者为 OWNER
        UserBoardRelation relation = new UserBoardRelation();
        relation.setUserId(creatorId);
        relation.setBoardId(board.getId());
        relation.setBoardRole("OWNER");
        relationMapper.insert(relation);

        log.info("贴吧创建: id={}, name={}, creatorId={}", board.getId(), name, creatorId);
        return board;
    }

    // ==================== 吧详情 ====================

    public Map<String, Object> getBoardDetail(Long boardId) {
        Board board = boardMapper.selectById(boardId);
        if (board == null) {
            throw new BusinessException("贴吧不存在");
        }

        // 吧主信息
        User owner = userMapper.selectById(board.getCreatorId());

        // 管理员列表
        List<UserBoardRelation> admins = relationMapper.selectList(
                new LambdaQueryWrapper<UserBoardRelation>()
                        .eq(UserBoardRelation::getBoardId, boardId)
                        .eq(UserBoardRelation::getBoardRole, "ADMIN"));
        List<Map<String, Object>> adminList = new ArrayList<>();
        for (UserBoardRelation admin : admins) {
            User u = userMapper.selectById(admin.getUserId());
            if (u != null) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("userId", u.getId());
                item.put("username", u.getUsername());
                item.put("avatar", u.getAvatar());
                adminList.add(item);
            }
        }

        // 成员数
        Long memberCount = relationMapper.selectCount(
                new LambdaQueryWrapper<UserBoardRelation>()
                        .eq(UserBoardRelation::getBoardId, boardId));

        // 帖子数
        Long postCount = postMapper.selectCount(
                new LambdaQueryWrapper<Post>()
                        .eq(Post::getBoardId, boardId)
                        .ne(Post::getStatus, "DELETED")
                        .ne(Post::getStatus, "HIDDEN"));

        // 当前用户角色
        String currentUserRole = null;
        String currentUserGlobalRole = null;
        Long userId = SecurityUtils.getUserId();
        if (userId != null) {
            UserBoardRelation self = relationMapper.selectOne(
                    new LambdaQueryWrapper<UserBoardRelation>()
                            .eq(UserBoardRelation::getUserId, userId)
                            .eq(UserBoardRelation::getBoardId, boardId));
            if (self != null) {
                currentUserRole = self.getBoardRole();
            }
            User cu = userMapper.selectById(userId);
            if (cu != null) {
                currentUserGlobalRole = cu.getGlobalRole();
                // 系统管理员不冒充吧主，前端单独处理
            }
            // 如果用户是该吧的创建者但不存在关系记录，视为 OWNER
            if (userId.equals(board.getCreatorId()) && currentUserRole == null && !"SYS_ADMIN".equals(currentUserGlobalRole)) {
                currentUserRole = "OWNER";
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("board", board);
        result.put("owner", owner != null ? Map.of("userId", owner.getId(), "username", owner.getUsername()) : null);
        result.put("admins", adminList);
        result.put("memberCount", memberCount);
        result.put("postCount", postCount);
        result.put("currentUserRole", currentUserRole);
        return result;
    }

    @Transactional
    public void updateBoard(Long boardId, String description) {
        permissionService.checkBoardPermission(boardId, List.of("OWNER"));
        Board board = new Board();
        board.setId(boardId);
        board.setDescription(description);
        boardMapper.updateById(board);
    }

    @Value("${file.upload.dir:uploads}")
    private String uploadDir;

    @Transactional
    public String uploadBoardAvatar(Long boardId, MultipartFile file) {
        permissionService.checkBoardPermission(boardId, List.of("OWNER"));
        if (file.isEmpty()) {
            throw new BusinessException("文件不能为空");
        }
        String originalName = file.getOriginalFilename();
        String ext = "";
        if (originalName != null && originalName.contains(".")) {
            ext = originalName.substring(originalName.lastIndexOf("."));
        }
        String fileName = "board_" + boardId + "_" + UUID.randomUUID().toString().substring(0, 8) + ext;

        File dir = new File(uploadDir + "/avatars");
        if (!dir.exists()) {
            dir.mkdirs();
        }

        try {
            file.transferTo(new File(dir, fileName));
        } catch (IOException e) {
            log.error("吧头像上传失败", e);
            throw new BusinessException("头像上传失败");
        }

        String avatarUrl = "/uploads/avatars/" + fileName;
        Board board = new Board();
        board.setId(boardId);
        board.setAvatar(avatarUrl);
        boardMapper.updateById(board);
        log.info("贴吧 {} 更新头像: {}", boardId, avatarUrl);
        return avatarUrl;
    }

    @Transactional
    public void deleteBoard(Long boardId) {
        // SYS_ADMIN can delete any board; OWNER can delete own board
        Long userId = getCurrentUserId();
        User currentUser = userMapper.selectById(userId);
        if (currentUser == null || !"SYS_ADMIN".equals(currentUser.getGlobalRole())) {
            permissionService.checkBoardPermission(boardId, List.of("OWNER"));
        }
        Board board = boardMapper.selectById(boardId);
        // 通知所有成员
        List<UserBoardRelation> members = relationMapper.selectList(
                new LambdaQueryWrapper<UserBoardRelation>().eq(UserBoardRelation::getBoardId, boardId));
        for (UserBoardRelation m : members) {
            sendSystemNotification(m.getUserId(), "贴吧「" + board.getName() + "」已被解散");
        }
        Board b = new Board();
        b.setId(boardId);
        b.setStatus("DELETED");
        boardMapper.updateById(b);
        log.info("贴吧删除: boardId={}", boardId);
    }

    // ==================== 帖子管理 ====================

    @Transactional
    public void hidePost(Long boardId, Long postId) {
        permissionService.checkBoardPermission(boardId, List.of("OWNER", "ADMIN"));
        Post post = postMapper.selectById(postId);
        if (post == null || !post.getBoardId().equals(boardId)) {
            throw new BusinessException("帖子不存在");
        }
        Post update = new Post();
        update.setId(postId);
        update.setStatus("HIDDEN");
        postMapper.updateById(update);
        // 通知帖子作者
        sendSystemNotification(post.getAuthorId(), "您在吧中的帖子「" + post.getTitle() + "」已被管理员隐藏");
        log.info("帖子隐藏: boardId={}, postId={}", boardId, postId);
    }

    @Transactional
    public void showPost(Long boardId, Long postId) {
        permissionService.checkBoardPermission(boardId, List.of("OWNER", "ADMIN"));
        Post post = postMapper.selectById(postId);
        if (post == null || !post.getBoardId().equals(boardId)) {
            throw new BusinessException("帖子不存在");
        }
        Post update = new Post();
        update.setId(postId);
        update.setStatus("ACTIVE");
        postMapper.updateById(update);
        log.info("帖子恢复显示: boardId={}, postId={}", boardId, postId);
    }

    @Transactional
    public void deleteBoardPost(Long boardId, Long postId) {
        permissionService.checkBoardPermission(boardId, List.of("OWNER", "ADMIN"));
        Post post = postMapper.selectById(postId);
        if (post == null || !post.getBoardId().equals(boardId)) {
            throw new BusinessException("帖子不存在");
        }
        Post update = new Post();
        update.setId(postId);
        update.setStatus("DELETED");
        postMapper.updateById(update);
        sendSystemNotification(post.getAuthorId(), "您在吧中的帖子「" + post.getTitle() + "」已被管理员删除");
        log.info("吧主删除帖子: boardId={}, postId={}", boardId, postId);
    }

    // ==================== 用户管理 ====================

    @Transactional
    public void muteUser(Long boardId, Long targetUserId) {
        permissionService.checkBoardPermission(boardId, List.of("OWNER", "ADMIN"));

        // 不能禁言 OWNER
        UserBoardRelation target = relationMapper.selectOne(
                new LambdaQueryWrapper<UserBoardRelation>()
                        .eq(UserBoardRelation::getUserId, targetUserId)
                        .eq(UserBoardRelation::getBoardId, boardId));
        if (target != null && "OWNER".equals(target.getBoardRole())) {
            throw new BusinessException("不能禁言吧主");
        }

        if (target == null) {
            // 如果用户还没加入吧，也直接创建 MUTED 记录
            UserBoardRelation relation = new UserBoardRelation();
            relation.setUserId(targetUserId);
            relation.setBoardId(boardId);
            relation.setBoardRole("MUTED");
            relationMapper.insert(relation);
        } else {
            target.setBoardRole("MUTED");
            relationMapper.updateById(target);
        }
        log.info("禁言用户: boardId={}, userId={}", boardId, targetUserId);
    }

    @Transactional
    public void unmuteUser(Long boardId, Long targetUserId) {
        permissionService.checkBoardPermission(boardId, List.of("OWNER", "ADMIN"));
        UserBoardRelation target = relationMapper.selectOne(
                new LambdaQueryWrapper<UserBoardRelation>()
                        .eq(UserBoardRelation::getUserId, targetUserId)
                        .eq(UserBoardRelation::getBoardId, boardId));
        if (target == null || !"MUTED".equals(target.getBoardRole())) {
            throw new BusinessException("该用户未被禁言");
        }
        target.setBoardRole("MEMBER");
        relationMapper.updateById(target);
        log.info("取消禁言: boardId={}, userId={}", boardId, targetUserId);
    }

    @Transactional
    public void addAdmin(Long boardId, Long targetUserId) {
        permissionService.checkBoardPermission(boardId, List.of("OWNER"));
        UserBoardRelation target = relationMapper.selectOne(
                new LambdaQueryWrapper<UserBoardRelation>()
                        .eq(UserBoardRelation::getUserId, targetUserId)
                        .eq(UserBoardRelation::getBoardId, boardId));
        if (target != null && "OWNER".equals(target.getBoardRole())) {
            throw new BusinessException("不能更改吧主角色");
        }
        if (target == null) {
            UserBoardRelation relation = new UserBoardRelation();
            relation.setUserId(targetUserId);
            relation.setBoardId(boardId);
            relation.setBoardRole("ADMIN");
            relationMapper.insert(relation);
        } else {
            target.setBoardRole("ADMIN");
            relationMapper.updateById(target);
        }
        log.info("任命管理员: boardId={}, userId={}", boardId, targetUserId);
    }

    @Transactional
    public void removeAdmin(Long boardId, Long targetUserId) {
        permissionService.checkBoardPermission(boardId, List.of("OWNER"));
        UserBoardRelation target = relationMapper.selectOne(
                new LambdaQueryWrapper<UserBoardRelation>()
                        .eq(UserBoardRelation::getUserId, targetUserId)
                        .eq(UserBoardRelation::getBoardId, boardId));
        if (target == null || !"ADMIN".equals(target.getBoardRole())) {
            throw new BusinessException("该用户不是管理员");
        }
        target.setBoardRole("MEMBER");
        relationMapper.updateById(target);
        log.info("移除管理员: boardId={}, userId={}", boardId, targetUserId);
    }

    // ==================== 加入/离开 ====================

    @Transactional
    public void joinBoard(Long boardId) {
        Long userId = getCurrentUserId();
        Board board = boardMapper.selectById(boardId);
        if (board == null) {
            throw new BusinessException("贴吧不存在");
        }
        UserBoardRelation existing = relationMapper.selectOne(
                new LambdaQueryWrapper<UserBoardRelation>()
                        .eq(UserBoardRelation::getUserId, userId)
                        .eq(UserBoardRelation::getBoardId, boardId));
        if (existing != null) {
            if ("MUTED".equals(existing.getBoardRole())) {
                throw new BusinessException("您已被禁言，无法加入");
            }
            throw new BusinessException("您已在贴吧中");
        }
        UserBoardRelation relation = new UserBoardRelation();
        relation.setUserId(userId);
        relation.setBoardId(boardId);
        relation.setBoardRole("MEMBER");
        relation.setActivityPoints(0);
        relationMapper.insert(relation);

        // 通知吧主和管理员：新成员加入
        User joiner = userMapper.selectById(userId);
        String joinerName = joiner != null ? joiner.getUsername() : "未知用户";
        List<UserBoardRelation> managers = relationMapper.selectList(
                new LambdaQueryWrapper<UserBoardRelation>()
                        .eq(UserBoardRelation::getBoardId, boardId)
                        .in(UserBoardRelation::getBoardRole, List.of("OWNER", "ADMIN")));
        for (UserBoardRelation m : managers) {
            User mgr = userMapper.selectById(m.getUserId());
            if (mgr != null && !"SYS_ADMIN".equals(mgr.getGlobalRole())) {
                sendSystemNotification(m.getUserId(), joinerName + " 加入了贴吧「" + board.getName() + "」");
            }
        }

        log.info("用户 {} 加入了贴吧 {}", userId, boardId);
    }

    @Transactional
    public void leaveBoard(Long boardId) {
        Long userId = getCurrentUserId();
        UserBoardRelation existing = relationMapper.selectOne(
                new LambdaQueryWrapper<UserBoardRelation>()
                        .eq(UserBoardRelation::getUserId, userId)
                        .eq(UserBoardRelation::getBoardId, boardId));
        if (existing == null) {
            throw new BusinessException("您不在该贴吧中");
        }
        if ("OWNER".equals(existing.getBoardRole())) {
            throw new BusinessException("吧主不能退出，请先转让吧主或删除贴吧");
        }
        relationMapper.deleteById(existing.getId());
        // 通知吧主和管理员：成员退出
        User leaver = userMapper.selectById(userId);
        String leaverName = leaver != null ? leaver.getUsername() : "未知用户";
        Board board = boardMapper.selectById(boardId);
        String boardName = board != null ? board.getName() : "未知吧";
        List<UserBoardRelation> managers = relationMapper.selectList(
                new LambdaQueryWrapper<UserBoardRelation>()
                        .eq(UserBoardRelation::getBoardId, boardId)
                        .in(UserBoardRelation::getBoardRole, List.of("OWNER", "ADMIN")));
        for (UserBoardRelation m : managers) {
            // 过滤系统管理员（用户自己退出时也过滤掉自己）
            User mgr = userMapper.selectById(m.getUserId());
            if (mgr != null && !"SYS_ADMIN".equals(mgr.getGlobalRole())) {
                sendSystemNotification(m.getUserId(), leaverName + " 退出了贴吧「" + boardName + "」");
            }
        }
        log.info("用户 {} 离开了贴吧 {}", userId, boardId);
    }

    @Transactional
    public void addActivity(Long userId, Long boardId, int points) {
        UserBoardRelation rel = relationMapper.selectOne(
                new LambdaQueryWrapper<UserBoardRelation>()
                        .eq(UserBoardRelation::getUserId, userId)
                        .eq(UserBoardRelation::getBoardId, boardId));
        if (rel != null) {
            UserBoardRelation update = new UserBoardRelation();
            update.setId(rel.getId());
            update.setActivityPoints((rel.getActivityPoints() != null ? rel.getActivityPoints() : 0) + points);
            relationMapper.updateById(update);
        }
    }

    // ==================== 排行榜 ====================

    public List<Map<String, Object>> getLeaderboard(Long boardId) {
        List<UserBoardRelation> top = relationMapper.selectList(
                new LambdaQueryWrapper<UserBoardRelation>()
                        .eq(UserBoardRelation::getBoardId, boardId)
                        .orderByDesc(UserBoardRelation::getActivityPoints)
                        .last("LIMIT 50"));

        List<Map<String, Object>> list = new ArrayList<>();
        int rank = 1;
        for (UserBoardRelation rel : top) {
            User u = userMapper.selectById(rel.getUserId());
            if (u == null) continue;
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("rank", rank++);
            item.put("userId", u.getId());
            item.put("username", u.getUsername());
            item.put("avatar", u.getAvatar());
            item.put("activityPoints", rel.getActivityPoints() != null ? rel.getActivityPoints() : 0);
            item.put("boardRole", rel.getBoardRole());
            list.add(item);
        }
        return list;
    }

    public List<Map<String, Object>> getUserBoards(Long userId) {
        List<UserBoardRelation> rels = relationMapper.selectList(
                new LambdaQueryWrapper<UserBoardRelation>()
                        .eq(UserBoardRelation::getUserId, userId));
        Set<Long> seen = new HashSet<>();
        List<Map<String, Object>> list = new ArrayList<>();
        for (UserBoardRelation rel : rels) {
            if (!seen.add(rel.getBoardId())) continue;
            Board board = boardMapper.selectById(rel.getBoardId());
            if (board == null || "DELETED".equals(board.getStatus())) continue;
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("boardId", board.getId());
            item.put("boardName", board.getName());
            item.put("boardAvatar", board.getAvatar());
            item.put("boardRole", rel.getBoardRole());
            item.put("activityPoints", rel.getActivityPoints() != null ? rel.getActivityPoints() : 0);
            list.add(item);
        }
        return list;
    }

    // ==================== 查询 ====================

    public List<Map<String, Object>> getAdmins(Long boardId) {
        List<UserBoardRelation> admins = relationMapper.selectList(
                new LambdaQueryWrapper<UserBoardRelation>()
                        .eq(UserBoardRelation::getBoardId, boardId)
                        .eq(UserBoardRelation::getBoardRole, "ADMIN"));
        List<Map<String, Object>> list = new ArrayList<>();
        for (UserBoardRelation rel : admins) {
            User u = userMapper.selectById(rel.getUserId());
            if (u == null) continue;
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("userId", u.getId());
            item.put("username", u.getUsername());
            item.put("avatar", u.getAvatar());
            list.add(item);
        }
        return list;
    }

    public Map<String, Object> getMembers(Long boardId, String keyword, Integer page, Integer size) {
        int pageNum = page != null ? page : 1;
        int pageSize = size != null ? size : 20;

        // 先收集该吧所有用户的ID
        List<UserBoardRelation> allRels = relationMapper.selectList(
                new LambdaQueryWrapper<UserBoardRelation>()
                        .eq(UserBoardRelation::getBoardId, boardId));

        List<Map<String, Object>> filtered = new ArrayList<>();
        for (UserBoardRelation rel : allRels) {
            User u = userMapper.selectById(rel.getUserId());
            if (u == null) continue;
            if (keyword != null && !keyword.isEmpty()
                    && !u.getUsername().contains(keyword)) {
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("userId", u.getId());
            item.put("username", u.getUsername());
            item.put("avatar", u.getAvatar());
            item.put("boardRole", rel.getBoardRole());
            item.put("activityPoints", rel.getActivityPoints() != null ? rel.getActivityPoints() : 0);
            item.put("joinTime", rel.getJoinTime());
            filtered.add(item);
        }

        // 手动分页
        int total = filtered.size();
        int fromIndex = Math.min((pageNum - 1) * pageSize, total);
        int toIndex = Math.min(fromIndex + pageSize, total);
        List<Map<String, Object>> pageData = filtered.subList(fromIndex, toIndex);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("records", pageData);
        result.put("total", total);
        result.put("page", pageNum);
        result.put("size", pageSize);
        return result;
    }

    public Map<String, Object> getUserBoardRole(Long boardId) {
        Long userId = getCurrentUserId();
        UserBoardRelation rel = relationMapper.selectOne(
                new LambdaQueryWrapper<UserBoardRelation>()
                        .eq(UserBoardRelation::getUserId, userId)
                        .eq(UserBoardRelation::getBoardId, boardId));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("role", rel != null ? rel.getBoardRole() : null);
        return result;
    }

    // ==================== 帖子/评论回收站 ====================

    public Map<String, Object> getHiddenDeletedPosts(Long boardId) {
        permissionService.checkBoardPermission(boardId, List.of("OWNER", "ADMIN"));
        List<Post> posts = postMapper.selectList(
                new LambdaQueryWrapper<Post>()
                        .eq(Post::getBoardId, boardId)
                        .and(w -> w.eq(Post::getStatus, "HIDDEN").or().eq(Post::getStatus, "DELETED").or().eq(Post::getStatus, "BLOCKED"))
                        .orderByDesc(Post::getUpdatedAt));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("records", posts);
        result.put("total", posts.size());
        return result;
    }

    @Transactional
    public void recoverPost(Long boardId, Long postId) {
        permissionService.checkBoardPermission(boardId, List.of("OWNER", "ADMIN"));
        Post post = postMapper.selectById(postId);
        if (post == null || !post.getBoardId().equals(boardId)) throw new BusinessException("帖子不存在");
        Post update = new Post();
        update.setId(postId);
        update.setStatus("ACTIVE");
        postMapper.updateById(update);
    }

    public List<Comment> getPostComments(Long boardId, Long postId) {
        permissionService.checkBoardPermission(boardId, List.of("OWNER", "ADMIN"));
        return commentMapper.selectList(
                new LambdaQueryWrapper<Comment>().eq(Comment::getPostId, postId).orderByAsc(Comment::getId));
    }

    @Transactional
    public void deleteCommentOnPost(Long boardId, Long commentId) {
        permissionService.checkBoardPermission(boardId, List.of("OWNER", "ADMIN"));
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null) throw new BusinessException("评论不存在");
        Comment update = new Comment();
        update.setId(commentId);
        update.setStatus("DELETED");
        commentMapper.updateById(update);
    }

    @Transactional
    public void recoverCommentOnPost(Long boardId, Long commentId) {
        permissionService.checkBoardPermission(boardId, List.of("OWNER", "ADMIN"));
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null) throw new BusinessException("评论不存在");
        Comment update = new Comment();
        update.setId(commentId);
        update.setStatus("ACTIVE");
        commentMapper.updateById(update);
    }

    public List<Map<String, Object>> getTrashComments(Long boardId) {
        permissionService.checkBoardPermission(boardId, List.of("OWNER", "ADMIN"));
        // 查询该吧下所有帖子
        List<Post> boardPosts = postMapper.selectList(
                new LambdaQueryWrapper<Post>().eq(Post::getBoardId, boardId));
        List<Long> postIds = boardPosts.stream().map(Post::getId).toList();

        if (postIds.isEmpty()) return Collections.emptyList();

        // 查询这些帖子中被删除的评论
        List<Comment> deleted = commentMapper.selectList(
                new LambdaQueryWrapper<Comment>()
                        .in(Comment::getPostId, postIds)
                        .and(w -> w.eq(Comment::getStatus, "DELETED").or().eq(Comment::getStatus, "BLOCKED"))
                        .orderByDesc(Comment::getUpdatedAt));

        List<Map<String, Object>> list = new ArrayList<>();
        for (Comment c : deleted) {
            User author = userMapper.selectById(c.getAuthorId());
            Post p = postMapper.selectById(c.getPostId());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", c.getId());
            item.put("content", c.getContent());
            item.put("postId", c.getPostId());
            item.put("postTitle", p != null ? p.getTitle() : "未知");
            item.put("authorId", c.getAuthorId());
            item.put("authorName", author != null ? author.getUsername() : "未知");
            item.put("status", c.getStatus());
            item.put("createdAt", c.getCreatedAt());
            list.add(item);
        }
        return list;
    }
}
