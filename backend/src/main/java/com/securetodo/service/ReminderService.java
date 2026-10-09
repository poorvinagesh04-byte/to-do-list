package com.securetodo.service;

import com.securetodo.dto.ReminderDto;
import com.securetodo.entity.*;
import com.securetodo.repository.ReminderRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ReminderService {

    private final ReminderRepository reminderRepository;

    public ReminderService(ReminderRepository reminderRepository) {
        this.reminderRepository = reminderRepository;
    }

    @Transactional
    public void createOrUpdateRemindersForTask(Task task, List<Integer> offsetMinutesList) {
        if (offsetMinutesList == null || offsetMinutesList.isEmpty()) {
            return;
        }

        LocalTime time = task.getStartTime() != null ? task.getStartTime() : LocalTime.of(9, 0);
        LocalDateTime baseDateTime = LocalDateTime.of(task.getScheduledDate(), time);

        for (Integer offset : offsetMinutesList) {
            if (offset == null) continue;
            LocalDateTime reminderTime = baseDateTime.minusMinutes(offset);
            String dedupKey = "u_" + task.getUser().getId() + "_t_" + task.getId() + "_m_" + offset;

            Optional<Reminder> existing = reminderRepository.findByDeduplicationKey(dedupKey);
            if (existing.isPresent()) {
                Reminder reminder = existing.get();
                reminder.setReminderDatetime(reminderTime);
                if (reminder.getStatus() == ReminderStatus.CANCELLED) {
                    reminder.setStatus(ReminderStatus.PENDING);
                }
                Reminder saved = reminderRepository.save(reminder);
                if (!task.getReminders().contains(saved)) {
                    task.getReminders().add(saved);
                }
            } else {
                Reminder reminder = new Reminder(
                        task,
                        task.getUser(),
                        reminderTime,
                        offset,
                        DeliveryMethod.IN_APP,
                        dedupKey
                );
                Reminder saved = reminderRepository.save(reminder);
                task.getReminders().add(saved);
            }
        }
    }

    @Transactional
    public void cancelRemindersForTask(Long taskId) {
        reminderRepository.cancelPendingByTaskId(taskId);
    }

    @Transactional
    public void deleteRemindersForTask(Long taskId) {
        reminderRepository.deleteByTaskId(taskId);
    }

    public List<Reminder> getDueReminders() {
        return reminderRepository.findDueReminders(ReminderStatus.PENDING, LocalDateTime.now());
    }

    @Transactional
    public void markSent(Reminder reminder) {
        reminder.setStatus(ReminderStatus.SENT);
        reminder.setSentAt(LocalDateTime.now());
        reminderRepository.save(reminder);
    }

    public List<ReminderDto> getUserReminders(User user) {
        return reminderRepository.findAllByUserId(user.getId())
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public List<ReminderDto> getPendingReminders(User user) {
        return reminderRepository.findPendingByUserId(user.getId())
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public void dismissReminder(User user, Long reminderId) {
        Reminder reminder = reminderRepository.findByIdAndUserId(reminderId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reminder not found"));
        reminder.setStatus(ReminderStatus.DISMISSED);
        reminderRepository.save(reminder);
    }

    @Transactional
    public void snoozeReminder(User user, Long reminderId, int minutes) {
        Reminder reminder = reminderRepository.findByIdAndUserId(reminderId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reminder not found"));
        reminder.setReminderDatetime(LocalDateTime.now().plusMinutes(minutes > 0 ? minutes : 10));
        reminder.setStatus(ReminderStatus.PENDING);
        reminderRepository.save(reminder);
    }

    public ReminderDto mapToDto(Reminder reminder) {
        ReminderDto dto = new ReminderDto();
        dto.setId(reminder.getId());
        dto.setTaskId(reminder.getTask().getId());
        dto.setTaskTitle(reminder.getTask().getTitle());
        dto.setReminderDatetime(reminder.getReminderDatetime());
        dto.setOffsetMinutes(reminder.getOffsetMinutes());
        dto.setDeliveryMethod(reminder.getDeliveryMethod());
        dto.setStatus(reminder.getStatus());
        dto.setDeduplicationKey(reminder.getDeduplicationKey());
        return dto;
    }
}
