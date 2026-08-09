package com.dragoncommunity.domain.post.dto;

import com.dragoncommunity.common.util.FileUtil;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
public class PostDetailDto {
    private final Long postId;
    private final String nickname;
    private final String title;
    private final String content;
    private final Long likeCount;
    private final Long commentCount;
    private final String postImageUrl;
    private final LocalDateTime updatedAt;
    @Setter
    private Long viewCount;
    public PostDetailDto(
            Long postId,
            String nickname,
            String title,
            String content,
            Long likeCount,
            Long commentCount,
            String postImageUrl,
            LocalDateTime updatedAt) {
        this.postId = postId;
        this.nickname = nickname;
        this.title = title;
        this.content = content;
        this.likeCount = likeCount;
        this.commentCount = commentCount;
        this.postImageUrl = postImageUrl != null ? FileUtil.toFullUrl(postImageUrl) : null;
        this.updatedAt = updatedAt;
    }
}
