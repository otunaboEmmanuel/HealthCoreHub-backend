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
                The HealthCore Hub Team
                """,  firstName, hospitalName);
    }
    public void sendUserRegistrationEmail(String email, String role, String firstName, String hospitalName, String password) {
        try{
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(email);
            message.setSubject("User Registration Confirmation");
            message.setText(buildUserRegistrationEmail(firstName, hospitalName, role, password));
            javaMailSender.send(message);
            log.info("Sending email: {}", email);
    }catch (Exception e){
            log.error("Error sending mail to email: {}", email, e);
            throw new RuntimeException("error sending email", e);
        }
    }

    private String buildUserRegistrationEmail(String firstName, String hospitalName, String role, String password) {
        return String.format("""
            Dear %s,
            
            Welcome to %s!
            
            Your account has been successfully created with the role of *%s*.
            
            You can log in to your account using your registered email and the temporary password below:
            
            Temporary Password: %s
            
            Please log in and change your password immediately after your first sign-in for security reasons.
            
            If you have any issues accessing your account, feel free to reach out to our support team.
            
            Best regards,
            The HealthCore Hub Team
            """, firstName, hospitalName, role, password);
    }


}
