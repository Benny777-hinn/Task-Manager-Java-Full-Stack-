package com.taskmanager.scheduler;

import com.taskmanager.entity.Task;
import com.taskmanager.entity.TaskStatus;
import com.taskmanager.repository.TaskRepository;
import com.taskmanager.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * The email reminder job.
 *
 * @Scheduled(cron = ...) makes Spring call runReminderJob() automatically.
 * "0 0 * * * *" is a 6-field Spring cron meaning: second 0, minute 0, every hour,
 * so the job runs once per hour on the hour.
 *
 * The "reminderSent" flag on Task guarantees we email about a task only once.
 */
@Component
public class ReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(ReminderScheduler.class);

    private final TaskRepository taskRepository;
    private final EmailService emailService;

    public ReminderScheduler(TaskRepository taskRepository, EmailService emailService) {
        this.taskRepository = taskRepository;
        this.emailService = emailService;
    }

    @Scheduled(cron = "${app.reminder.cron}")
    @Transactional
    public void runReminderJob() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime in24Hours = now.plusHours(24);

        // PENDING tasks due in the next 24 hours that we have not emailed about yet.
        List<Task> tasks = taskRepository.findTasksDueForReminder(TaskStatus.PENDING, now, in24Hours);

        log.info("Reminder job started: {} task(s) due within 24 hours", tasks.size());

        for (Task task : tasks) {
            try {
                emailService.sendReminderEmail(task.getOwner().getEmail(), task.getOwner().getUsername(), task);
                // Mark as reminded ONLY after the email went out, so a failure is retried next hour.
                task.setReminderSent(true);
                taskRepository.save(task);
            } catch (Exception e) {
                // One bad email must not stop the whole job.
                log.error("Failed to send reminder for task {}: {}", task.getId(), e.getMessage());
            }
        }

        log.info("Reminder job finished");
    }
}
