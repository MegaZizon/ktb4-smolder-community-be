package com.dragoncommunity.domain.post.dto.response;

import com.dragoncommunity.common.util.FileUtil;

import java.time.LocalDateTime;

public record GetPostDetailResponseDto(
        Long postId,
        String nickname,
        String title,
        String content,
        Long viewCount,
        Long likeCount,
        Long commentCount,
        String postImageUrl,
        LocalDateTime updatedAt
) {
    public GetPostDetailResponseDto(
            Long postId,
            String nickname,
            String title,
            String content,
            Long viewCount,
            Long likeCount,
            Long commentCount,
            String postImageUrl,
            LocalDateTime updatedAt) {
        this.postId = postId;
        this.nickname = nickname;
        this.title = title;
        this.content = content;
        this.viewCount = viewCount;
        this.likeCount = likeCount;
        this.commentCount = commentCount;
        this.postImageUrl = postImageUrl != null ? FileUtil.toFullUrl(postImageUrl) : null;
        this.updatedAt = updatedAt;
    }
}
