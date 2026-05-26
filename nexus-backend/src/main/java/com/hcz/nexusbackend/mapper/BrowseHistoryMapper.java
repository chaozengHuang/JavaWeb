package com.hcz.nexusbackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hcz.nexusbackend.entity.BrowseHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface BrowseHistoryMapper extends BaseMapper<BrowseHistory> {

    @Select("SELECT post_id FROM browse_history WHERE user_id = #{userId} GROUP BY post_id ORDER BY MAX(browse_time) DESC")
    List<Long> selectHistoryPostIds(@Param("userId") Long userId);
}
