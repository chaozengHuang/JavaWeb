package com.hcz.nexusbackend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hcz.nexusbackend.entity.FriendRelation;
import com.hcz.nexusbackend.entity.User;
import com.hcz.nexusbackend.exception.BusinessException;
import com.hcz.nexusbackend.handler.ChatWebSocketHandler;
import com.hcz.nexusbackend.mapper.FriendRelationMapper;
import com.hcz.nexusbackend.mapper.UserMapper;
import com.hcz.nexusbackend.util.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
public class FriendService {

    @Autowired
    private FriendRelationMapper friendRelationMapper;

    @Autowired
    private UserMapper userMapper;

    private Long getCurrentUserId() {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            throw new BusinessException(401, "请先登录");
        }
        return userId;
    }

    @Transactional
    public void sendRequest(Long friendId) {
        Long userId = getCurrentUserId();
        if (userId.equals(friendId)) {
            throw new BusinessException("不能添加自己为好友");
        }

        User friend = userMapper.selectById(friendId);
        if (friend == null) {
            throw new BusinessException("目标用户不存在");
        }

        // 检查是否已存在记录
        FriendRelation existing = friendRelationMapper.selectOne(
                new LambdaQueryWrapper<FriendRelation>()
                        .eq(FriendRelation::getUserId, userId)
                        .eq(FriendRelation::getFriendId, friendId));
        if (existing != null) {
            if ("ACCEPTED".equals(existing.getStatus())) {
                throw new BusinessException("已经是好友");
            }
            if ("PENDING".equals(existing.getStatus())) {
                throw new BusinessException("好友请求已发送，请等待对方确认");
            }
            // 如果是 REJECTED，允许重新发送
            existing.setStatus("PENDING");
            existing.setCreatedAt(LocalDateTime.now());
            friendRelationMapper.updateById(existing);
            log.info("用户 {} 重新发送好友请求给 {}", userId, friendId);
            return;
        }

        // 检查对方是否已向我发送请求
        FriendRelation reverse = friendRelationMapper.selectOne(
                new LambdaQueryWrapper<FriendRelation>()
                        .eq(FriendRelation::getUserId, friendId)
                        .eq(FriendRelation::getFriendId, userId));
        if (reverse != null && "PENDING".equals(reverse.getStatus())) {
            // 直接接受对方的请求
            reverse.setStatus("ACCEPTED");
            friendRelationMapper.updateById(reverse);
            // 同时插入自己的记录
            FriendRelation mine = new FriendRelation();
            mine.setUserId(userId);
            mine.setFriendId(friendId);
            mine.setStatus("ACCEPTED");
            friendRelationMapper.insert(mine);
            log.info("用户 {} 和 {} 成为好友（自动接受）", userId, friendId);
            return;
        }

        FriendRelation fr = new FriendRelation();
        fr.setUserId(userId);
        fr.setFriendId(friendId);
        fr.setStatus("PENDING");
        friendRelationMapper.insert(fr);
        log.info("用户 {} 发送好友请求给 {}", userId, friendId);
    }

    @Transactional
    public void acceptRequest(Long requestId) {
        Long userId = getCurrentUserId();
        FriendRelation fr = friendRelationMapper.selectById(requestId);
        if (fr == null) {
            throw new BusinessException("请求不存在");
        }
        if (!fr.getFriendId().equals(userId)) {
            throw new BusinessException("无权处理该请求");
        }
        if (!"PENDING".equals(fr.getStatus())) {
            throw new BusinessException("请求已处理");
        }

        fr.setStatus("ACCEPTED");
        friendRelationMapper.updateById(fr);

        // 在对方记录中创建好友关系（如果不存在）
        FriendRelation reverse = friendRelationMapper.selectOne(
                new LambdaQueryWrapper<FriendRelation>()
                        .eq(FriendRelation::getUserId, fr.getFriendId())
                        .eq(FriendRelation::getFriendId, fr.getUserId()));
        if (reverse == null) {
            reverse = new FriendRelation();
            reverse.setUserId(fr.getFriendId());
            reverse.setFriendId(fr.getUserId());
            reverse.setStatus("ACCEPTED");
            friendRelationMapper.insert(reverse);
        } else {
            reverse.setStatus("ACCEPTED");
            friendRelationMapper.updateById(reverse);
        }

        log.info("用户 {} 接受了 {} 的好友请求", userId, fr.getUserId());
    }

    @Transactional
    public void rejectRequest(Long requestId) {
        Long userId = getCurrentUserId();
        FriendRelation fr = friendRelationMapper.selectById(requestId);
        if (fr == null) {
            throw new BusinessException("请求不存在");
        }
        if (!fr.getFriendId().equals(userId)) {
            throw new BusinessException("无权处理该请求");
        }
        fr.setStatus("REJECTED");
        friendRelationMapper.updateById(fr);
        log.info("用户 {} 拒绝了 {} 的好友请求", userId, fr.getUserId());
    }

    @Transactional
    public void deleteFriend(Long friendId) {
        Long userId = getCurrentUserId();

        // 禁止删除通知管理员
        User friend = userMapper.selectById(friendId);
        if (friend != null && "NOTIFY_ADMIN".equals(friend.getGlobalRole())) {
            throw new BusinessException("不能删除通知管理员");
        }

        // 删除双向记录
        FriendRelation mine = friendRelationMapper.selectOne(
                new LambdaQueryWrapper<FriendRelation>()
                        .eq(FriendRelation::getUserId, userId)
                        .eq(FriendRelation::getFriendId, friendId));
        if (mine != null) {
            friendRelationMapper.deleteById(mine.getId());
        }

        FriendRelation theirs = friendRelationMapper.selectOne(
                new LambdaQueryWrapper<FriendRelation>()
                        .eq(FriendRelation::getUserId, friendId)
                        .eq(FriendRelation::getFriendId, userId));
        if (theirs != null) {
            friendRelationMapper.deleteById(theirs.getId());
        }

        log.info("用户 {} 删除了好友 {}", userId, friendId);
    }

    public List<Map<String, Object>> getFriendList() {
        Long userId = getCurrentUserId();
        ensureAdminFriends(userId);
        List<FriendRelation> relations = friendRelationMapper.selectAcceptedFriends(userId);

        Set<Long> seen = new HashSet<>();
        // 自己始终是好友列表第一位
        User self = userMapper.selectById(userId);
        List<Map<String, Object>> friends = new ArrayList<>();
        if (self != null) {
            Map<String, Object> selfItem = new LinkedHashMap<>();
            selfItem.put("relationId", 0L);
            selfItem.put("userId", self.getId());
            selfItem.put("username", self.getUsername());
            selfItem.put("avatar", self.getAvatar());
            selfItem.put("globalRole", self.getGlobalRole());
            selfItem.put("online", true);
            selfItem.put("isSelf", true);
            friends.add(selfItem);
            seen.add(userId);
        }
        for (FriendRelation fr : relations) {
            Long friendId = fr.getUserId().equals(userId) ? fr.getFriendId() : fr.getUserId();
            if (!seen.add(friendId)) continue; // 防止双向记录导致重复
            User user = userMapper.selectById(friendId);
            if (user == null) continue;

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("relationId", fr.getId());
            item.put("userId", user.getId());
            item.put("username", user.getUsername());
            item.put("avatar", user.getAvatar());
            item.put("globalRole", user.getGlobalRole());
            item.put("online", ChatWebSocketHandler.isUserOnline(friendId));
            friends.add(item);
        }
        return friends;
    }

    public List<Map<String, Object>> getPendingRequests() {
        Long userId = getCurrentUserId();
        List<FriendRelation> requests = friendRelationMapper.selectPendingRequests(userId);

        List<Map<String, Object>> result = new ArrayList<>();
        for (FriendRelation fr : requests) {
            User user = userMapper.selectById(fr.getUserId());
            if (user == null) continue;

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", fr.getId());
            item.put("userId", user.getId());
            item.put("username", user.getUsername());
            item.put("avatar", user.getAvatar());
            item.put("createdAt", fr.getCreatedAt());
            result.add(item);
        }
        return result;
    }

    public Map<String, Object> getPendingCount() {
        Long userId = getCurrentUserId();
        int count = friendRelationMapper.countPendingRequests(userId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("count", count);
        return result;
    }

    public String getFriendStatus(Long otherUserId) {
        Long userId = getCurrentUserId();
        if (userId == null) return "none";
        // 是否已是好友
        Long acceptedCount = friendRelationMapper.selectCount(
                new LambdaQueryWrapper<FriendRelation>()
                        .eq(FriendRelation::getUserId, userId)
                        .eq(FriendRelation::getFriendId, otherUserId)
                        .eq(FriendRelation::getStatus, "ACCEPTED"));
        if (acceptedCount > 0) return "accepted";
        // 我是否已发送请求等待对方通过
        Long pendingSent = friendRelationMapper.selectCount(
                new LambdaQueryWrapper<FriendRelation>()
                        .eq(FriendRelation::getUserId, userId)
                        .eq(FriendRelation::getFriendId, otherUserId)
                        .eq(FriendRelation::getStatus, "PENDING"));
        if (pendingSent > 0) return "pending_sent";
        // 对方是否已向我发送请求
        Long pendingReceived = friendRelationMapper.selectCount(
                new LambdaQueryWrapper<FriendRelation>()
                        .eq(FriendRelation::getUserId, otherUserId)
                        .eq(FriendRelation::getFriendId, userId)
                        .eq(FriendRelation::getStatus, "PENDING"));
        if (pendingReceived > 0) return "pending_received";
        return "none";
    }

    public boolean isFriend(Long otherUserId) {
        return "accepted".equals(getFriendStatus(otherUserId));
    }

    private void ensureAdminFriends(Long userId) {
        try {
            // 只查找 NOTIFY_ADMIN，确保当前用户和通知管理员互为好友
            List<User> admins = userMapper.selectList(
                    new LambdaQueryWrapper<User>().eq(User::getGlobalRole, "NOTIFY_ADMIN"));
            for (User admin : admins) {
                if (admin.getId().equals(userId)) continue;
                // 检查是否已有好友关系
                Long exists = friendRelationMapper.selectCount(
                        new LambdaQueryWrapper<FriendRelation>()
                                .eq(FriendRelation::getUserId, userId)
                                .eq(FriendRelation::getFriendId, admin.getId())
                                .eq(FriendRelation::getStatus, "ACCEPTED"));
                if (exists == 0) {
                    // 双向创建好友关系
                    FriendRelation f1 = new FriendRelation();
                    f1.setUserId(userId); f1.setFriendId(admin.getId()); f1.setStatus("ACCEPTED");
                    friendRelationMapper.insert(f1);
                    FriendRelation f2 = new FriendRelation();
                    f2.setUserId(admin.getId()); f2.setFriendId(userId); f2.setStatus("ACCEPTED");
                    friendRelationMapper.insert(f2);
                }
            }
        } catch (Exception ignored) {}
    }
}
