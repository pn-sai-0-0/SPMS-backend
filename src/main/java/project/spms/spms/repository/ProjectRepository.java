package project.spms.spms.repository;

import project.spms.spms.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Integer> {
    List<Project> findByStatus(Project.ProjectStatus status);

    List<Project> findByDepartmentName(String departmentName);

    List<Project> findByDepartmentId(Integer departmentId);

    @Query("SELECT p FROM Project p WHERE p.id IN (SELECT pa.projectId FROM ProjectAssignment pa WHERE pa.userId=:uid)")
    List<Project> findByUserId(@Param("uid") Integer uid);

    @Query("SELECT p FROM Project p WHERE p.status=:status AND p.id IN (SELECT pa.projectId FROM ProjectAssignment pa WHERE pa.userId=:uid)")
    List<Project> findByUserIdAndStatus(@Param("uid") Integer uid, @Param("status") Project.ProjectStatus status);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Project p SET p.progress=:prog, p.progressNote=:note WHERE p.id=:id")
    void updateProgress(@Param("id") Integer id, @Param("prog") Integer prog, @Param("note") String note);

    boolean existsByCode(String code);
}