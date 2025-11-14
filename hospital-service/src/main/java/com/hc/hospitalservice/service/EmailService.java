package com.hc.hospitalservice.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
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
    public void sendActivationEmail(String toEmail, String firstName, String activationLink, String role) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom("noreply@gmail.com");
            helper.setTo(toEmail);
            helper.setSubject("Activate Your Healthcare Account");

            String htmlContent = buildActivationEmailHtml(firstName, activationLink, role);
            helper.setText(htmlContent, true);

            javaMailSender.send(message);
            log.info(" Activation email sent to: {}", toEmail);

        } catch (MessagingException e) {
            log.error(" Failed to send activation email to: {}", toEmail, e);
            throw new RuntimeException("Failed to send activation email", e);
        }
    }

    private String buildActivationEmailHtml(String firstName, String activationLink, String role) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #4CAF50; color: white; padding: 20px; text-align: center; }
                    .content { padding: 20px; background-color: #f9f9f9; }
                    .button {
                        display: inline-block;
                        padding: 12px 30px;
                        background-color: #4CAF50;
                        color: white;
                        text-decoration: none;
                        border-radius: 5px;
                        margin: 20px 0;
                    }
                    .footer { text-align: center; padding: 20px; color: #666; font-size: 12px; }
                    .warning { background-color: #fff3cd; padding: 10px; border-left: 4px solid #ffc107; margin: 20px 0; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>Welcome to Healthcare Hub</h1>
                    </div>
                    <div class="content">
                        <p>Hello %s,</p>
                        <p>You have been invited to join our HealthCoreHub platform as a %s.</p>
                        <p>To activate your account and set your password, please click the button below:</p>
                        <p style="text-align: center;">
                            <a href="%s" class="button">Activate Account</a>
                        </p>
                        <p>Or copy and paste this link into your browser:</p>
                        <p style="word-break: break-all; background-color: #f0f0f0; padding: 10px;">%s</p>
                        <div class="warning">
                            <strong> Important:</strong> This link will expire in 24 hours.
                        </div>
                        <p>If you did not expect this invitation, please ignore this email or contact your administrator.</p>
                    </div>
                    <div class="footer">
                        <p>© 2025 HealthCore Hub. All rights reserved.</p>
                        <p>This is an automated message. Please do not reply to this email.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(firstName, role, activationLink, activationLink);
    }

}
