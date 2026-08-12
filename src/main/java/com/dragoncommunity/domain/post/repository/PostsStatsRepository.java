package com.dragoncommunity.domain.post.repository;

import com.dragoncommunity.domain.post.model.Posts;
import com.dragoncommunity.domain.post.model.PostsStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PostsStatsRepository extends JpaRepository<PostsStats, Posts> {

    @Modifying(clearAutomatically = true)
    @Query("""
        UPDATE PostsStats p
        SET p.viewCount = p.viewCount + 1
        WHERE p.postId = :postId
    """)
    void increaseViewCount(@Param("postId") Long postId);


    Optional<PostsStats> findByPostId(Long postId);

    Optional<PostsStats> findViewCountByPostId(Long postId);

    @Modifying
    @Query("""
        UPDATE PostsStats ps
        SET ps.viewCount = :viewCount
        WHERE ps.postId = :postId
    """)
    int updateViewCount(
            @Param("postId") Long postId,
            @Param("viewCount") Long viewCount
    );
}
