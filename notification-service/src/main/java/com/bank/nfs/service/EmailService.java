package com.bank.nfs.service;

import com.bank.common.events.EmailNotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendEmail( EmailNotificationEvent request) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(request.getTo());
        message.setSubject(request.getSubject());
        message.setText(request.getBody());
        try {
            mailSender.send(message);
            log.info("Email sent successfully to {}",request.getTo());
        }
        catch (MailException ex) {
            log.error("Unable to send email",ex);
            throw ex;
        }
    }
}