package org.study.platform.service;

import org.study.platform.entity.Assignment;
import org.study.platform.entity.AssignmentSubmission;
import org.study.platform.repository.AssignmentRepository;
import org.study.platform.repository.AssignmentSubmissionRepository;
import org.study.platform.repository.PostRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final AssignmentSubmissionRepository submissionRepository;
    private final PostRepository postRepository;

    @Autowired
    public AssignmentService(AssignmentRepository assignmentRepository,
                            AssignmentSubmissionRepository submissionRepository,
                            PostRepository postRepository) {
        this.assignmentRepository = assignmentRepository;
        this.submissionRepository = submissionRepository;
        this.postRepository = postRepository;
    }

    // 과제 생성
    @Transactional
    public Assignment createAssignment(Long postId, String title, String description, LocalDateTime dueDate) {
        Assignment assignment = new Assignment(postId, title, description, dueDate);
        Assignment saved = assignmentRepository.save(assignment);
        
        // Post에 과제 여부 표시
        postRepository.findById(postId).ifPresent(post -> {
            post.setHasAssignment(true);
            postRepository.save(post);
        });
        
        return saved;
    }

    // 과제 조회
    public Optional<Assignment> findById(Long assignmentId) {
        return assignmentRepository.findById(assignmentId);
    }

    // 게시글의 과제 조회
    public Optional<Assignment> findByPostId(Long postId) {
        return assignmentRepository.findByPostId(postId);
    }

    // 과제 삭제
    @Transactional
    public void deleteAssignment(Long assignmentId) {
        assignmentRepository.findById(assignmentId).ifPresent(assignment -> {
            postRepository.findById(assignment.getPostId()).ifPresent(post -> {
                post.setHasAssignment(false);
                postRepository.save(post);
            });
            assignmentRepository.delete(assignment);
        });
    }

    // 과제 제출
    @Transactional
    public AssignmentSubmission submitAssignment(Long assignmentId, Long userId, String content,
                                                  String fileName, String filePath, Long fileSize) {
        // 이미 제출했는지 확인
        if (submissionRepository.existsByAssignmentIdAndUserId(assignmentId, userId)) {
            throw new IllegalArgumentException("이미 과제를 제출했습니다.");
        }

        AssignmentSubmission submission = new AssignmentSubmission(assignmentId, userId, content);
        submission.setFileName(fileName);
        submission.setFilePath(filePath);
        submission.setFileSize(fileSize);
        
        return submissionRepository.save(submission);
    }

    // 과제 제출 (텍스트만)
    @Transactional
    public AssignmentSubmission submitAssignment(Long assignmentId, Long userId, String content) {
        return submitAssignment(assignmentId, userId, content, null, null, null);
    }

    // 과제 제출 수정
    @Transactional
    public AssignmentSubmission updateSubmission(Long submissionId, Long userId, String content,
                                                  String fileName, String filePath, Long fileSize) {
        AssignmentSubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new IllegalArgumentException("제출물을 찾을 수 없습니다."));
        
        if (!submission.getUserId().equals(userId)) {
            throw new IllegalArgumentException("본인의 제출물만 수정할 수 있습니다.");
        }
        
        submission.setContent(content);
        if (fileName != null) {
            submission.setFileName(fileName);
            submission.setFilePath(filePath);
            submission.setFileSize(fileSize);
        }
        
        return submissionRepository.save(submission);
    }

    // 과제의 모든 제출물 조회
    public List<AssignmentSubmission> getSubmissionsByAssignment(Long assignmentId) {
        return submissionRepository.findByAssignmentIdOrderBySubmittedAtDesc(assignmentId);
    }

    // 특정 사용자의 제출물 조회
    public Optional<AssignmentSubmission> getSubmission(Long assignmentId, Long userId) {
        return submissionRepository.findByAssignmentIdAndUserId(assignmentId, userId);
    }

    // 제출 여부 확인
    public boolean hasSubmitted(Long assignmentId, Long userId) {
        return submissionRepository.existsByAssignmentIdAndUserId(assignmentId, userId);
    }

    // 과제 채점 (작성자 전용)
    @Transactional
    public AssignmentSubmission gradeSubmission(Long submissionId, Integer score, String feedback) {
        AssignmentSubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new IllegalArgumentException("제출물을 찾을 수 없습니다."));
        
        submission.setScore(score);
        submission.setFeedback(feedback);
        submission.setStatus(AssignmentSubmission.SubmissionStatus.GRADED);
        submission.setGradedAt(LocalDateTime.now());
        
        return submissionRepository.save(submission);
    }

    // 제출물 조회
    public Optional<AssignmentSubmission> findSubmissionById(Long submissionId) {
        return submissionRepository.findById(submissionId);
    }
}
