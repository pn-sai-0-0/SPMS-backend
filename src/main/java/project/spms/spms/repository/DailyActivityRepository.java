package project.spms.spms.repository;

import project.spms.spms.entity.DailyActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DailyActivityRepository extends JpaRepository<DailyActivity, Integer> {
    List<DailyActivity> findByUserIdOrderByActivityDateDesc(Integer userId);

    Optional<DailyActivity> findByUserIdAndActivityDate(Integer userId, LocalDate date);

    @Query("SELECT da FROM DailyActivity da WHERE da.userId = :uid " +
            "AND YEAR(da.activityDate) = :year ORDER BY da.activityDate DESC")
    List<DailyActivity> findByUserIdAndYear(@Param("uid") Integer uid, @Param("year") int year);

    @Query("SELECT da FROM DailyActivity da WHERE da.userId = :uid " +
            "AND YEAR(da.activityDate) = :year AND MONTH(da.activityDate) = :month " +
            "ORDER BY da.activityDate DESC")
    List<DailyActivity> findByUserIdAndYearAndMonth(@Param("uid") Integer uid,
            @Param("year") int year,
            @Param("month") int month);

    @Query("SELECT SUM(da.hoursWorked) FROM DailyActivity da WHERE da.userId = :uid " +
            "AND da.activityDate >= :from AND da.activityDate <= :to")
    Double sumHoursInRange(@Param("uid") Integer uid,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    @Query("SELECT SUM(da.tasksDone) FROM DailyActivity da WHERE da.userId = :uid " +
            "AND da.activityDate >= :from AND da.activityDate <= :to")
    Integer sumTasksInRange(@Param("uid") Integer uid,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);
}