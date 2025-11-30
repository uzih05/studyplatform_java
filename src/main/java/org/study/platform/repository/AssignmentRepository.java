package org.study.platform.repository;

import org.study.platform.entity.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface AssignmentRepository extends JpaRepository<Assignment, Long> {

    Optional<Assignment> findByPostId(Long postId);
    
    List<Assignment> findByPostIdIn(List<Long> postIds);
}
