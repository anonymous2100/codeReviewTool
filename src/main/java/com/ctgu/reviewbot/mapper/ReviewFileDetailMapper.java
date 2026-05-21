package com.ctgu.reviewbot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ctgu.reviewbot.model.ReviewFileDetail;
import org.apache.ibatis.annotations.Mapper;

/**
 * ReviewFileDetail MyBatis-Plus Mapper，提供文件审查详情的 CRUD 操作
 */
@Mapper
public interface ReviewFileDetailMapper extends BaseMapper<ReviewFileDetail>
{
}
