package com.hcz.nexusbackend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hcz.nexusbackend.entity.UserBoardRelation;
import com.hcz.nexusbackend.exception.BusinessException;
import com.hcz.nexusbackend.mapper.UserBoardRelationMapper;
import com.hcz.nexusbackend.util.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PermissionService {

    private static final String ROLE_SYS_ADMIN = "SYS_ADMIN";

    @Autowired
    private UserBoardRelationMapper boardRelationMapper;

    public void checkIsSysAdmin() {
        String role = SecurityUtils.getGlobalRole();
        if (!ROLE_SYS_ADMIN.equals(role)) {
            throw new BusinessException(403, "权限不足：需要系统管理员权限");
        }
    }

    public void checkBoardPermission(Long boardId, List<String> allowedRoles) {
        String globalRole = SecurityUtils.getGlobalRole();
        if (ROLE_SYS_ADMIN.equals(globalRole)) {
            return;
        }
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            throw new BusinessException(401, "请先登录");
        }
        UserBoardRelation relation = boardRelationMapper.selectOne(
                new LambdaQueryWrapper<UserBoardRelation>()
                        .eq(UserBoardRelation::getUserId, userId)
                        .eq(UserBoardRelation::getBoardId, boardId)
        );
        if (relation == null || !allowedRoles.contains(relation.getBoardRole())) {
            throw new BusinessException(403, "权限不足：没有该板块的操作权限");
        }
    }
}
