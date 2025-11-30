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

    @Transactional
    public PostReadStatus markAsRead(Long postId, Long userId) {
        if (postReadStatusRepository.existsByPostIdAndUserId(postId, userId)) {
            throw new IllegalArgumentException("이미 읽은 게시글입니다.");
        }
        PostReadStatus readStatus = new PostReadStatus(postId, userId);
        return postReadStatusRepository.save(readStatus);
    }

    public List<PostReadStatus> getReadStatusByPost(Long postId) {
        return postReadStatusRepository.findByPostId(postId);
    }

    public List<PostReadStatus> getReadStatusByUser(Long userId) {
        return postReadStatusRepository.findByUserId(userId);
    }

    public boolean hasRead(Long postId, Long userId) {
        return postReadStatusRepository.existsByPostIdAndUserId(postId, userId);
    }
}
