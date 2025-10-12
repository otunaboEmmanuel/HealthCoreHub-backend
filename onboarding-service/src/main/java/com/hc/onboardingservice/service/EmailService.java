package com.hc.onboardingservice.service;

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

    public void sendHospitalActivationEmail(String toEmail,
                                            String adminName,
                                            String hospitalName,
                                            String tenantDb,
                                            String password) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject("Welcome to HealthCare Hub - Hospital Portal Ready!");
            message.setText(buildEmailContent(adminName, hospitalName, tenantDb, password));

            mailSender.send(message);
            log.info("✅ Sent activation email to: {}", toEmail);

        } catch (Exception e) {
            log.error("❌ Failed to send email to: {}", toEmail, e);
            throw new RuntimeException("Email sending failed", e);
        }
    }

    private String buildEmailContent(String adminName, String hospitalName, String tenantDb,String password) {
        return String.format("""
            Dear %s,
            
            Congratulations! Your hospital portal for %s is now ready.
            
            You can now log in to your dedicated hospital management system and start managing your operations.
            
            Your Hospital Details:
            - Hospital Name: %s
            -Password: %s
            - Database: %s
            - Status: Active
            
            Next Steps:
            1. Log in to your portal
            2. Complete your hospital profile
            3. Add staff members
            4. Configure your departments
            5. Start managing patients
            
            If you have any questions, please don't hesitate to contact our support team.
            
            Best regards,
            The HealthCare Hub Team
            """, adminName, hospitalName, hospitalName, tenantDb, password);
    }


}