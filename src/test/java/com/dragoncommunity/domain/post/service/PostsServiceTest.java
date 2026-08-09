package com.dragoncommunity.domain.post.service;

import com.dragoncommunity.domain.post.model.Posts;
import com.dragoncommunity.domain.post.model.PostsStats;
import com.dragoncommunity.domain.post.repository.PostsRepository;
import com.dragoncommunity.domain.post.repository.PostsStatsRepository;
import com.dragoncommunity.domain.user.model.Users;
import com.dragoncommunity.domain.user.repository.UsersRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.LongCodec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.dragoncommunity.domain.post.constant.PostConstant.VIEW_COUNT_KEY_PREFIX;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest
@Slf4j
public class PostsServiceTest {
    @Autowired
    private PostsService postService;

    @Autowired
    private PostsRepository postRepository;

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private PostsStatsRepository postsStatsRepository;

    @Autowired
    private RedissonClient redissonClient;

    private Long postId;

    private String testKey;

    @BeforeEach
    void setUp() {
        Users users = Users.createUser("test@example.com","test","!Password123");
        Posts posts = Posts.createPost("test","test",users);

        usersRepository.save(users);
        postsStatsRepository.save(PostsStats.createPostsStats(posts));
        postRepository.save(posts);
        postId = posts.getPostId();

        testKey = VIEW_COUNT_KEY_PREFIX + postId;
        redissonClient.getBucket(testKey).delete();
    }

    @AfterEach
    void tearDown() {
        redissonClient.getBucket(testKey).delete();
    }

    @Test
    void multiThreadsLikesTest() throws InterruptedException {
        int numberOfThreads = 100;

        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);

        for (int i = 0; i < numberOfThreads; i++) {
            executorService.execute(() -> {
                try {
                    postService.getPostDetail(postId);
                }
                finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        RBucket<Long> bucket = redissonClient.getBucket(testKey, LongCodec.INSTANCE);


        Long value = bucket.get();

        assertThat(value).isEqualTo(numberOfThreads);
    }

}