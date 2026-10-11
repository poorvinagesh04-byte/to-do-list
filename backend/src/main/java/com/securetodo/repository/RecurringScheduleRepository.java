package com.securetodo.repository;

import com.securetodo.entity.RecurrenceType;
import com.securetodo.entity.RecurringSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RecurringScheduleRepository extends JpaRepository<RecurringSchedule, Long> {

    Optional<RecurringSchedule> findByTaskId(Long taskId);

    @Query("SELECT r FROM RecurringSchedule r JOIN FETCH r.task t WHERE r.recurrenceType <> 'NONE'")
    List<RecurringSchedule> findAllActiveSchedules();
}

