package com.knowledge.questioncard.entity;

import lombok.Data;
import java.util.Date;

@Data
public class BankShareView {
    private Long id;
    private Long sharedBankId;
    private Long viewerId; // 访问者用户ID(未登录为NULL)
    private String viewerIp;
    private String userAgent;
    private String deviceType; // 设备类型:PC/Mobile
    private Integer viewDuration; // 浏览时长(秒)
    private Date createdAt;
}