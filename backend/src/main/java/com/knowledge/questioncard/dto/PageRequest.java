package com.knowledge.questioncard.dto;

import lombok.Data;

@Data
public class PageRequest {
    private Integer page = 1; // 当前页码，默认第1页
    private Integer pageSize = 10; // 每页数量，默认10条
    private String sortBy = "createdAt"; // 排序字段
    private String sortOrder = "desc"; // 排序方向：asc, desc
    
    public Integer getOffset() {
        return (page - 1) * pageSize;
    }
}