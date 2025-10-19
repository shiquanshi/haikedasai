package com.knowledge.questioncard.mapper;

import com.knowledge.questioncard.entity.StudyRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface StudyRecordMapper {
    
    /**
     * 插入学习记录
     */
    int insert(StudyRecord record);
    
    /**
     * 更新学习记录
     */
    int update(StudyRecord record);
    
    /**
     * 根据用户和题库查询学习记录
     */
    StudyRecord selectByUserAndBank(@Param("userId") Long userId, 
                                     @Param("bankId") Long bankId);
    
    /**
     * 查询用户的所有学习记录(按最后学习时间倒序)
     */
    List<StudyRecord> selectByUserId(@Param("userId") Long userId,
                                      @Param("limit") Integer limit);
    
    /**
     * 删除学习记录
     */
    int deleteById(Long id);
}