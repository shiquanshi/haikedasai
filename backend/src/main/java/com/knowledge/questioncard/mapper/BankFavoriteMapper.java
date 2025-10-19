package com.knowledge.questioncard.mapper;

import com.knowledge.questioncard.entity.BankFavorite;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface BankFavoriteMapper {
    
    int insert(BankFavorite favorite);
    
    int deleteByUserAndBank(@Param("userId") Long userId, @Param("bankId") Long bankId, @Param("tenantId") Long tenantId);
    
    BankFavorite selectByUserAndBank(@Param("userId") Long userId, @Param("bankId") Long bankId, @Param("tenantId") Long tenantId);
    
    List<Long> selectBankIdsByUser(@Param("userId") Long userId, @Param("tenantId") Long tenantId);
    
    Long countByBank(@Param("bankId") Long bankId);
}