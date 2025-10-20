package com.knowledge.questioncard.mapper;

import com.knowledge.questioncard.entity.BankFavorite;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface BankFavoriteMapper {
    
    int insert(BankFavorite favorite);
    
    int deleteByUserAndBank(@Param("userId") Long userId, @Param("bankId") Long bankId);
    
    BankFavorite selectByUserAndBank(@Param("userId") Long userId, @Param("bankId") Long bankId);
    
    List<Long> selectBankIdsByUser(@Param("userId") Long userId);
    
    Long countByBank(@Param("bankId") Long bankId);
}