package com.knowledge.questioncard.service;

import com.knowledge.questioncard.entity.BankShareView;
import com.knowledge.questioncard.entity.SharedBank;
import com.knowledge.questioncard.mapper.BankShareViewMapper;
import com.knowledge.questioncard.mapper.QuestionBankMapper;
import com.knowledge.questioncard.mapper.SharedBankMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class SharedBankService {

    @Autowired
    private SharedBankMapper sharedBankMapper;

    @Autowired
    private BankShareViewMapper bankShareViewMapper;

    @Autowired
    private QuestionBankMapper questionBankMapper;

    /**
     * 创建或更新题库分享
     */
    @Transactional
    public SharedBank createOrUpdateShare(Long bankId, Long userId, Boolean isPublic, 
                                          String shareTitle, String shareDescription, Date expireAt) {
        // 检查是否已经分享过
        SharedBank existingShare = sharedBankMapper.selectByBankIdAndUserId(bankId, userId);
        
        if (existingShare != null) {
            // 更新现有分享
            existingShare.setIsPublic(isPublic);
            existingShare.setShareTitle(shareTitle);
            existingShare.setShareDescription(shareDescription);
            existingShare.setExpireAt(expireAt);  // 添加到期时间
            existingShare.setUpdatedAt(new Date());
            sharedBankMapper.update(existingShare);
            return existingShare;
        } else {
            // 创建新分享
            SharedBank sharedBank = new SharedBank();
            sharedBank.setBankId(bankId);
            sharedBank.setUserId(userId);
            sharedBank.setShareCode(generateShareCode());
            sharedBank.setIsPublic(isPublic);
            sharedBank.setShareTitle(shareTitle);
            sharedBank.setShareDescription(shareDescription);
            sharedBank.setExpireAt(expireAt);  // 添加到期时间
            sharedBank.setViewCount(0);
            sharedBank.setUniqueViewCount(0);
            sharedBank.setFavoriteCount(0);
            sharedBank.setCopyCount(0);
            sharedBank.setStatus(1);
            sharedBank.setCreatedAt(new Date());
            sharedBankMapper.insert(sharedBank);
            return sharedBank;
        }
    }

    /**
     * 生成唯一的分享码
     */
    private String generateShareCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder code = new StringBuilder();
        Random random = new Random();
        
        for (int i = 0; i < 8; i++) {
            code.append(chars.charAt(random.nextInt(chars.length())));
        }
        
        // 检查是否重复
        if (sharedBankMapper.selectByShareCode(code.toString()) != null) {
            return generateShareCode(); // 递归生成新的
        }
        
        return code.toString();
    }

    /**
     * 获取分享大厅列表(分页)
     */
    public Map<String, Object> getPublicShares(String topic, String difficulty, String keyword,
                                                String orderBy, int page, int pageSize) {
        int offset = (page - 1) * pageSize;
        
        List<Map<String, Object>> shares = sharedBankMapper.selectPublicShares(
            topic, difficulty, keyword, orderBy, offset, pageSize
        );
        
        // 处理 shareTitle 和 shareDescription,如果为null则使用题库的字段
        for (Map<String, Object> share : shares) {
            if (share.get("shareTitle") == null) {
                share.put("shareTitle", share.get("bankName"));
            }
            if (share.get("shareDescription") == null) {
                share.put("shareDescription", share.get("bankDescription"));
            }
        }
        
        int total = sharedBankMapper.countPublicShares(topic, difficulty, keyword);
        
        Map<String, Object> result = new HashMap<>();
        result.put("list", shares);
        result.put("total", total);
        result.put("page", page);
        result.put("pageSize", pageSize);
        result.put("totalPages", (int) Math.ceil((double) total / pageSize));
        
        return result;
    }

    /**
     * 根据分享码获取分享详情
     */
    public SharedBank getByShareCode(String shareCode) {
        return sharedBankMapper.selectByShareCode(shareCode);
    }

    /**
     * 记录浏览(UV/PV统计)
     */
    @Transactional
    public void recordView(Long sharedBankId, Long viewerId, String viewerIp, 
                          String userAgent, String deviceType) {
        // 记录浏览
        BankShareView view = new BankShareView();
        view.setSharedBankId(sharedBankId);
        view.setViewerId(viewerId);
        view.setViewerIp(viewerIp);
        view.setUserAgent(userAgent);
        view.setDeviceType(deviceType);
        view.setViewDuration(0);
        view.setCreatedAt(new Date());
        bankShareViewMapper.insert(view);
        
        // 增加PV
        sharedBankMapper.incrementViewCount(sharedBankId);
        
        // 检查是否是新访客(24小时内)
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.HOUR, -24);
        Date timeLimit = cal.getTime();
        
        int recentViews = bankShareViewMapper.countRecentViews(
            sharedBankId, viewerId, viewerIp, timeLimit
        );
        
        // 如果是新访客,增加UV
        if (recentViews == 1) { // 只有当前这条记录
            sharedBankMapper.incrementUniqueViewCount(sharedBankId);
        }
    }

    /**
     * 获取浏览统计数据
     */
    public Map<String, Object> getViewStatistics(Long sharedBankId) {
        return bankShareViewMapper.getViewStatistics(sharedBankId);
    }

    /**
     * 按日期获取浏览统计
     */
    public List<Map<String, Object>> getViewsByDate(Long sharedBankId, Date startDate, Date endDate) {
        return bankShareViewMapper.getViewsByDate(sharedBankId, startDate, endDate);
    }

    /**
     * 获取用户的分享列表
     */
    public List<Map<String, Object>> getUserShares(Long userId) {
        List<SharedBank> sharedBanks = sharedBankMapper.selectByUserId(userId);
        List<Map<String, Object>> result = new ArrayList<>();
        
        for (SharedBank sb : sharedBanks) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", sb.getId());
            map.put("bankId", sb.getBankId());
            map.put("userId", sb.getUserId());
            map.put("shareCode", sb.getShareCode());
            map.put("isPublic", sb.getIsPublic());
            map.put("status", sb.getStatus());
            map.put("createdAt", sb.getCreatedAt());
            map.put("updatedAt", sb.getUpdatedAt());
            // 将expireAt转换为时间戳（毫秒），方便前端el-date-picker处理
            map.put("expireAt", sb.getExpireAt() != null ? sb.getExpireAt().getTime() : null);
            
            // 获取题库信息
            var bank = questionBankMapper.findById(sb.getBankId());
            if (bank != null) {
                // 优先使用分享时设置的标题和描述，如果为空则使用原题库的
                map.put("name", sb.getShareTitle() != null && !sb.getShareTitle().isEmpty() 
                    ? sb.getShareTitle() : bank.getName());
                map.put("description", sb.getShareDescription() != null && !sb.getShareDescription().isEmpty() 
                    ? sb.getShareDescription() : bank.getDescription());
                map.put("difficulty", bank.getDifficulty());
                map.put("language", bank.getLanguage());
                map.put("cardCount", bank.getCardCount());
            }
            
            result.add(map);
        }
        
        return result;
    }

    /**
     * 删除分享
     */
    @Transactional
    public void deleteShare(Long shareId, Long userId) {
        // 先查询分享记录，验证用户权限
        SharedBank sharedBank = sharedBankMapper.selectById(shareId);
        if (sharedBank == null) {
            throw new RuntimeException("分享记录不存在");
        }
        
        // 验证是否是分享的创建者
        if (!sharedBank.getUserId().equals(userId)) {
            throw new RuntimeException("无权删除该分享，只有创建者可以删除");
        }
        
        // 删除浏览记录
        bankShareViewMapper.deleteBySharedBankId(shareId);
        // 删除分享
        sharedBankMapper.deleteById(shareId);
    }
    


    /**
     * 增加收藏数
     */
    public void incrementFavoriteCount(Long sharedBankId) {
        sharedBankMapper.incrementFavoriteCount(sharedBankId);
    }

    /**
     * 减少收藏数
     */
    public void decrementFavoriteCount(Long sharedBankId) {
        sharedBankMapper.decrementFavoriteCount(sharedBankId);
    }

    /**
     * 增加复制数（通过分享ID）
     */
    public void incrementCopyCount(Long sharedBankId) {
        sharedBankMapper.incrementCopyCount(sharedBankId);
    }

    /**
     * 增加复制数（通过分享码）
     */
    public void incrementCopyCount(String shareCode) {
        SharedBank sharedBank = sharedBankMapper.selectByShareCode(shareCode);
        if (sharedBank != null) {
            sharedBankMapper.incrementCopyCount(sharedBank.getId());
        } else {
            throw new RuntimeException("分享码不存在或已失效");
        }
    }

    /**
     * 通过题库ID增加复制数（更新该题库的所有分享记录）
     */
    public void incrementCopyCountByBankId(Long bankId) {
        List<SharedBank> sharedBanks = sharedBankMapper.selectByBankId(bankId);
        for (SharedBank sharedBank : sharedBanks) {
            sharedBankMapper.incrementCopyCount(sharedBank.getId());
        }
    }
}