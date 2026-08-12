package com.dragoncommunity.domain.post.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostsStats {

    @Id
    @Column(name = "post_id", unique = true, nullable = false)
    private Long postId;

    @Column(name = "like_count", nullable = false)
    private Long likeCount;

    @Column(name = "view_count", nullable = false)
    private Long viewCount;

    @Column(name = "comment_count", nullable = false)
    private Long commentCount;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "post_id")
    private Posts post;

    private PostsStats(Posts post,Long likeCount, Long viewCount, Long commentCount){
        this.post = post;
        this.likeCount = likeCount;
        this.viewCount = viewCount;
        this.commentCount= commentCount;
    }

    public static PostsStats createPostsStats(Posts post){
        return new PostsStats(post,0L,0L,0L);
    }
}
