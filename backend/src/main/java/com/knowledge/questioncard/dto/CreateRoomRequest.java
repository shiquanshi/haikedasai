package com.knowledge.questioncard.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import jakarta.validation.constraints.*;

/**
 * 创建房间请求DTO
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreateRoomRequest {
    @NotBlank(message = "房间名称不能为空")
    @Size(max = 50, message = "房间名称不能超过50个字符")
    private String roomName;
    
    @NotNull(message = "最大玩家数不能为空")
    @Min(value = 2, message = "最少需要2名玩家")
    @Max(value = 10, message = "最多支持10名玩家")
    private Integer maxPlayers;
    
    @NotNull(message = "总轮次数不能为空")
    @Min(value = 1, message = "至少需要1轮")
    @Max(value = 10, message = "最多支持10轮")
    private Integer totalRounds;
    
    @NotBlank(message = "答题主题不能为空")
    @Size(max = 100, message = "主题不能超过100个字符")
    private String topic;
    
    @Size(max = 100, message = "学习场景不能超过100个字符")
    private String scenario;
    
    @NotBlank(message = "难度等级不能为空")
    @Pattern(regexp = "^(easy|medium|hard)$", message = "难度只能是easy、medium或hard")
    private String difficulty;
}