package com.knowledge.questioncard.service;

import com.knowledge.questioncard.entity.BankFavorite;
import com.knowledge.questioncard.mapper.BankFavoriteMapper;
import com.knowledge.questioncard.mapper.QuestionBankMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BankFavoriteService {
    private final BankFavoriteMapper bankFavoriteMapper;
    private final QuestionBankMapper questionBankMapper;
    
    /**
     * 收藏题库
     */
    @Transactional
    public void addFavorite(Long userId, Long bankId) {
        // 检查是否已收藏
        BankFavorite existing = bankFavoriteMapper.selectByUserAndBank(userId, bankId);
        if (existing != null) {
            throw new RuntimeException("已经收藏过该题库");
        }
        
        BankFavorite favorite = new BankFavorite();
        favorite.setUserId(userId);
        favorite.setBankId(bankId);
        favorite.setCreatedAt(new java.util.Date());
        bankFavoriteMapper.insert(favorite);
        
        // 更新题库收藏数
        Long favoriteCount = bankFavoriteMapper.countByBank(bankId);
        questionBankMapper.updateStatistics(bankId, null);
    }
    
    /**
     * 取消收藏
     */
    @Transactional
    public void removeFavorite(Long userId, Long bankId) {
        int deleted = bankFavoriteMapper.deleteByUserAndBank(userId, bankId);
        if (deleted == 0) {
            throw new RuntimeException("未收藏该题库");
        }
        
        // 更新题库收藏数
        Long favoriteCount = bankFavoriteMapper.countByBank(bankId);
        questionBankMapper.updateStatistics(bankId, null);
    }
    
    /**
     * 获取用户收藏的题库ID列表
     */
    public List<Long> getUserFavoriteBankIds(Long userId) {
        return bankFavoriteMapper.selectBankIdsByUser(userId);
    }
    
    /**
     * 检查是否已收藏
     */
    public boolean isFavorited(Long userId, Long bankId) {
        return bankFavoriteMapper.selectByUserAndBank(userId, bankId) != null;
    }
    
    /**
     * 获取题库的收藏数
     */
    public Long getBankFavoriteCount(Long bankId) {
        return bankFavoriteMapper.countByBank(bankId);
    }
}