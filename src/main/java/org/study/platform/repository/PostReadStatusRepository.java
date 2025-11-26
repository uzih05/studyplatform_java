package org.study.platform.repository;

import org.study.platform.entity.PostReadStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface PostReadStatusRepository extends JpaRepository<PostReadStatus, Long> {

    // 특정 게시글의 읽음 상태 목록 조회
    List<PostReadStatus> findByPostId(Long postId);

    // 특정 사용자가 읽은 게시글 목록 조회
    List<PostReadStatus> findByUserId(Long userId);

    // 특정 사용자가 특정 게시글을 읽었는지 확인
    Optional<PostReadStatus> findByPostIdAndUserId(Long postId, Long userId);

    // 특정 사용자가 특정 게시글을 읽었는지 여부
    boolean existsByPostIdAndUserId(Long postId, Long userId);
}