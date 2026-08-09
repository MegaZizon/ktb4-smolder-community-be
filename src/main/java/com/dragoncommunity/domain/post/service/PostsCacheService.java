package com.dragoncommunity.domain.post.service;


import com.dragoncommunity.common.exception.ApplicationException;
import com.dragoncommunity.domain.post.dto.PostDetailDto;
import com.dragoncommunity.domain.post.repository.PostsRepository;
import com.dragoncommunity.domain.post.repository.PostsStatsRepository;
import lombok.RequiredArgsConstructor;
import org.redisson.api.*;
import org.redisson.client.codec.LongCodec;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.dragoncommunity.common.exception.enums.ApplicationErrorCode.POST_INTERNAL_ERROR;
import static com.dragoncommunity.domain.post.constant.PostConstant.*;

@Service
@RequiredArgsConstructor
public class PostsCacheService {

    private final RedissonClient redissonClient;
    private final PostsStatsRepository postsStatsRepository;
    private final PostsRepository postsRepository;


    public long increaseViewCount(Long postId) {

        String key = VIEW_COUNT_KEY_PREFIX + postId;

        long value = readViewCountFromRedis(key);

        if (value != -1) {
            return value;
        }

        /*
         * 캐시 스템피드 시 조회수 동시 접근 현상 발생할 때 해결책
         * 이 정도의 게시글 조회 캐시 스탬피드 동시성 문제가 발생하면 동시 접근이 엄청 많다는 가정이다.
         * 따라서 비관적 락 적용한다.
         * 비관적 락
         * - 조회수는 자주 변경되는 값, 정합성이 크게 중요하진 않다. But 내 사이트는 커뮤니티, 조회수 = 힘
         * - 자주 변경됨 -> 동시성 이슈가 발생할 일이 많음 -> 동시성 문제 발생 시 성능 문제가 적으려면 -> 비관적 락
         */

        String lockKey = VIEW_COUNT_KEY_PREFIX + LOCK_KEY_PREFIX + postId;
        RLock lock = redissonClient.getLock(lockKey);

        for (int attempt = 0; attempt < MAX_LOCK_ATTEMPTS; attempt++) {
            try {
                if (lock.tryLock(
                        LOCK_WAIT_TIME_MS,
                        LOCK_LEASE_TIME_MS,
                        TimeUnit.MICROSECONDS
                )) {
                    try {
                        value = readViewCountFromRedis(key);

                        if (value != -1) {
                            return value;
                        }

                        long dbViewCount = postsStatsRepository.findByPostId(postId)
                                .orElseThrow(() -> new ApplicationException(POST_INTERNAL_ERROR)).getViewCount();
                        value = dbViewCount + 1;
                        RAtomicLong counter = redissonClient.getAtomicLong(key);
                        counter.set(value);
                        counter.expire(Duration.ofHours(1));

                        return value;
                    } finally {
                        lock.unlock();
                    }
                } else {
                    value = readViewCountFromRedis(key);
                    if (value != -1) {
                        return value;
                    }
                }
            } catch (Exception e) {
                throw new ApplicationException(POST_INTERNAL_ERROR);
            }
        }
        throw new ApplicationException(POST_INTERNAL_ERROR);
    }

    public PostDetailDto getPostDetailById(Long postId) {
        String key = POST_DETAIL_KEY_PREFIX + postId;

        RBucket<PostDetailDto> bucket = redissonClient.getBucket(key);

        PostDetailDto dto = bucket.get();

        if(dto != null){
            return dto;
        }

        /**
         * 마찬가지로 비관 락 적용한다.
         * 위와 생명 주기가 같음, 같은 메서드에서 접근하기 때문
         * 따라서 TTL을 다르게 설정할 필요성이 있다.
         */
        String lockKey = POST_DETAIL_KEY_PREFIX + LOCK_KEY_PREFIX + postId;
        RLock lock = redissonClient.getLock(lockKey);

        for (int attempt = 0; attempt < MAX_LOCK_ATTEMPTS; attempt++) {
            try {
                if (lock.tryLock(
                        LOCK_WAIT_TIME_MS,
                        LOCK_LEASE_TIME_MS,
                        TimeUnit.MICROSECONDS
                )) {
                    try {
                        bucket = redissonClient.getBucket(key);

                        dto = bucket.get();
                        if (dto != null) {
                            return dto;
                        }

                        dto = postsRepository.findPostDetailById(postId)
                                .orElseThrow(()-> new ApplicationException(POST_INTERNAL_ERROR));
                        bucket.set(dto, Duration.ofHours(1));
                        return dto;

                    } finally {
                        lock.unlock();
                    }
                } else {
                    dto = bucket.get();
                    if (dto != null) {
                        return dto;
                    }
                }
            } catch (Exception e) {
                throw new ApplicationException(POST_INTERNAL_ERROR);
            }
        }
        throw new ApplicationException(POST_INTERNAL_ERROR);
    }



    private Long readViewCountFromRedis(String key) {
        RScript script = redissonClient.getScript(LongCodec.INSTANCE);
        return  script.eval(
                RScript.Mode.READ_WRITE,
                VIEW_COUNT_CHECK_LUA_SCRIPT,
                RScript.ReturnType.INTEGER,
                List.of(key)
        );
    }


}
