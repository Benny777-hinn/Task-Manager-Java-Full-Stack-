package com.taskmanager.service;

import com.taskmanager.entity.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Sends the reminder emails. Kept separate from the scheduler so the scheduler's
 * only job is "find tasks and call this service", which is easy to test.
 *
 * During local development MAIL_HOST points at MailHog, which shows the emails
 * in a web UI instead of really sending them.
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final String fromAddress;

    public EmailService(JavaMailSender mailSender,
                        @Value("${app.mail-from}") String fromAddress) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    /** Send a simple "your task is due soon" email. */
    public void sendReminderEmail(String to, String username, Task task) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(to);
        message.setSubject("Reminder: \"" + task.getTitle() + "\" is due soon");
        message.setText(String.format(
                "Hi %s,%n%n" +
                        "This is a friendly reminder that your task is due within 24 hours.%n%n" +
                        "Title:    %s%n" +
                        "Due:      %s%n" +
                        "Priority: %s%n" +
                        "Status:   %s%n%n" +
                        "Open the Task Manager to view or complete it.%n",
                username, task.getTitle(), task.getDueDate(), task.getPriority(), task.getStatus()));

        mailSender.send(message);
        log.info("Reminder email sent to {} for task '{}'", to, task.getTitle());
    }
}
