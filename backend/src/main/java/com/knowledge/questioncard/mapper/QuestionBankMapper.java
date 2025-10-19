package com.knowledge.questioncard.mapper;

import com.knowledge.questioncard.entity.QuestionBank;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface QuestionBankMapper {
    
    int insert(QuestionBank questionBank);
    
    int deleteById(Long id);
    
    int updateById(QuestionBank questionBank);
    
    QuestionBank selectById(Long id);
    
    List<QuestionBank> selectByTopicAndType(@Param("topic") String topic, @Param("type") String type);
    
    List<QuestionBank> selectByTopic(@Param("topic") String topic);
    
    List<QuestionBank> selectAll();
    
    // 高级搜索（分页）
    List<QuestionBank> searchBanks(
        @Param("topic") String topic,
        @Param("type") String type,
        @Param("difficulty") String difficulty,
        @Param("tags") String tags,
        @Param("userId") Long userId,
        @Param("tenantId") String tenantId,
        @Param("minCards") Integer minCards,
        @Param("maxCards") Integer maxCards,
        @Param("offset") Integer offset,
        @Param("limit") Integer limit,
        @Param("sortBy") String sortBy,
        @Param("sortOrder") String sortOrder
    );
    
    // 统计搜索结果总数
    Long countBanks(
        @Param("topic") String topic,
        @Param("type") String type,
        @Param("difficulty") String difficulty,
        @Param("tags") String tags,
        @Param("userId") Long userId,
        @Param("tenantId") String tenantId,
        @Param("minCards") Integer minCards,
        @Param("maxCards") Integer maxCards
    );
    
    // 增加浏览次数
    int incrementViewCount(@Param("id") Long id);
    
    // 更新统计信息
    int updateStatistics(@Param("id") Long id, @Param("cardCount") Integer cardCount);
    
    // 根据ID查询题库
    QuestionBank findById(@Param("id") Long id);
    
    // 更新题库信息
    int update(QuestionBank questionBank);
    
    // 删除题库
    int delete(@Param("id") Long id);
}