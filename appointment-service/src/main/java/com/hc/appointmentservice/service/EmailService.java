package com.hc.appointmentservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {
    private final JavaMailSender mailSender;
    public void sendSimpleMail(String email, String firstName, String appointmentTime, String docName) {
        try {
            log.info("sending mail to {}", email);
            SimpleMailMessage mailMessage = new SimpleMailMessage();
            mailMessage.setTo(email);
            mailMessage.setSubject("DOCTOR APPOINTMENT UPDATE EMAIL");
            mailMessage.setText(buildEmailContext(firstName, appointmentTime, docName));
            mailSender.send(mailMessage);
        }catch (Exception e){
            log.error(e.getMessage());
            throw  new RuntimeException(e.getMessage());
        }
    }

    private String buildEmailContext(String firstName, String appointmentTime, String docName) {
        return String.format("""
                DEAR %s your appointment has been updated
                you will have your appointment at %s with Dr %s
                THANK YOU
                Best regards,
                The HealthCore Hub Team
                """, firstName,appointmentTime,docName);
    }

}
