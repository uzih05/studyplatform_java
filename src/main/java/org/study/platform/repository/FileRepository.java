package org.study.platform.repository;

import org.study.platform.entity.File;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface FileRepository extends JpaRepository<File, Long> {

    // 특정 게시글의 첨부파일 목록 조회
    List<File> findByPostId(Long postId);

    // 특정 게시글의 첨부파일을 업로드일순으로 조회
    List<File> findByPostIdOrderByUploadedAtAsc(Long postId);
}