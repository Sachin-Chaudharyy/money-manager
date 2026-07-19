package com.project.moneymanager.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.MimeMessageHelper;
import java.io.ByteArrayInputStream;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    @Value("${spring.mail.properties.mail.smtp.from}")
    private String fromEmail;

    public void sendEmail(String to, String subject, String body) {
        try{
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
        }catch(Exception e){
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public void sendEmailWithAttachment(String to, String subject, String body,
                                        ByteArrayInputStream attachment, String attachmentName) {
        try {
            System.out.println("Sending email to: " + to);
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setTo(to);
            helper.setFrom(fromEmail);
            helper.setSubject(subject);
            helper.setText(body);
            helper.addAttachment(attachmentName, new jakarta.mail.util.ByteArrayDataSource(
                    attachment.readAllBytes(), "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            mailSender.send(message);
            System.out.println("Email sent successfully to: " + to);
        } catch (MessagingException e) {
            System.err.println("Failed to send email: " + e.getMessage());
            throw new RuntimeException("Failed to send email: " + e.getMessage());
        }
    }
}
