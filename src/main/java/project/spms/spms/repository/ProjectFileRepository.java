package project.spms.spms.repository;

import project.spms.spms.entity.ProjectFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ProjectFileRepository extends JpaRepository<ProjectFile, Integer> {
    List<ProjectFile> findByProjectId(Integer projectId);

    long countByProjectId(Integer projectId);

    void deleteAllByProjectId(Integer projectId);
}