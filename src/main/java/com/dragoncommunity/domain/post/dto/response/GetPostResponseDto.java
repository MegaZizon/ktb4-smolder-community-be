package com.dragoncommunity.domain.post.dto.response;

import com.dragoncommunity.domain.post.dto.PostInfoDto;

import java.util.List;

public record GetPostResponseDto(
        List<PostInfoDto> contents,
        Long nextCursorId,
        Boolean hasNext
) {
    public static GetPostResponseDto of(List<PostInfoDto> contents, Long nextCursorId, Boolean hasNext){
        return new GetPostResponseDto(contents,nextCursorId,hasNext);
    }
}
