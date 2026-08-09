package com.dragoncommunity.domain.post.service;

import com.dragoncommunity.common.exception.ApplicationException;
import com.dragoncommunity.common.util.FileUtil;
import com.dragoncommunity.domain.image.model.Images;
import com.dragoncommunity.domain.image.repository.ImagesRepository;
import com.dragoncommunity.domain.post.dto.PostDetailDto;
import com.dragoncommunity.domain.post.dto.PostInfoDto;
import com.dragoncommunity.domain.post.dto.request.CreatePostRequestDto;
import com.dragoncommunity.domain.post.dto.request.GetPostsRequestDto;
import com.dragoncommunity.domain.post.dto.request.ModifyPostRequestDto;
import com.dragoncommunity.domain.post.dto.response.GetPostResponseDto;
import com.dragoncommunity.domain.post.model.Posts;
import com.dragoncommunity.domain.post.model.PostsImages;
import com.dragoncommunity.domain.post.model.PostsStats;
import com.dragoncommunity.domain.post.repository.PostsImagesRepository;
import com.dragoncommunity.domain.post.repository.PostsRepository;
import com.dragoncommunity.domain.post.repository.PostsStatsRepository;
import com.dragoncommunity.domain.user.model.Users;
import com.dragoncommunity.domain.user.repository.UsersRepository;
import com.dragoncommunity.infrastructure.storage.FileManager;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Optional;

import static com.dragoncommunity.common.exception.enums.ApplicationErrorCode.*;
import static com.dragoncommunity.domain.post.constant.PostConstant.*;

@Service
@RequiredArgsConstructor
@Validated
public class PostsService {

    private final PostsRepository postsRepository;
    private final UsersRepository usersRepository;
    private final PostsImagesRepository postsImagesRepository;
    private final PostsStatsRepository postsStatsRepository;
    private final ImagesRepository imagesRepository;
    private final FileManager fileManager;
    private final PostsCacheService postsCacheService;

    /**
     * 게시글 등록 서비스
     * 1. 유저 ID로 유저 엔티티 가져온다.
     * 2. 게시글 등록 시 첨부파일 Url이 있을 경우, 파일을 검증한다.
     * 3. 게시글을 저장한다
     * 4. 게시글 이미지가 있다면 저장한다
     * 5. 게시글 통계 정보를 저장한다.
     */
    @Transactional
    public void createPost(@Valid CreatePostRequestDto createPostRequestDto, Long userId){
        Images image = null;

        Users user = usersRepository.findByUserId(userId)
                .orElseThrow(()-> new ApplicationException(USER_NOT_EXIST));

        if (createPostRequestDto.postImageUrl() != null  && !createPostRequestDto.postImageUrl().isEmpty()) {
            String postImagePath = FileUtil.extractPathFromUrl(createPostRequestDto.postImageUrl());

            fileManager.validatePostImageExists(postImagePath);

            image = imagesRepository.findByImageUrl(postImagePath)
                    .orElseThrow(() -> new ApplicationException(POST_IMAGE_NOT_FOUND));

            if (postsImagesRepository.existsByImage(image)) {
                throw new ApplicationException(FILE_UPLOAD_FAILED);
            }
        }

        Posts post =Posts.createPost(
                createPostRequestDto.title(),
                createPostRequestDto.content(),
                user);

        postsRepository.save(post);

        if(image !=null){
            postsImagesRepository.save(PostsImages.createPostsImages(post,image));
        }

        postsStatsRepository.save(PostsStats.createPostsStats(post));
    }

    /**
     * 게시글 조회 서비스
     * 마지막 본 게시글의 ID 이후 10개의 게시글을 조회한다.
     * 1. 조회할 게시글의 버퍼 개수 설정
     * 2. 조회 쿼리 실행
     * 2-1. 쿼리파라미터가 없다면 가장 최근 게시글 10개 조회
     * 2-2. 쿼리파라미터가 있다면 해당 ID 이후 10개 조회
     */
    public GetPostResponseDto getPosts(GetPostsRequestDto getPostsRequestDto){
        Pageable pageable = PageRequest.of(0, DEFAULT_POST_GET_SIZE);

        Slice<PostInfoDto> sliceResult;

        if (getPostsRequestDto.lastSeenId() == null) {
            sliceResult = postsRepository.findFirstPage(pageable);
        } else {
            sliceResult = postsRepository.findNextPage(getPostsRequestDto.lastSeenId(), pageable);
        }

        List<PostInfoDto> contents = sliceResult.getContent();
        boolean hasNext = sliceResult.hasNext();

        Long nextCursorId = contents.isEmpty() ? null : contents.get(contents.size() - 1).postId();

        return GetPostResponseDto.of(contents, nextCursorId, hasNext);
    }

    /**
     * 게시글 수정 서비스
     * 1. 게시글 유저 정보와 유저 정보가 일치하는지 확인
     * 2. 게시글 엔티티 변경
     * 3. 게시글 첨부파일 CASE에 따라 분기하여 추가 변경 삭제 현상유지
     */
    public void modifyPost(ModifyPostRequestDto modifyPostRequestDto, Long userId, Long postId) {

        Users user = usersRepository.findByUserId(userId)
                .orElseThrow(() -> new ApplicationException(USER_NOT_EXIST));

        Posts post = postsRepository.findByPostId(postId)
                .orElseThrow(() -> new ApplicationException(POST_NOT_EXIST));

        if (!post.getUser().getUserId().equals(user.getUserId())) {
            throw new ApplicationException(UNAUTHORIZED_RESOURCE);
        }

        post.modify(modifyPostRequestDto.title(), modifyPostRequestDto.content());

        Optional<PostsImages> beforePostImage = postsImagesRepository.findByPost(post);

        /**
         *  case 1 : 게시글 수정 이전 이후 둘 다 첨부파일이 존재하지 않는 경우 -> 종료
         *  case 2 : 전에 첨부파일이 없었다가 수정 후 첨부파일이 생긴 경우 -> 추가
         *  case 3 : 전에 첨부파일이 있었다가 수정 후 첨부파일이 없어진 경우 -> 삭제
         *  case 4 : 전에 첨부파일이 있었다가 수정 후 첨부파일이 변경된 경우 -> 변경
         *  case 5 : 게시글 수정 이전 이후 첨부파일이 같은 경우 -> 종료
         */

        if (beforePostImage.isEmpty() && modifyPostRequestDto.postImageUrl() != null) {
            // 전에 첨부파일이 없었다가 수정 후 첨부파일이 생긴 경우
            Images newImage;

            String newPostImagePath = FileUtil.extractPathFromUrl(modifyPostRequestDto.postImageUrl());

            fileManager.validatePostImageExists(newPostImagePath);

            newImage = imagesRepository.findByImageUrl(newPostImagePath)
                    .orElseThrow(() -> new ApplicationException(POST_IMAGE_NOT_FOUND));

            if (postsImagesRepository.existsByImage(newImage)) {
                throw new ApplicationException(FILE_UPLOAD_FAILED);
            }

            postsImagesRepository.save(PostsImages.createPostsImages(post, newImage));

        } else if (beforePostImage.isPresent() && modifyPostRequestDto.postImageUrl() == null) {
            // 전에 첨부파일이 있었다가 수정 후 첨부파일이 없어진 경우
            Images beforeImage;

            String beforePostImagePath = FileUtil.extractPathFromUrl(beforePostImage.get().getImage().getImageUrl());

            beforeImage = imagesRepository.findByImageUrl(beforePostImagePath)
                    .orElseThrow(() -> new ApplicationException(POST_IMAGE_NOT_FOUND));

            postsImagesRepository.delete(beforePostImage.get());

            imagesRepository.delete(beforeImage);

            fileManager.postImageDelete(beforePostImagePath);
        } else if (beforePostImage.isPresent() && !beforePostImage.get().getImage().getImageUrl().equals(modifyPostRequestDto.postImageUrl())) {
            // 전에 첨부파일이 있었다가 수정 후 첨부파일이 변경된 경우

            System.out.println("1");

            Images newImage, beforeImage;
            beforeImage = beforePostImage.get().getImage();

            String beforePostImagePath = FileUtil.extractPathFromUrl(beforePostImage.get().getImage().getImageUrl());
            String newPostImagePath = FileUtil.extractPathFromUrl(modifyPostRequestDto.postImageUrl());

            newImage = imagesRepository.findByImageUrl(newPostImagePath)
                    .orElseThrow(() -> new ApplicationException(POST_IMAGE_NOT_FOUND));

            fileManager.validatePostImageExists(newPostImagePath);

            if (postsImagesRepository.existsByImage(newImage)) {
                throw new ApplicationException(FILE_UPLOAD_FAILED);
            }

            imagesRepository.delete(beforeImage);

            beforePostImage.get().modify(newImage);

            fileManager.postImageDelete(beforePostImagePath);
        }

    }

    /**
     * 게시글 상세 조회 서비스
     */
    @Transactional
    public PostDetailDto getPostDetail(Long postId) {
        Long viewCount = postsCacheService.increaseViewCount(postId);

        PostDetailDto dto = postsCacheService.getPostDetailById(postId);

        dto.setViewCount(viewCount);

        return dto;
    }
}
