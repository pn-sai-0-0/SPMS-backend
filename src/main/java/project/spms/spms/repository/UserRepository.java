package project.spms.spms.repository;

import project.spms.spms.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByUsername(String username);

    Optional<User> findByUsernameAndRole(String username, User.Role role);

    List<User> findByRole(User.Role role);

    List<User> findByManagerId(Integer managerId);

    List<User> findByManagerIdAndRole(Integer managerId, User.Role role);

    List<User> findByDepartmentName(String departmentName);

    List<User> findByDepartmentNameAndRole(String departmentName, User.Role role);

    List<User> findByStatus(User.Status status);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    @Modifying
    @Query(value = "UPDATE users SET last_login=:t WHERE id=:id", nativeQuery = true)
    void updateLastLogin(@Param("id") Integer id, @Param("t") LocalDateTime t);

    @Modifying
    @Query(value = "UPDATE users SET status=:status WHERE id=:id", nativeQuery = true)
    void updateStatus(@Param("id") Integer id, @Param("status") String status);

    @Query("SELECT COUNT(pa) FROM ProjectAssignment pa JOIN Project p ON pa.projectId=p.id WHERE pa.userId=:uid AND p.status='active'")
    Long countActiveProjectsByUserId(@Param("uid") Integer uid);
}