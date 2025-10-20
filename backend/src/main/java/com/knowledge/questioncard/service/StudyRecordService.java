package com.knowledge.questioncard.service;

import com.knowledge.questioncard.dto.StudyRecordDTO;
import com.knowledge.questioncard.entity.QuestionBank;
import com.knowledge.questioncard.entity.StudyRecord;
import com.knowledge.questioncard.mapper.QuestionBankMapper;
import com.knowledge.questioncard.mapper.StudyRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StudyRecordService {
    private final StudyRecordMapper studyRecordMapper;
    private final QuestionBankMapper questionBankMapper;

    /**
     * 创建或更新学习记录
     */
    @Transactional
    public StudyRecordDTO createOrUpdateRecord(Long userId, Long bankId, Integer totalCards) {
        StudyRecord existing = studyRecordMapper.selectByUserAndBank(userId, bankId);
        
        if (existing != null) {
            // 更新现有记录
            existing.setLastStudyAt(new Date());
            existing.setUpdatedAt(new Date());
            studyRecordMapper.update(existing);
            return convertToDTO(existing);
        } else {
            // 创建新记录
            StudyRecord record = new StudyRecord();
            record.setUserId(userId);
            record.setBankId(bankId);
            record.setTotalCards(totalCards);
            record.setCompletedCards(0);
            record.setCorrectCount(0);
            record.setWrongCount(0);
            record.setMasteryLevel(BigDecimal.ZERO);
            record.setStudyDuration(0);
            record.setLastStudyAt(new Date());
            record.setCreatedAt(new Date());
            record.setUpdatedAt(new Date());
            
            studyRecordMapper.insert(record);
            log.info("创建学习记录: userId={}, bankId={}", userId, bankId);
            return convertToDTO(record);
        }
    }

    /**
     * 更新学习进度
     */
    @Transactional
    public void updateProgress(Long userId, Long bankId, Integer completedCards, 
                               Integer correctCount, Integer wrongCount, Integer studyDuration) {
        StudyRecord record = studyRecordMapper.selectByUserAndBank(userId, bankId);
        if (record == null) {
            throw new RuntimeException("学习记录不存在");
        }
        
        record.setCompletedCards(completedCards);
        record.setCorrectCount(correctCount);
        record.setWrongCount(wrongCount);
        record.setStudyDuration(record.getStudyDuration() + studyDuration);
        
        // 计算掌握度 (正确率 * 完成进度)
        int totalAnswers = correctCount + wrongCount;
        double accuracy = totalAnswers > 0 ? (double) correctCount / totalAnswers : 0;
        double progress = record.getTotalCards() > 0 ? (double) completedCards / record.getTotalCards() : 0;
        double mastery = accuracy * progress * 100;
        record.setMasteryLevel(BigDecimal.valueOf(mastery));
        
        record.setLastStudyAt(new Date());
            record.setUpdatedAt(new Date());
        
        studyRecordMapper.update(record);
        log.info("更新学习进度: userId={}, bankId={}, 完成度={}%, 掌握度={}%", 
                userId, bankId, progress * 100, mastery);
    }

    /**
     * 获取用户的学习记录列表
     */
    public List<StudyRecordDTO> getUserRecords(Long userId, Integer limit) {
        List<StudyRecord> records = studyRecordMapper.selectByUserId(userId, limit);
        return records.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 获取指定题库的学习记录
     */
    public StudyRecordDTO getRecordByBank(Long userId, Long bankId) {
        StudyRecord record = studyRecordMapper.selectByUserAndBank(userId, bankId);
        return record != null ? convertToDTO(record) : null;
    }

    /**
     * 删除学习记录
     */
    @Transactional
    public void deleteRecord(Long id, Long userId) {
        StudyRecord record = studyRecordMapper.selectByUserAndBank(userId, null);
        if (record == null || !record.getId().equals(id)) {
            throw new RuntimeException("学习记录不存在或无权限删除");
        }
        studyRecordMapper.deleteById(id);
        log.info("删除学习记录: id={}, userId={}", id, userId);
    }

    /**
     * 转换为DTO
     */
    private StudyRecordDTO convertToDTO(StudyRecord record) {
        StudyRecordDTO dto = new StudyRecordDTO();
        dto.setId(record.getId());
        dto.setUserId(record.getUserId());
        dto.setBankId(record.getBankId());
        dto.setTotalCards(record.getTotalCards());
        dto.setCompletedCards(record.getCompletedCards());
        dto.setCorrectCount(record.getCorrectCount());
        dto.setWrongCount(record.getWrongCount());
        dto.setMasteryLevel(record.getMasteryLevel());
        dto.setStudyDuration(record.getStudyDuration());
        dto.setLastStudyAt(record.getLastStudyAt());
        dto.setCreatedAt(record.getCreatedAt());
        
        // 加载题库信息
        QuestionBank bank = questionBankMapper.selectById(record.getBankId());
        if (bank != null) {
            dto.setBankName(bank.getName());
            dto.setBankTopic(bank.getTopic());
        }
        
        return dto;
    }
}