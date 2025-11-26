package org.study.platform.repository;

import org.study.platform.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    // 특정 게시글의 모든 댓글 조회
    List<Comment> findByPostId(Long postId);

    // 특정 게시글의 댓글을 생성일순으로 조회
    List<Comment> findByPostIdOrderByCreatedAtAsc(Long postId);

    // 특정 작성자의 댓글 조회
    List<Comment> findByAuthorId(Long authorId);
}