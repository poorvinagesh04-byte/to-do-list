package com.securetodo.repository;

import com.securetodo.entity.TaskHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskHistoryRepository extends JpaRepository<TaskHistory, Long> {

    List<TaskHistory> findByUserIdOrderByTimestampDesc(Long userId, Pageable pageable);

    List<TaskHistory> findByTaskIdOrderByTimestampDesc(Long taskId);
}

