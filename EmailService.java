package com.example.project2.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.Nullable;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {
    @Autowired(required = false)
    @Nullable
    private JavaMailSender mailSender;

    @Value("${mail.from:}")
    private String fromAddress;

    public void send(String to, String subject, String body) {
        try {
            if (mailSender != null) {
                SimpleMailMessage message = new SimpleMailMessage();
                if (fromAddress != null && !fromAddress.isBlank()) {
                    message.setFrom(fromAddress);
                }
                message.setTo(to);
                message.setSubject(subject);
                message.setText(body);
                mailSender.send(message);
                System.out.println("[EmailService] Email sent to " + to + ": " + subject);
            } else {
                // Fallback: логируем письмо, если JavaMailSender не сконфигурирован
                System.out.println("[EmailService][FAKE] Would send email to: " + to);
                System.out.println("Subject: " + subject);
                System.out.println("Body:\n" + body);
            }
        } catch (Exception e) {
            System.err.println("[EmailService] Failed to send email: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void sendHtml(String to, String subject, String htmlBody) {
        try {
            if (mailSender != null) {
                MimeMessage mime = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(mime, "UTF-8");
                if (fromAddress != null && !fromAddress.isBlank()) {
                    helper.setFrom(fromAddress);
                }
                helper.setTo(to);
                helper.setSubject(subject);
                helper.setText(htmlBody, true);
                mailSender.send(mime);
                System.out.println("[EmailService] HTML email sent to " + to + ": " + subject);
            } else {
                // Fallback: логируем HTML
                System.out.println("[EmailService][FAKE HTML] Would send email to: " + to);
                System.out.println("Subject: " + subject);
                System.out.println("HTML Body:\n" + htmlBody);
            }
        } catch (Exception e) {
            System.err.println("[EmailService] Failed to send HTML email: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
