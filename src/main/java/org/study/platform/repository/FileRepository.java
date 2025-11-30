package org.study.platform.repository;

import org.study.platform.entity.File;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface FileRepository extends JpaRepository<File, Long> {
    List<File> findByPostId(Long postId);
    List<File> findByPostIdOrderByUploadedAtAsc(Long postId);
}
