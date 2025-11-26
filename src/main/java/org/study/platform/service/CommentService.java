package org.study.platform.service;

import org.study.platform.entity.Comment;
import org.study.platform.repository.CommentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Service
public class CommentService {

    private final CommentRepository commentRepository;

    @Autowired
    public CommentService(CommentRepository commentRepository) {
        this.commentRepository = commentRepository;
    }

    // 댓글 작성
    @Transactional
    public Comment createComment(Long postId, Long authorId, String content) {
        Comment comment = new Comment(postId, authorId, content);
        return commentRepository.save(comment);
    }

    // 댓글 삭제
    @Transactional
    public void deleteComment(Long commentId, Long userId) {
        Optional<Comment> commentOpt = commentRepository.findById(commentId);

        if (commentOpt.isEmpty()) {
            throw new IllegalArgumentException("존재하지 않는 댓글입니다.");
        }

        Comment comment = commentOpt.get();
        if (!comment.getAuthorId().equals(userId)) {
            throw new IllegalArgumentException("작성자만 삭제할 수 있습니다.");
        }

        commentRepository.delete(comment);
    }

    // 특정 게시글의 모든 댓글 조회 (생성일순)
    public List<Comment> findByPostId(Long postId) {
        return commentRepository.findByPostIdOrderByCreatedAtAsc(postId);
    }

    // 댓글 조회 (ID로)
    public Optional<Comment> findById(Long commentId) {
        return commentRepository.findById(commentId);
    }
}