package com.knowledge.questioncard.mapper;

import com.knowledge.questioncard.entity.SharedBank;
import org.apache.ibatis.annotations.*;
import java.util.List;
import java.util.Map;

@Mapper
public interface SharedBankMapper {
    
    @Insert("INSERT INTO shared_banks (bank_id, user_id, share_code, is_public, share_title, " +
            "share_description, view_count, unique_view_count, favorite_count, copy_count, status, " +
            "created_at, updated_at, expire_at) VALUES (#{bankId}, #{userId}, #{shareCode}, #{isPublic}, " +
            "#{shareTitle}, #{shareDescription}, #{viewCount}, #{uniqueViewCount}, #{favoriteCount}, " +
            "#{copyCount}, #{status}, #{createdAt}, #{updatedAt}, #{expireAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(SharedBank sharedBank);
    
    @Update("UPDATE shared_banks SET is_public=#{isPublic}, share_title=#{shareTitle}, " +
            "share_description=#{shareDescription}, status=#{status}, updated_at=#{updatedAt}, " +
            "expire_at=#{expireAt} WHERE id=#{id}")
    int update(SharedBank sharedBank);
    
    @Update("UPDATE shared_banks SET view_count=view_count+1 WHERE id=#{id}")
    int incrementViewCount(@Param("id") Long id);
    
    @Update("UPDATE shared_banks SET unique_view_count=unique_view_count+1 WHERE id=#{id}")
    int incrementUniqueViewCount(@Param("id") Long id);
    
    @Update("UPDATE shared_banks SET favorite_count=favorite_count+1 WHERE id=#{id}")
    int incrementFavoriteCount(@Param("id") Long id);
    
    @Update("UPDATE shared_banks SET favorite_count=favorite_count-1 WHERE id=#{id} AND favorite_count>0")
    int decrementFavoriteCount(@Param("id") Long id);
    
    @Update("UPDATE shared_banks SET copy_count=copy_count+1 WHERE id=#{id}")
    int incrementCopyCount(@Param("id") Long id);
    
    @Select("SELECT * FROM shared_banks WHERE id=#{id}")
    SharedBank selectById(@Param("id") Long id);
    
    @Select("SELECT * FROM shared_banks WHERE share_code=#{shareCode}")
    SharedBank selectByShareCode(@Param("shareCode") String shareCode);
    
    @Select("SELECT * FROM shared_banks WHERE bank_id=#{bankId} AND user_id=#{userId}")
    SharedBank selectByBankIdAndUserId(@Param("bankId") Long bankId, @Param("userId") Long userId);
    
    @Select("SELECT * FROM shared_banks WHERE bank_id=#{bankId}")
    List<SharedBank> selectByBankId(@Param("bankId") Long bankId);
    
    // 查询大厅所有公开分享(分页)
    @Select("<script>" +
            "SELECT sb.id, sb.bank_id as bankId, sb.user_id as userId, sb.share_code as shareCode, sb.is_public as isPublic, " +
            "sb.share_title as shareTitle, sb.share_description as shareDescription, sb.view_count as viewCount, sb.unique_view_count as uniqueViewCount, " +
            "sb.favorite_count as favoriteCount, sb.copy_count as copyCount, sb.status, " +
            "DATE_FORMAT(sb.created_at, '%Y-%m-%d %H:%i:%s') as createdAt, " +
            "qb.name as bankName, qb.description as bankDescription, qb.topic, " +
            "qb.difficulty, qb.language, qb.card_count as cardCount, qb.tags, " +
            "u.username as creatorName, u.avatar as creatorAvatar " +
            "FROM shared_banks sb " +
            "LEFT JOIN question_banks qb ON sb.bank_id = qb.id " +
            "LEFT JOIN users u ON sb.user_id = u.id " +
            "WHERE sb.is_public=1 AND sb.status=1 " +
            "<if test='topic != null and topic != \"\"'>AND qb.topic=#{topic}</if> " +
            "<if test='difficulty != null and difficulty != \"\"'>AND qb.difficulty=#{difficulty}</if> " +
            "<if test='keyword != null and keyword != \"\"'>"+
            "AND (qb.name LIKE CONCAT('%', #{keyword}, '%') OR qb.description LIKE CONCAT('%', #{keyword}, '%') "+
            "OR sb.share_title LIKE CONCAT('%', #{keyword}, '%') OR sb.share_description LIKE CONCAT('%', #{keyword}, '%') "+
            "OR u.username LIKE CONCAT('%', #{keyword}, '%'))"+
            "</if> "+
            "ORDER BY " +
            "<choose>" +
            "<when test='orderBy == \"view_count\"'>sb.view_count DESC</when>" +
            "<when test='orderBy == \"favorite_count\"'>sb.favorite_count DESC</when>" +
            "<when test='orderBy == \"copy_count\"'>sb.copy_count DESC</when>" +
            "<otherwise>sb.created_at DESC</otherwise>" +
            "</choose> " +
            "LIMIT #{offset}, #{limit}" +
            "</script>")
    List<Map<String, Object>> selectPublicShares(@Param("topic") String topic, 
                                                   @Param("difficulty") String difficulty,
                                                   @Param("keyword") String keyword,
                                                   @Param("orderBy") String orderBy,
                                                   @Param("offset") int offset, 
                                                   @Param("limit") int limit);
    
    @Select("<script>" +
            "SELECT COUNT(*) FROM shared_banks sb " +
            "LEFT JOIN question_banks qb ON sb.bank_id = qb.id " +
            "WHERE sb.is_public=1 AND sb.status=1 " +
            "<if test='topic != null and topic != \"\"'>AND qb.topic=#{topic}</if> " +
            "<if test='difficulty != null and difficulty != \"\"'>AND qb.difficulty=#{difficulty}</if> " +
            "<if test='keyword != null and keyword != \"\"'>"+
            "AND (qb.name LIKE CONCAT('%', #{keyword}, '%') OR qb.description LIKE CONCAT('%', #{keyword}, '%') "+
            "OR sb.share_title LIKE CONCAT('%', #{keyword}, '%') OR sb.share_description LIKE CONCAT('%', #{keyword}, '%') "+
            "OR u.username LIKE CONCAT('%', #{keyword}, '%'))"+
            "</if>" +
            "</script>")
    int countPublicShares(@Param("topic") String topic, 
                          @Param("difficulty") String difficulty,
                          @Param("keyword") String keyword);
    
    // 查询用户的分享列表(返回SharedBank实体)
    @Select("SELECT * FROM shared_banks WHERE user_id=#{userId} ORDER BY created_at DESC")
    @Results({
        @Result(property = "id", column = "id"),
        @Result(property = "bankId", column = "bank_id"),
        @Result(property = "userId", column = "user_id"),
        @Result(property = "shareCode", column = "share_code"),
        @Result(property = "isPublic", column = "is_public"),
        @Result(property = "shareTitle", column = "share_title"),
        @Result(property = "shareDescription", column = "share_description"),
        @Result(property = "viewCount", column = "view_count"),
        @Result(property = "uniqueViewCount", column = "unique_view_count"),
        @Result(property = "favoriteCount", column = "favorite_count"),
        @Result(property = "copyCount", column = "copy_count"),
        @Result(property = "status", column = "status"),
        @Result(property = "createdAt", column = "created_at"),
        @Result(property = "updatedAt", column = "updated_at"),
        @Result(property = "expireAt", column = "expire_at")
    })
    List<SharedBank> selectByUserId(@Param("userId") Long userId);
    
    @Delete("DELETE FROM shared_banks WHERE id=#{id}")
    int deleteById(@Param("id") Long id);
}