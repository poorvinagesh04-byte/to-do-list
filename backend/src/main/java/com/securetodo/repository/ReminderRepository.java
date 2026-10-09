package com.securetodo.repository;

import com.securetodo.entity.Reminder;
import com.securetodo.entity.ReminderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReminderRepository extends JpaRepository<Reminder, Long> {

    @Query("SELECT r FROM Reminder r JOIN FETCH r.task t JOIN FETCH r.user u " +
           "WHERE r.status = :status AND r.reminderDatetime <= :now")
    List<Reminder> findDueReminders(
        @Param("status") ReminderStatus status,
        @Param("now") LocalDateTime now
    );

    @Query("SELECT r FROM Reminder r JOIN FETCH r.task t WHERE r.user.id = :userId ORDER BY r.reminderDatetime DESC")
    List<Reminder> findAllByUserId(@Param("userId") Long userId);

    @Query("SELECT r FROM Reminder r JOIN FETCH r.task t WHERE r.user.id = :userId AND r.status = 'PENDING' ORDER BY r.reminderDatetime ASC")
    List<Reminder> findPendingByUserId(@Param("userId") Long userId);

    Optional<Reminder> findByDeduplicationKey(String deduplicationKey);

    Optional<Reminder> findByIdAndUserId(Long id, Long userId);

    List<Reminder> findByTaskId(Long taskId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Reminder r SET r.status = 'CANCELLED' WHERE r.task.id = :taskId AND r.status = 'PENDING'")
    void cancelPendingByTaskId(@Param("taskId") Long taskId);

    @Modifying
    @Query("DELETE FROM Reminder r WHERE r.task.id = :taskId")
    void deleteByTaskId(@Param("taskId") Long taskId);
}
