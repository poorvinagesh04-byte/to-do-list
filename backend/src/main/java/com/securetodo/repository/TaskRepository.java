package com.securetodo.repository;

import com.securetodo.entity.Priority;
import com.securetodo.entity.Task;
import com.securetodo.entity.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long>, JpaSpecificationExecutor<Task> {

    Optional<Task> findByIdAndUserId(Long id, Long userId);

    List<Task> findAllByUserId(Long userId, Sort sort);

    List<Task> findByUserIdAndScheduledDate(Long userId, LocalDate date);

    List<Task> findByUserIdAndScheduledDateBetweenOrderByScheduledDateAscStartTimeAsc(
        Long userId, LocalDate startDate, LocalDate endDate
    );

    List<Task> findByUserIdAndStatusOrderByScheduledDateDesc(Long userId, TaskStatus status);

    long countByUserIdAndScheduledDate(Long userId, LocalDate date);

    long countByUserIdAndScheduledDateAndStatus(Long userId, LocalDate date, TaskStatus status);

    @Query("SELECT COUNT(t) FROM Task t WHERE t.user.id = :userId AND t.status = 'PENDING' AND t.scheduledDate < :today")
    long countOverdueTasks(@Param("userId") Long userId, @Param("today") LocalDate today);

    @Query("SELECT t FROM Task t WHERE t.user.id = :userId AND t.status = 'PENDING' AND t.scheduledDate < :today ORDER BY t.scheduledDate ASC")
    List<Task> findOverdueTasks(@Param("userId") Long userId, @Param("today") LocalDate today);

    @Query("SELECT t FROM Task t WHERE t.user.id = :userId AND t.status = 'PENDING' AND (t.priority = 'HIGH' OR t.priority = 'URGENT') ORDER BY t.scheduledDate ASC")
    List<Task> findHighPriorityTasks(@Param("userId") Long userId);

    @Query("SELECT t FROM Task t WHERE t.user.id = :userId AND t.status = 'PENDING' AND t.scheduledDate > :today AND t.scheduledDate <= :futureDate ORDER BY t.scheduledDate ASC")
    List<Task> findUpcomingTasks(@Param("userId") Long userId, @Param("today") LocalDate today, @Param("futureDate") LocalDate futureDate);

    @Query("SELECT t FROM Task t WHERE t.user.id = :userId AND (" +
           "LOWER(t.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(t.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<Task> searchTasks(@Param("userId") Long userId, @Param("keyword") String keyword);
}

