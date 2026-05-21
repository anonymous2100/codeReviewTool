package com.ctgu.reviewbot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ctgu.reviewbot.model.ReviewRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * ReviewRecord MyBatis-Plus Mapper，含自定义统计查询
 */
@Mapper
public interface ReviewRecordMapper extends BaseMapper<ReviewRecord>
{
    List<ReviewRecord> findByProjectIdAndDateRange(@Param("projectId") String projectId,
        @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    List<ReviewRecord> findByProjectIdSince(@Param("projectId") String projectId, @Param("since") LocalDateTime since);

    long countBlockingReviews(@Param("projectId") String projectId, @Param("from") LocalDateTime from,
        @Param("to") LocalDateTime to);
}
