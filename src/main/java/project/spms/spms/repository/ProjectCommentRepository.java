package project.spms.spms.repository;

import project.spms.spms.entity.ProjectComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ProjectCommentRepository extends JpaRepository<ProjectComment, Integer> {
    List<ProjectComment> findByProjectIdOrderByCreatedAtDesc(Integer projectId);
}