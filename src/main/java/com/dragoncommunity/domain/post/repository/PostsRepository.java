package com.dragoncommunity.domain.post.repository;

import com.dragoncommunity.domain.post.dto.PostInfoDto;
import com.dragoncommunity.domain.post.dto.response.GetPostDetailResponseDto;
import com.dragoncommunity.domain.post.model.Posts;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PostsRepository extends JpaRepository<Posts, Long> {

    @Query("SELECT new com.dragoncommunity.domain.post.dto.PostInfoDto(" +
            "       p.postId, u.nickname, i.imageUrl, p.title, s.viewCount, s.likeCount, s.commentCount, p.updatedAt) " +
            "FROM Posts p " +
            "JOIN p.user u " +
            "JOIN PostsStats s ON p.postId = s.postId " +
            "LEFT JOIN UsersImages ui ON u.userId = ui.user.userId " +
            "LEFT JOIN ui.image i " +
            "ORDER BY p.postId DESC")
    Slice<PostInfoDto> findFirstPage(Pageable pageable);

    @Query("SELECT new com.dragoncommunity.domain.post.dto.PostInfoDto(" +
            "       p.postId, u.nickname, i.imageUrl, p.title, s.viewCount, s.likeCount, s.commentCount, p.updatedAt) " +
            "FROM Posts p " +
            "JOIN p.user u " +
            "JOIN PostsStats s ON p.postId = s.postId " +
            "LEFT JOIN UsersImages ui ON u.userId = ui.user.userId " +
            "LEFT JOIN ui.image i " +
            "WHERE p.postId < :lastSeenId " +
            "ORDER BY p.postId DESC")
    Slice<PostInfoDto> findNextPage(@Param("lastSeenId") Long lastSeenId, Pageable pageable);

    Optional<Posts> findByPostId(Long postId);

    @Query("SELECT new com.dragoncommunity.domain.post.dto.response.GetPostDetailResponseDto(" +
            "       p.postId, u.nickname, p.title, p.content, " +
            "       s.viewCount, s.likeCount, s.commentCount, " +
            "       img.imageUrl, p.updatedAt) " +
            "FROM Posts p " +
            "JOIN p.user u " +
            "JOIN PostsStats s ON p.postId = s.postId " +
            "LEFT JOIN PostsImages pi ON p.postId = pi.post.postId " + // 이미지가 없을 수 있으므로 LEFT JOIN
            "LEFT JOIN pi.image img " +                             // 이미지 URL을 가져오기 위한 LEFT JOIN
            "WHERE p.postId = :postId AND p.deletedAt IS NULL")      // SoftDelete 조건 반영
    Optional<GetPostDetailResponseDto> findPostDetailById(@Param("postId") Long postId);
}
