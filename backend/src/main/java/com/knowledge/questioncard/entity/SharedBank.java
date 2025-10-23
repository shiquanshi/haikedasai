package com.knowledge.questioncard.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.util.Date;

@Data
public class SharedBank {
    private Long id;
    private Long bankId;
    private Long userId;
    private String shareCode;
    private Boolean isPublic; // 是否分享到大厅
    private String shareTitle;
    private String shareDescription;
    private Integer viewCount; // 总浏览次数(PV)
    private Integer uniqueViewCount; // 独立访客数(UV)
    private Integer favoriteCount; // 收藏次数
    private Integer copyCount; // 复制次数
    private Integer status; // 状态:0-已下架,1-正常
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private Date createdAt;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private Date updatedAt;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private Date expireAt;
}