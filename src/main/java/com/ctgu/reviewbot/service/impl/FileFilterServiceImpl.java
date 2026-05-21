package com.ctgu.reviewbot.service.impl;

import com.ctgu.reviewbot.config.ReviewProperties;
import com.ctgu.reviewbot.model.FileDiff;
import com.ctgu.reviewbot.monitoring.ReviewMetrics;
import com.ctgu.reviewbot.service.FileFilterService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.file.FileSystems;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;

@Slf4j
@Service
public class FileFilterServiceImpl implements FileFilterService
{
    private final ReviewProperties reviewProperties;
    private final ReviewMetrics metrics;

    public FileFilterServiceImpl(ReviewProperties reviewProperties, ReviewMetrics metrics)
    {
        this.reviewProperties = reviewProperties;
        this.metrics = metrics;
    }

    @Override
    public boolean shouldReview(FileDiff diff)
    {
        if(!diff.isJavaFile())
        {
            log.info("Skipping non-Java file: {}", diff.getPath());
            metrics.recordSkippedFile(diff.getPath(), "non-java");
            return false;
        }
        if(diff.isDeleted() && !diff.isNew())
        {
            log.info("Skipping deleted file: {}", diff.getPath());
            metrics.recordSkippedFile(diff.getPath(), "deleted");
            return false;
        }
        if(diff.lineCount() > reviewProperties.getMaxDiffLinesPerFile())
        {
            log.info("Skipping large diff: {} ({} lines)", diff.getPath(), diff.lineCount());
            metrics.recordSkippedFile(diff.getPath(), "too_large");
            return false;
        }
        for(String pattern : reviewProperties.getSkipPatterns())
        {
            if(pathMatchesGlob(diff.getPath(), pattern))
            {
                log.info("Skipping file matching pattern '{}': {}", pattern, diff.getPath());
                metrics.recordSkippedFile(diff.getPath(), "pattern:" + pattern);
                return false;
            }
        }
        if(diff.getDiff() != null && diff.getDiff().contains("@Generated"))
        {
            log.info("Skipping generated code: {}", diff.getPath());
            metrics.recordSkippedFile(diff.getPath(), "generated");
            return false;
        }
        return true;
    }

    private boolean pathMatchesGlob(String filePath, String globPattern)
    {
        try
        {
            PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + globPattern);
            return matcher.matches(Paths.get(filePath));
        }
        catch(Exception e)
        {
            log.warn("Invalid glob pattern: {}", globPattern, e);
            return false;
        }
    }
}
