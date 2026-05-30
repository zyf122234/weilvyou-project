package com.travel.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.travel.user.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * User mapper.
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    @Select("""
            SELECT
                u.id,
                u.username,
                u.password,
                u.nickname,
                u.avatar_url AS avatarUrl,
                u.email,
                u.phone,
                u.balance,
                u.status,
                u.create_time AS createTime,
                GROUP_CONCAT(r.role_code ORDER BY r.id SEPARATOR ',') AS roles
            FROM sys_user u
            LEFT JOIN sys_user_role ur ON ur.user_id = u.id
            LEFT JOIN sys_role r ON r.id = ur.role_id
            WHERE u.username = #{username}
              AND u.deleted = 0
            GROUP BY
                u.id,
                u.username,
                u.password,
                u.nickname,
                u.avatar_url,
                u.email,
                u.phone,
                u.balance,
                u.status,
                u.create_time
            LIMIT 1
            """)
    LoginUserRecord selectLoginUserByUsername(String username);

    @Select("SELECT r.role_code FROM sys_role r " +
            "INNER JOIN sys_user_role ur ON r.id = ur.role_id " +
            "WHERE ur.user_id = #{userId}")
    List<String> selectRoleCodesByUserId(Long userId);
}
