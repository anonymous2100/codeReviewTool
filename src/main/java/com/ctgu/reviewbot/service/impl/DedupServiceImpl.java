package com.ctgu.reviewbot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ctgu.reviewbot.mapper.ReviewRecordMapper;
import com.ctgu.reviewbot.model.ReviewRecord;
import com.ctgu.reviewbot.service.DedupService;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class DedupServiceImpl implements DedupService
{
    private final ReviewRecordMapper reviewRecordMapper;
    private final Cache<String, Boolean> reviewCache = Caffeine.newBuilder().expireAfterWrite(24, TimeUnit.HOURS)
        .maximumSize(10_000).build();

    public DedupServiceImpl(ReviewRecordMapper reviewRecordMapper)
    {
        this.reviewRecordMapper = reviewRecordMapper;
    }

    @Override
    public boolean isDuplicate(String projectId, String commitSha)
    {
        String key = projectId + ":" + commitSha;
        if(reviewCache.asMap().putIfAbsent(key, Boolean.TRUE) != null)
        {
            log.info("Duplicate detected (cache): project={}, sha={}", projectId, commitSha);
            return true;
        }
        Long count = reviewRecordMapper
            .selectCount(new LambdaQueryWrapper<ReviewRecord>().eq(ReviewRecord::getCommitSha, commitSha));
        if(count != null && count > 0)
        {
            log.info("Duplicate detected (db): project={}, sha={}", projectId, commitSha);
            return true;
        }
        return false;
    }

    @Override
    public void markAsReviewed(String projectId, String commitSha)
    {
        reviewCache.asMap().putIfAbsent(projectId + ":" + commitSha, Boolean.TRUE);
    }
}
