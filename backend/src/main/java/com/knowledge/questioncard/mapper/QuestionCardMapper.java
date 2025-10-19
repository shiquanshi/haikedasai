package com.knowledge.questioncard.mapper;

import com.knowledge.questioncard.entity.QuestionCard;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface QuestionCardMapper {
    
    int insert(QuestionCard questionCard);
    
    int deleteById(Long id);
    
    int updateById(QuestionCard questionCard);
    
    QuestionCard selectById(Long id);
    
    List<QuestionCard> selectByBankId(@Param("bankId") Long bankId, @Param("tenantId") String tenantId);
    
    List<QuestionCard> selectAll();
    
    int batchInsert(@Param("cards") List<QuestionCard> cards);
    
    int countByBankId(@Param("bankId") Long bankId);
    
    int deleteByBankId(@Param("bankId") Long bankId);
}