package com.knowledge.questioncard.dto;

import lombok.Data;
import jakarta.validation.constraints.*;

/**
 * 提交答案请求DTO
 */
@Data
public class SubmitAnswerRequest {
    @NotBlank(message = "房间ID不能为空")
    private String roomId;
    
    @NotBlank(message = "答案不能为空")
    @Size(max = 1000, message = "答案不能超过1000个字符")
    private String answer;
}