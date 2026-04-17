package project.spms.spms.repository;

import project.spms.spms.entity.ProjectAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectAssignmentRepository extends JpaRepository<ProjectAssignment, Integer> {
    List<ProjectAssignment> findByProjectId(Integer projectId);

    List<ProjectAssignment> findByUserId(Integer userId);

    Optional<ProjectAssignment> findByProjectIdAndUserId(Integer projectId, Integer userId);

    boolean existsByProjectIdAndUserId(Integer projectId, Integer userId);

    @Modifying
    @Query("DELETE FROM ProjectAssignment pa WHERE pa.projectId=:pid AND pa.userId=:uid")
    void deleteByProjectIdAndUserId(@Param("pid") Integer pid, @Param("uid") Integer uid);

    Long countByUserId(Integer userId);

    @Modifying
    @Query("DELETE FROM ProjectAssignment pa WHERE pa.projectId=:pid")
    void deleteAllByProjectId(@Param("pid") Integer pid);
}