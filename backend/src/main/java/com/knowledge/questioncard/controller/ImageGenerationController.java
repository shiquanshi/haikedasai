package com.knowledge.questioncard.controller;

import com.knowledge.questioncard.common.Result;
import com.knowledge.questioncard.service.VolcEngineService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 图片生成控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/image")
public class ImageGenerationController {
    
    @Autowired
    private VolcEngineService volcEngineService;
    
    /**
     * 生成图片
     * 
     * @param request 包含prompt, n, size的请求参数
     * @return 图片URL
     */
    @PostMapping("/generate")
    public Result<String> generateImage(@RequestBody Map<String, Object> request) {
        try {
            String prompt = (String) request.get("prompt");
            Integer n = request.get("n") != null ? (Integer) request.get("n") : 1;
            String size = request.get("size") != null ? (String) request.get("size") : "1024x768";
            
            if (prompt == null || prompt.trim().isEmpty()) {
                return Result.error("图片描述不能为空");
            }
            
            log.info("开始生成图片，prompt: {}, n: {}, size: {}", prompt, n, size);
            String imageUrl = volcEngineService.generateImage(prompt, n, size);
            
            if (imageUrl != null) {
                log.info("图片生成成功: {}", imageUrl);
                return Result.success(imageUrl);
            } else {
                log.error("图片生成失败");
                return Result.error("图片生成失败，请稍后重试");
            }
        } catch (Exception e) {
            log.error("生成图片异常", e);
            return Result.error("生成图片时发生错误: " + e.getMessage());
        }
    }
}