package com.travel.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.travel.user.entity.SysUserRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Delete;

/**
 * 用户角色Mapper
 */
@Mapper
public interface SysUserRoleMapper extends BaseMapper<SysUserRole> {

    @Delete("DELETE FROM sys_user_role WHERE user_id = #{userId}")
    int deleteByUserId(Long userId);
}
