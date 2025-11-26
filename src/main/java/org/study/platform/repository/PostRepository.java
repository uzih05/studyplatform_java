package org.study.platform.repository;

import org.study.platform.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    // 특정 방의 모든 게시글 조회
    List<Post> findByRoomId(Long roomId);

    // 특정 방의 특정 타입 게시글 조회 (NOTICE, GENERAL)
    List<Post> findByRoomIdAndPostType(Long roomId, Post.PostType postType);

    // 특정 작성자의 게시글 조회
    List<Post> findByAuthorId(Long authorId);

    // 특정 방의 게시글을 최신순으로 조회
    List<Post> findByRoomIdOrderByCreatedAtDesc(Long roomId);
}