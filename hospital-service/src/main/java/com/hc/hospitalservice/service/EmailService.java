package com.hc.hospitalservice.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class EmailService {
    private final JavaMailSender javaMailSender;
    public void sendEmail(String email,
                          String firstName,
                          String hospitalName)
    {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(email);
            message.setSubject("Patient Registration Confirmation");
            message.setText(buildRegistrationEmail(firstName, hospitalName));
            log.info("Sending mail to email: {}", email);
            javaMailSender.send(message);
        }catch (Exception exception){
            log.error("Error sending mail to email: {}", email, exception);
            throw new RuntimeException("error sending email", exception);
        }
    }

    private String buildRegistrationEmail(String firstName, String hospitalName) {
        return  String.format("""
                Dear %s
                
                you have successfully registered your hospital information under %s
                
                the admin has confirmed your hospital information and you can now log in
                
                Best regards,
                The HealthCare Hub Team
                """,  firstName, hospitalName);
    }


}
