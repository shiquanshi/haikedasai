package com.knowledge.questioncard.mapper;

import com.knowledge.questioncard.entity.BankShareView;
import org.apache.ibatis.annotations.*;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Mapper
public interface BankShareViewMapper {
    
    @Insert("INSERT INTO bank_share_views (shared_bank_id, viewer_id, viewer_ip, user_agent, " +
            "device_type, view_duration, created_at) VALUES (#{sharedBankId}, #{viewerId}, " +
            "#{viewerIp}, #{userAgent}, #{deviceType}, #{viewDuration}, #{createdAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(BankShareView view);
    
    // 检查是否是新访客(UV统计:同一IP或同一用户在24小时内只算1次UV)
    @Select("SELECT COUNT(*) FROM bank_share_views WHERE shared_bank_id=#{sharedBankId} " +
            "AND created_at > #{timeLimit} " +
            "AND (" +
            "  (viewer_id IS NOT NULL AND viewer_id=#{viewerId}) " +
            "  OR (viewer_id IS NULL AND viewer_ip=#{viewerIp})" +
            ")")
    int countRecentViews(@Param("sharedBankId") Long sharedBankId,
                        @Param("viewerId") Long viewerId,
                        @Param("viewerIp") String viewerIp,
                        @Param("timeLimit") Date timeLimit);
    
    // 获取浏览统计数据
    @Select("SELECT " +
            "COUNT(*) as total_views, " +
            "COUNT(DISTINCT CASE WHEN viewer_id IS NOT NULL THEN viewer_id ELSE viewer_ip END) as unique_visitors, " +
            "AVG(view_duration) as avg_duration " +
            "FROM bank_share_views WHERE shared_bank_id=#{sharedBankId}")
    Map<String, Object> getViewStatistics(@Param("sharedBankId") Long sharedBankId);
    
    // 按日期统计浏览量
    @Select("SELECT DATE(created_at) as view_date, COUNT(*) as view_count, " +
            "COUNT(DISTINCT CASE WHEN viewer_id IS NOT NULL THEN viewer_id ELSE viewer_ip END) as unique_count " +
            "FROM bank_share_views WHERE shared_bank_id=#{sharedBankId} " +
            "AND created_at >= #{startDate} AND created_at <= #{endDate} " +
            "GROUP BY DATE(created_at) ORDER BY view_date DESC")
    List<Map<String, Object>> getViewsByDate(@Param("sharedBankId") Long sharedBankId,
                                              @Param("startDate") Date startDate,
                                              @Param("endDate") Date endDate);
    
    @Delete("DELETE FROM bank_share_views WHERE shared_bank_id=#{sharedBankId}")
    int deleteBySharedBankId(@Param("sharedBankId") Long sharedBankId);
}