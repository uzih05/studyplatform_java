package org.study.platform.service;

import org.study.platform.entity.PostReadStatus;
import org.study.platform.repository.PostReadStatusRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class PostReadStatusService {

    private final PostReadStatusRepository postReadStatusRepository;

    @Autowired
    public PostReadStatusService(PostReadStatusRepository postReadStatusRepository) {
        this.postReadStatusRepository = postReadStatusRepository;
    }

    // 게시글 읽음 처리
    @Transactional
    public PostReadStatus markAsRead(Long postId, Long userId) {
        // 이미 읽었는지 확인
        if (postReadStatusRepository.existsByPostIdAndUserId(postId, userId)) {
            throw new IllegalArgumentException("이미 읽은 게시글입니다.");
        }

        PostReadStatus readStatus = new PostReadStatus(postId, userId);
        return postReadStatusRepository.save(readStatus);
    }

    // 특정 게시글을 읽은 사용자 목록 조회
    public List<PostReadStatus> getReadStatusByPost(Long postId) {
        return postReadStatusRepository.findByPostId(postId);
    }

    // 특정 사용자가 읽은 게시글 목록 조회
    public List<PostReadStatus> getReadStatusByUser(Long userId) {
        return postReadStatusRepository.findByUserId(userId);
    }

    // 특정 사용자가 특정 게시글을 읽었는지 확인
    public boolean hasRead(Long postId, Long userId) {
        return postReadStatusRepository.existsByPostIdAndUserId(postId, userId);
    }
}