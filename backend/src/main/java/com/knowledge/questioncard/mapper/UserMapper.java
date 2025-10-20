package com.knowledge.questioncard.mapper;

import com.knowledge.questioncard.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface UserMapper {
    
    /**
     * 插入用户
     */
    int insert(User user);
    
    /**
     * 根据ID删除用户
     */
    int deleteById(Long id);
    
    /**
     * 更新用户信息
     */
    int updateById(User user);
    
    /**
     * 根据ID查询用户
     */
    User selectById(Long id);
    
    /**
     * 根据用户名查询用户
     */
    User selectByUsername(String username);
    
    /**
     * 根据邮箱查询用户
     */
    User selectByEmail(String email);
    
    /**
     * 根据手机号查询用户
     */
    User selectByPhone(String phone);
    
    /**
     * 分页查询用户
     */
    List<User> selectPage(@Param("keyword") String keyword,
                          @Param("status") Integer status,
                          @Param("offset") Integer offset,
                          @Param("limit") Integer limit);
    
    /**
     * 统计用户数量
     */
    int countUsers(@Param("keyword") String keyword,
                   @Param("status") Integer status);
    
    /**
     * 更新最后登录时间
     */
    int updateLastLoginAt(@Param("id") Long id, @Param("lastLoginAt") LocalDateTime lastLoginAt);
    
    /**
     * 更新用户状态
     */
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);
}