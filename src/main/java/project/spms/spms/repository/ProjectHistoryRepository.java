package project.spms.spms.repository;

import project.spms.spms.entity.ProjectHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ProjectHistoryRepository extends JpaRepository<ProjectHistory, Integer> {
    List<ProjectHistory> findByProjectIdOrderByCreatedAtDesc(Integer projectId);

    List<ProjectHistory> findTop10ByProjectIdOrderByCreatedAtDesc(Integer projectId);
}