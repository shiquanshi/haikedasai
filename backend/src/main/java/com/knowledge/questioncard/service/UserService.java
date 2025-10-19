package com.knowledge.questioncard.service;

import com.knowledge.questioncard.dto.*;
import com.knowledge.questioncard.entity.User;
import com.knowledge.questioncard.mapper.UserMapper;
import com.knowledge.questioncard.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    
    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    
    /**
     * 用户注册
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // 1. 验证用户名是否已存在
        if (userMapper.selectByUsername(request.getUsername()) != null) {
            throw new RuntimeException("用户名已存在");
        }
        
        // 2. 验证邮箱是否已存在
        if (userMapper.selectByEmail(request.getEmail()) != null) {
            throw new RuntimeException("邮箱已被注册");
        }
        
        // 3. 验证手机号是否已存在（如果提供）
        if (request.getPhone() != null && !request.getPhone().isEmpty()) {
            if (userMapper.selectByPhone(request.getPhone()) != null) {
                throw new RuntimeException("手机号已被注册");
            }
        }
        
        // 4. 创建用户
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(encryptPassword(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setNickname(request.getNickname() != null ? request.getNickname() : request.getUsername());
        user.setPhone(request.getPhone());
        user.setRole("user");
        user.setStatus(1);
        
        // 5. 分配租户ID（如果没有邀请码，创建新租户；如果有邀请码，加入对应租户）
        if (request.getInviteCode() != null && !request.getInviteCode().isEmpty()) {
            // TODO: 实现邀请码逻辑，这里暂时使用邀请码作为租户ID
            try {
                user.setTenantId(Long.parseLong(request.getInviteCode()));
            } catch (NumberFormatException e) {
                throw new RuntimeException("无效的邀请码");
            }
        } else {
            // 新用户创建新租户，使用用户ID作为租户ID（插入后会自动生成）
            user.setTenantId(null); // 先设为null，插入后更新
        }
        
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        
        // 6. 插入用户
        userMapper.insert(user);
        
        // 7. 如果是新租户，更新租户ID为用户ID
        if (user.getTenantId() == null) {
            user.setTenantId(user.getId());
            userMapper.updateById(user);
        }
        
        // 8. 生成Token
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getTenantId(), user.getRole());
        
        // 9. 返回认证响应
        return new AuthResponse(token, convertToDTO(user));
    }
    
    /**
     * 用户登录
     */
    @Transactional
    public AuthResponse login(LoginRequest request) {
        // 1. 查找用户（支持用户名、邮箱、手机号登录）
        User user = userMapper.selectByUsername(request.getUsername());
        if (user == null) {
            user = userMapper.selectByEmail(request.getUsername());
        }
        if (user == null) {
            user = userMapper.selectByPhone(request.getUsername());
        }
        
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        
        // 2. 验证密码
        if (!user.getPassword().equals(encryptPassword(request.getPassword()))) {
            throw new RuntimeException("密码错误");
        }
        
        // 3. 检查用户状态
        if (user.getStatus() != 1) {
            throw new RuntimeException("账号已被禁用");
        }
        
        // 4. 更新最后登录时间
        userMapper.updateLastLoginAt(user.getId(), LocalDateTime.now());
        
        // 5. 生成Token
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getTenantId(), user.getRole());
        
        // 6. 返回认证响应
        return new AuthResponse(token, convertToDTO(user));
    }
    
    /**
     * 获取用户信息
     */
    public UserDTO getUserInfo(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        return convertToDTO(user);
    }
    
    /**
     * 更新用户信息
     */
    @Transactional
    public UserDTO updateUserInfo(Long userId, UserDTO userDTO) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        
        // 只允许更新部分字段
        if (userDTO.getNickname() != null) {
            user.setNickname(userDTO.getNickname());
        }
        if (userDTO.getAvatar() != null) {
            user.setAvatar(userDTO.getAvatar());
        }
        if (userDTO.getPhone() != null) {
            user.setPhone(userDTO.getPhone());
        }
        
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        
        return convertToDTO(user);
    }
    
    /**
     * 分页查询租户用户
     */
    public PageResponse<UserDTO> getUsersByTenant(Long tenantId, String keyword, Integer status, PageRequest pageRequest) {
        int offset = (pageRequest.getPage() - 1) * pageRequest.getPageSize();
        
        List<User> users = userMapper.selectPage(tenantId, keyword, status, offset, pageRequest.getPageSize());
        int total = userMapper.countUsers(tenantId, keyword, status);
        
        List<UserDTO> userDTOs = users.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        
        return new PageResponse<>(userDTOs, (long) total, pageRequest.getPage(), pageRequest.getPageSize());
    }
    
    /**
     * 修改密码
     */
    @Transactional
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        
        // 验证旧密码
        if (!user.getPassword().equals(encryptPassword(oldPassword))) {
            throw new RuntimeException("原密码错误");
        }
        
        // 更新密码
        user.setPassword(encryptPassword(newPassword));
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
    }
    
    /**
     * 密码加密（使用MD5）
     */
    private String encryptPassword(String password) {
        return DigestUtils.md5DigestAsHex(password.getBytes(StandardCharsets.UTF_8));
    }
    
    /**
     * 转换为DTO
     */
    private UserDTO convertToDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setNickname(user.getNickname());
        dto.setPhone(user.getPhone());
        dto.setAvatar(user.getAvatar());
        dto.setRole(user.getRole());
        dto.setStatus(user.getStatus());
        dto.setTenantId(user.getTenantId());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setLastLoginAt(user.getLastLoginAt());
        return dto;
    }
}