package com.dragoncommunity.domain.post.controller;

import com.dragoncommunity.common.dto.ApiResponse;
import com.dragoncommunity.common.util.SecurityContextUtil;
import com.dragoncommunity.domain.post.dto.request.CreatePostRequestDto;
import com.dragoncommunity.domain.post.dto.request.GetPostsRequestDto;
import com.dragoncommunity.domain.post.dto.request.ModifyPostRequestDto;
import com.dragoncommunity.domain.post.dto.response.GetPostDetailResponseDto;
import com.dragoncommunity.domain.post.dto.response.GetPostResponseDto;
import com.dragoncommunity.domain.post.service.PostsService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static com.dragoncommunity.domain.post.constant.SuccessMessage.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/post")
public class PostsController {

    private final PostsService postsService;

    /**
     * 게시글 등록 API
     * request Attribute에 있는 user Id를 추출하여 게시글을 등록한다.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<Void>>createPost(@Valid @RequestBody CreatePostRequestDto createPostRequestDto, HttpServletRequest request){
        postsService.createPost(createPostRequestDto, SecurityContextUtil.extractUserId((request)));

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.of(POST_UPLOAD_SUCCESS));
    }

    /**
     * 게시글 조회  API
     * query parameter에 있는 lastSeenId부터 게시글을 10개 조회한다.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<GetPostResponseDto>> getPosts(@ModelAttribute GetPostsRequestDto getPostsRequestDto){
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(
                        ApiResponse.of(POSTS_LOAD_SUCCESS,postsService.getPosts(getPostsRequestDto)));
    }

    /**
     * 게시글 수정 API
     */
    @PatchMapping("/{post_id}")
    public ResponseEntity<ApiResponse<Void>> modifyPost(@PathVariable("post_id") Long postId,
                                        @RequestBody ModifyPostRequestDto modifyPostRequestDto,
                                        HttpServletRequest request){
        postsService.modifyPost(modifyPostRequestDto,SecurityContextUtil.extractUserId(request),postId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.of(POST_MODIFY_SUCCESS));
    }

    /**
     * 게시글 상세 조회  API
     */
    @GetMapping("/{post_id}")
    public ResponseEntity<ApiResponse<GetPostDetailResponseDto>> getPostDetail(@PathVariable("post_id") Long postId){
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.of(POST_DETAIL_LOAD_SUCCESS, postsService.getPostDetail(postId)));
    }

}
