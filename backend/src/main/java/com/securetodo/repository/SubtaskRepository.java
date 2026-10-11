package com.securetodo.repository;

import com.securetodo.entity.Subtask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubtaskRepository extends JpaRepository<Subtask, Long> {

    List<Subtask> findByTaskId(Long taskId);

    @Query("SELECT s FROM Subtask s WHERE s.id = :id AND s.task.user.id = :userId")
    Optional<Subtask> findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);
}

