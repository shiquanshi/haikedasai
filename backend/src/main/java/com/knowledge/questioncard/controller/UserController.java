package com.knowledge.questioncard.controller;

import com.knowledge.questioncard.dto.*;
import com.knowledge.questioncard.service.UserService;
import com.knowledge.questioncard.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;

import jakarta.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class UserController {
    
    private final UserService userService;
    private final JwtUtil jwtUtil;
    
    /**
     * 用户注册
     */
    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        log.info("用户注册: {}", request.getUsername());
        return userService.register(request);
    }
    
    /**
     * 用户登录
     */
    @PostMapping("/login")
    public AuthResponse login(@RequestBody LoginRequest request) {
        log.info("用户登录: {}", request.getUsername());
        return userService.login(request);
    }
    
    /**
     * 获取当前用户信息
     */
    @GetMapping("/info")
    public UserDTO getCurrentUserInfo(HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        return userService.getUserInfo(userId);
    }
    
    /**
     * 更新当前用户信息
     */
    @PutMapping("/info")
    public UserDTO updateCurrentUserInfo(HttpServletRequest request, @RequestBody UserDTO userDTO) {
        Long userId = getUserIdFromRequest(request);
        return userService.updateUserInfo(userId, userDTO);
    }
    
    /**
     * 修改密码
     */
    @PostMapping("/change-password")
    public Map<String, String> changePassword(HttpServletRequest request, @RequestBody Map<String, String> passwordMap) {
        Long userId = getUserIdFromRequest(request);
        String oldPassword = passwordMap.get("oldPassword");
        String newPassword = passwordMap.get("newPassword");
        
        userService.changePassword(userId, oldPassword, newPassword);
        
        Map<String, String> result = new HashMap<>();
        result.put("message", "密码修改成功");
        return result;
    }
    
    /**
     * 分页查询用户(管理员功能)
     */
    @GetMapping("/list")
    public PageResponse<UserDTO> getUserList(
            HttpServletRequest request,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        PageRequest pageRequest = new PageRequest();
        pageRequest.setPage(page);
        pageRequest.setPageSize(size);
        
        return userService.getUsers(keyword, status, pageRequest);
    }
    
    /**
     * 验证Token
     */
    @GetMapping("/validate-token")
    public Map<String, Object> validateToken(HttpServletRequest request) {
        String token = extractToken(request);
        Map<String, Object> result = new HashMap<>();
        
        if (token != null && jwtUtil.validateToken(token)) {
            result.put("valid", true);
            result.put("userId", jwtUtil.getUserIdFromToken(token));
            result.put("username", jwtUtil.getUsernameFromToken(token));
            result.put("role", jwtUtil.getRoleFromToken(token));
        } else {
            result.put("valid", false);
        }
        
        return result;
    }
    
    /**
     * 从请求中获取用户ID
     */
    private Long getUserIdFromRequest(HttpServletRequest request) {
        String token = extractToken(request);
        if (token == null || !jwtUtil.validateToken(token)) {
            throw new RuntimeException("无效的token");
        }
        return jwtUtil.getUserIdFromToken(token);
    }
    
    /**
     * 从请求头中提取Token
     */
    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}