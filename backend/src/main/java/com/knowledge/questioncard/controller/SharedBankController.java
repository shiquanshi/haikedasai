package com.knowledge.questioncard.controller;

import com.knowledge.questioncard.common.Result;
import com.knowledge.questioncard.entity.SharedBank;
import com.knowledge.questioncard.service.SharedBankService;
import com.knowledge.questioncard.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/share")
public class SharedBankController {

    @Autowired
    private SharedBankService sharedBankService;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 创建或更新题库分享
     */
    @PostMapping("/create")
    public Result<SharedBank> createShare(
            @RequestBody Map<String, Object> params,
            HttpServletRequest request) {
        try {
            System.out.println("[分享更新] 收到请求参数: " + params);
            Long userId = getUserIdFromRequest(request);
            Long bankId = Long.parseLong(params.get("bankId").toString());
            Boolean isPublic = (Boolean) params.get("isPublic");
            String shareTitle = (String) params.get("shareTitle");
            String shareDescription = (String) params.get("shareDescription");
            
            // 解析到期时间（支持时间戳格式）
            Date expireAt = null;
            if (params.get("expireAt") != null && !params.get("expireAt").toString().isEmpty()) {
                try {
                    // 前端发送的是时间戳（毫秒数）
                    Long timestamp = Long.parseLong(params.get("expireAt").toString());
                    expireAt = new Date(timestamp);
                    System.out.println("[分享更新] 解析到期时间成功: " + expireAt);
                } catch (NumberFormatException e) {
                    System.out.println("[分享更新] 解析时间戳失败: " + e.getMessage());
                    // 如果解析失败，保持为null（永久有效）
                }
            } else {
                System.out.println("[分享更新] 到期时间为空，设置为永久有效");
            }

            SharedBank sharedBank = sharedBankService.createOrUpdateShare(
                bankId, userId, isPublic, shareTitle, shareDescription, expireAt
            );

            return Result.success(sharedBank);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("创建分享失败: " + e.getMessage());
        }
    }

    /**
     * 获取分享大厅列表(分页)
     */
    @GetMapping("/plaza")
    public Result<Map<String, Object>> getPlaza(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "created_at") String orderBy,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        try {
            Map<String, Object> result = sharedBankService.getPublicShares(
                null, null, keyword, orderBy, page, pageSize
            );
            return Result.success(result);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取分享大厅失败: " + e.getMessage());
        }
    }

    /**
     * 根据分享码获取分享详情
     */
    @GetMapping("/detail/{shareCode}")
    public Result<SharedBank> getShareDetail(
            @PathVariable String shareCode,
            HttpServletRequest request) {
        try {
            SharedBank sharedBank = sharedBankService.getByShareCode(shareCode);
            if (sharedBank == null) {
                return Result.error("分享不存在或已过期");
            }

            // 记录浏览(UV/PV统计)
            Long viewerId = null;
            try {
                viewerId = getUserIdFromRequest(request);
            } catch (Exception ignored) {
                // 未登录用户
            }

            String viewerIp = getClientIp(request);
            String userAgent = request.getHeader("User-Agent");
            String deviceType = userAgent != null && userAgent.toLowerCase().contains("mobile") ? "Mobile" : "PC";

            sharedBankService.recordView(
                sharedBank.getId(), viewerId, viewerIp, userAgent, deviceType
            );

            return Result.success(sharedBank);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取分享详情失败: " + e.getMessage());
        }
    }

    /**
     * 获取浏览统计数据
     */
    @GetMapping("/statistics/{sharedBankId}")
    public Result<Map<String, Object>> getStatistics(
            @PathVariable Long sharedBankId,
            HttpServletRequest request) {
        try {
            Long userId = getUserIdFromRequest(request);
            // 这里可以添加权限验证,确保只有分享者可以查看统计

            Map<String, Object> statistics = sharedBankService.getViewStatistics(sharedBankId);
            return Result.success(statistics);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取统计数据失败: " + e.getMessage());
        }
    }

    /**
     * 增加导入/复制次数
     */
    @PostMapping("/increment-copy/{shareCode}")
    public Result<Void> incrementCopyCount(
            @PathVariable String shareCode) {
        try {
            sharedBankService.incrementCopyCount(shareCode);
            return Result.success(null);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("更新导入次数失败: " + e.getMessage());
        }
    }

    /**
     * 按日期获取浏览统计
     */
    @GetMapping("/statistics/{sharedBankId}/by-date")
    public Result<List<Map<String, Object>>> getStatisticsByDate(
            @PathVariable Long sharedBankId,
            @RequestParam String startDate,
            @RequestParam String endDate,
            HttpServletRequest request) {
        try {
            Long userId = getUserIdFromRequest(request);

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            Date start = sdf.parse(startDate);
            Date end = sdf.parse(endDate);

            List<Map<String, Object>> statistics = sharedBankService.getViewsByDate(
                sharedBankId, start, end
            );
            return Result.success(statistics);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取统计数据失败: " + e.getMessage());
        }
    }

    /**
     * 获取用户的分享列表
     */
    @GetMapping("/my-shares")
    public Result<List<Map<String, Object>>> getMyShares(HttpServletRequest request) {
        try {
            Long userId = getUserIdFromRequest(request);
            List<Map<String, Object>> shares = sharedBankService.getUserShares(userId);
            return Result.success(shares);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取分享列表失败: " + e.getMessage());
        }
    }

    /**
     * 删除分享
     */
    @DeleteMapping("/delete/{shareId}")
    public Result<String> deleteShare(
            @PathVariable Long shareId,
            HttpServletRequest request) {
        try {
            Long userId = getUserIdFromRequest(request);
            // 验证权限，确保只有分享者可以删除
            sharedBankService.deleteShare(shareId, userId);
            return Result.success("删除成功");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("删除分享失败: " + e.getMessage());
        }
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

    /**
     * 获取客户端IP地址
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}