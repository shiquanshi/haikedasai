package com.knowledge.questioncard.controller;

import com.knowledge.questioncard.common.Result;
import com.knowledge.questioncard.dto.StudyRecordDTO;
import com.knowledge.questioncard.service.StudyRecordService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/study-record")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class StudyRecordController {
    private final StudyRecordService studyRecordService;

    /**
     * 创建或获取学习记录
     */
    @PostMapping("/create")
    public Result<StudyRecordDTO> createRecord(
            @RequestBody Map<String, Object> request,
            HttpServletRequest httpRequest) {
        Long bankId = Long.valueOf(request.get("bankId").toString());
        Integer totalCards = Integer.valueOf(request.get("totalCards").toString());
        
        Long userId = (Long) httpRequest.getAttribute("userId");
        
        StudyRecordDTO record = studyRecordService.createOrUpdateRecord(userId, bankId, totalCards);
        return Result.success(record);
    }

    /**
     * 更新学习进度
     */
    @PostMapping("/update-progress")
    public Result<Void> updateProgress(
            @RequestBody Map<String, Object> request,
            HttpServletRequest httpRequest) {
        Long bankId = Long.valueOf(request.get("bankId").toString());
        Integer completedCards = Integer.valueOf(request.get("completedCards").toString());
        Integer correctCount = Integer.valueOf(request.get("correctCount").toString());
        Integer wrongCount = Integer.valueOf(request.get("wrongCount").toString());
        Integer studyDuration = Integer.valueOf(request.get("studyDuration").toString());
        
        Long userId = (Long) httpRequest.getAttribute("userId");
        
        studyRecordService.updateProgress(userId, bankId, completedCards, correctCount, wrongCount, studyDuration);
        return Result.success(null);
    }

    /**
     * 获取用户的学习记录列表
     */
    @GetMapping("/list")
    public Result<List<StudyRecordDTO>> getUserRecords(
            @RequestParam(required = false, defaultValue = "20") Integer limit,
            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        List<StudyRecordDTO> records = studyRecordService.getUserRecords(userId, limit);
        return Result.success(records);
    }

    /**
     * 获取指定题库的学习记录
     */
    @GetMapping("/bank/{bankId}")
    public Result<StudyRecordDTO> getRecordByBank(
            @PathVariable Long bankId,
            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        StudyRecordDTO record = studyRecordService.getRecordByBank(userId, bankId);
        return Result.success(record);
    }

    /**
     * 删除学习记录
     */
    @DeleteMapping("/{recordId}")
    public Result<Void> deleteRecord(
            @PathVariable Long recordId,
            HttpServletRequest httpRequest) {
        try {
            Long userId = (Long) httpRequest.getAttribute("userId");
            studyRecordService.deleteRecord(recordId, userId);
            return Result.success(null);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}