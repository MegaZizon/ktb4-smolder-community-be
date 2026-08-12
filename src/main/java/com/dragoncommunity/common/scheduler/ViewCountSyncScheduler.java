package com.dragoncommunity.common.scheduler;

import com.dragoncommunity.domain.post.repository.PostsStatsRepository;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RBucket;
import org.redisson.api.RKeys;
import org.redisson.api.RedissonClient;
import org.redisson.api.options.KeysScanOptions;
import org.redisson.client.codec.LongCodec;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ViewCountSyncScheduler {

    private final RedissonClient redissonClient;
    private final PostsStatsRepository postsStatsRepository;

    @Scheduled(fixedRate = 1000)
    @Transactional
    public void syncViewCount() {

        RKeys keys = redissonClient.getKeys();

        Iterable<String> redisKeys = keys.getKeys(
                KeysScanOptions.defaults()
                        .pattern("post:view:*")
                        .limit(100)
        );

        for (String key : redisKeys) {
            Long postId = extractPostId(key);

            RBucket<Long> bucket =
                    redissonClient.getBucket(key, LongCodec.INSTANCE);

            Long redisViewCount = bucket.get();

            if (redisViewCount == null) {
                continue;
            }

            postsStatsRepository.updateViewCount(
                    postId,
                    redisViewCount
            );
        }
    }

    private Long extractPostId(String key) {
        return Long.parseLong(key.substring("view:".length()));
    }
}