package org.study.platform.repository;

import org.study.platform.entity.PostReadStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface PostReadStatusRepository extends JpaRepository<PostReadStatus, Long> {
    List<PostReadStatus> findByPostId(Long postId);
    List<PostReadStatus> findByUserId(Long userId);
    Optional<PostReadStatus> findByPostIdAndUserId(Long postId, Long userId);
    boolean existsByPostIdAndUserId(Long postId, Long userId);
}
