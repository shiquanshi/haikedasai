package com.knowledge.questioncard.dto;

import lombok.Data;
import java.util.List;

@Data
public class PageResponse<T> {
    private List<T> data; // 数据列表
    private Long total; // 总数量
    private Integer page; // 当前页码
    private Integer pageSize; // 每页数量
    private Integer totalPages; // 总页数
    
    public PageResponse(List<T> data, Long total, Integer page, Integer pageSize) {
        this.data = data;
        this.total = total;
        this.page = page;
        this.pageSize = pageSize;
        this.totalPages = (int) Math.ceil((double) total / pageSize);
    }
}