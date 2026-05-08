package com.connectsphere.auth.email;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.frontend.url:http://localhost:4200}")
    private String frontendUrl;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Async
    public void sendPasswordResetEmail(String toEmail, String name, String token) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, "ConnectSphere");
            helper.setTo(toEmail);
            helper.setSubject("Reset Your ConnectSphere Password");

            String resetLink = frontendUrl + "/auth/reset-password?token=" + token;
            String html = buildPasswordResetHtml(name != null ? name : "User", resetLink);
            helper.setText(html, true);

            mailSender.send(message);
            log.info("Password reset email sent to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send password reset email to {}: {}", toEmail, e.getMessage());
        }
    }

    @Async
    public void sendWelcomeEmail(String toEmail, String name) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, "ConnectSphere");
            helper.setTo(toEmail);
            helper.setSubject("Welcome to ConnectSphere! 🚀");

            String html = buildWelcomeHtml(name != null ? name : "User");
            helper.setText(html, true);

            mailSender.send(message);
        } catch (Exception e) {
            log.error("Failed to send welcome email: {}", e.getMessage());
        }
    }

    private String buildPasswordResetHtml(String name, String resetLink) {
        return """
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8"></head>
            <body style="margin:0;padding:0;background-color:#0A0E1A;font-family:'Roboto',sans-serif;">
              <div style="max-width:600px;margin:40px auto;background:#0D1117;border:1px solid #1E2535;border-radius:16px;overflow:hidden;">
                <div style="background:linear-gradient(135deg,#00D4FF,#7B2FBE);padding:32px;text-align:center;">
                  <h1 style="color:#fff;margin:0;font-size:28px;letter-spacing:2px;">ConnectSphere</h1>
                  <p style="color:rgba(255,255,255,0.8);margin:8px 0 0;">Share Moments. Build Connections.</p>
                </div>
                <div style="padding:40px;">
                  <h2 style="color:#E8EAF0;margin:0 0 16px;">Reset Your Password</h2>
                  <p style="color:#6B7280;line-height:1.6;">Hi %s,</p>
                  <p style="color:#6B7280;line-height:1.6;">We received a request to reset your ConnectSphere password. Click the button below to create a new password. This link expires in 1 hour.</p>
                  <div style="text-align:center;margin:32px 0;">
                    <a href="%s" style="display:inline-block;background:linear-gradient(135deg,#00D4FF,#7B2FBE);color:#fff;text-decoration:none;padding:14px 36px;border-radius:50px;font-weight:bold;font-size:16px;">Reset Password</a>
                  </div>
                  <p style="color:#6B7280;font-size:13px;">If you didn't request this, you can safely ignore this email.</p>
                  <hr style="border:none;border-top:1px solid #1E2535;margin:24px 0;">
                  <p style="color:#6B7280;font-size:12px;text-align:center;">© 2026 ConnectSphere. All rights reserved.</p>
                </div>
              </div>
            </body>
            </html>
            """.formatted(name, resetLink);
    }

    private String buildWelcomeHtml(String name) {
        return """
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8"></head>
            <body style="margin:0;padding:0;background-color:#0A0E1A;font-family:'Roboto',sans-serif;">
              <div style="max-width:600px;margin:40px auto;background:#0D1117;border:1px solid #1E2535;border-radius:16px;overflow:hidden;">
                <div style="background:linear-gradient(135deg,#00D4FF,#7B2FBE);padding:32px;text-align:center;">
                  <h1 style="color:#fff;margin:0;font-size:28px;letter-spacing:2px;">ConnectSphere</h1>
                  <p style="color:rgba(255,255,255,0.8);margin:8px 0 0;">Share Moments. Build Connections.</p>
                </div>
                <div style="padding:40px;">
                  <h2 style="color:#E8EAF0;">Welcome aboard, %s! 🎉</h2>
                  <p style="color:#6B7280;line-height:1.6;">Your account has been created. Start sharing moments, connecting with people, and discovering amazing content.</p>
                  <hr style="border:none;border-top:1px solid #1E2535;margin:24px 0;">
                  <p style="color:#6B7280;font-size:12px;text-align:center;">© 2026 ConnectSphere. All rights reserved.</p>
                </div>
              </div>
            </body>
            </html>
            """.formatted(name);
    }

    @Async
    public void sendOtpEmail(String toEmail, String name, String otp) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, "ConnectSphere");
            helper.setTo(toEmail);
            helper.setSubject("Your ConnectSphere Password Reset OTP");

            String html = buildOtpEmailHtml(name != null ? name : "User", otp);
            helper.setText(html, true);

            mailSender.send(message);
            log.info("OTP email sent to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send OTP email to {}: {}", toEmail, e.getMessage());
        }
    }

    private String buildOtpEmailHtml(String name, String otp) {
        return """
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8"></head>
            <body style="margin:0;padding:0;background-color:#0A0E1A;font-family:'Roboto',sans-serif;">
              <div style="max-width:600px;margin:40px auto;background:#0D1117;border:1px solid #1E2535;border-radius:16px;overflow:hidden;">
                <div style="background:linear-gradient(135deg,#00D4FF,#7B2FBE);padding:32px;text-align:center;">
                  <h1 style="color:#fff;margin:0;font-size:28px;letter-spacing:2px;">ConnectSphere</h1>
                  <p style="color:rgba(255,255,255,0.8);margin:8px 0 0;">Password Reset Request</p>
                </div>
                <div style="padding:40px;">
                  <h2 style="color:#E8EAF0;margin:0 0 16px;">Reset Your Password</h2>
                  <p style="color:#6B7280;line-height:1.6;">Hi %s,</p>
                  <p style="color:#6B7280;line-height:1.6;">Use the OTP below to reset your password. This code expires in <strong style="color:#E8EAF0;">10 minutes</strong>.</p>
                  <div style="text-align:center;margin:36px 0;">
                    <div style="display:inline-block;background:#1A2235;padding:20px 40px;border-radius:16px;border:1px solid #2A344A;">
                      <p style="color:#6B7280;font-size:13px;margin:0 0 10px;letter-spacing:1px;text-transform:uppercase;">Your OTP</p>
                      <span style="color:#00D4FF;font-size:40px;font-weight:bold;letter-spacing:12px;font-family:monospace;">%s</span>
                    </div>
                  </div>
                  <p style="color:#6B7280;font-size:13px;">If you did not request a password reset, you can safely ignore this email. Your password will not change.</p>
                  <hr style="border:none;border-top:1px solid #1E2535;margin:24px 0;">
                  <p style="color:#6B7280;font-size:12px;text-align:center;">© 2026 ConnectSphere. All rights reserved.</p>
                </div>
              </div>
            </body>
            </html>
            """.formatted(name, otp);
    }

    @Async
    public void sendOtpPasswordEmail(String toEmail, String name, String otpPassword) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, "ConnectSphere");
            helper.setTo(toEmail);
            helper.setSubject("Your New ConnectSphere Password");

            String html = buildOtpPasswordHtml(name != null ? name : "User", otpPassword);
            helper.setText(html, true);

            mailSender.send(message);
            log.info("OTP password email sent to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send OTP password email to {}: {}", toEmail, e.getMessage());
        }
    }

    @Async
    public void sendPaymentReceiptEmail(String toEmail, String name, int amountPaise, String currency, String orderId) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, "ConnectSphere");
            helper.setTo(toEmail);
            helper.setSubject("Payment Receipt - ConnectSphere Verification");

            String html = buildPaymentReceiptHtml(name != null ? name : "User", amountPaise, currency, orderId);
            helper.setText(html, true);

            mailSender.send(message);
            log.info("Payment receipt email sent to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send payment receipt email to {}: {}", toEmail, e.getMessage());
        }
    }

    @Async
    public void sendRefundEmail(String toEmail, String name, String amount, String currency, String orderId) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, "ConnectSphere");
            helper.setTo(toEmail);
            helper.setSubject("Refund Processed - ConnectSphere Verification");

            String html = buildRefundHtml(name != null ? name : "User", amount, currency, orderId);
            helper.setText(html, true);

            mailSender.send(message);
            log.info("Refund email sent to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send refund email to {}: {}", toEmail, e.getMessage());
        }
    }

    private String buildOtpPasswordHtml(String name, String otpPassword) {
        return """
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8"></head>
            <body style="margin:0;padding:0;background-color:#0A0E1A;font-family:'Roboto',sans-serif;">
              <div style="max-width:600px;margin:40px auto;background:#0D1117;border:1px solid #1E2535;border-radius:16px;overflow:hidden;">
                <div style="background:linear-gradient(135deg,#00D4FF,#7B2FBE);padding:32px;text-align:center;">
                  <h1 style="color:#fff;margin:0;font-size:28px;letter-spacing:2px;">ConnectSphere</h1>
                  <p style="color:rgba(255,255,255,0.8);margin:8px 0 0;">Share Moments. Build Connections.</p>
                </div>
                <div style="padding:40px;">
                  <h2 style="color:#E8EAF0;margin:0 0 16px;">Your New Password</h2>
                  <p style="color:#6B7280;line-height:1.6;">Hi %s,</p>
                  <p style="color:#6B7280;line-height:1.6;">As requested, your password has been reset. Please use the temporary password below to log in to your account. We highly recommend changing your password after logging in.</p>
                  <div style="text-align:center;margin:32px 0;">
                    <div style="display:inline-block;background:#1A2235;color:#00D4FF;padding:16px 32px;border-radius:12px;font-size:24px;font-weight:bold;letter-spacing:4px;border:1px solid #2A344A;">
                      %s
                    </div>
                  </div>
                  <p style="color:#6B7280;font-size:13px;">If you didn't request a password reset, please contact support immediately.</p>
                  <hr style="border:none;border-top:1px solid #1E2535;margin:24px 0;">
                  <p style="color:#6B7280;font-size:12px;text-align:center;">© 2026 ConnectSphere. All rights reserved.</p>
                </div>
              </div>
            </body>
            </html>
            """.formatted(name, otpPassword);
    }

    private String buildPaymentReceiptHtml(String name, int amountPaise, String currency, String orderId) {
        String formattedAmount = String.format("%.2f", amountPaise / 100.0);
        return """
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8"></head>
            <body style="margin:0;padding:0;background-color:#0A0E1A;font-family:'Roboto',sans-serif;">
              <div style="max-width:600px;margin:40px auto;background:#0D1117;border:1px solid #1E2535;border-radius:16px;overflow:hidden;">
                <div style="background:linear-gradient(135deg,#00D4FF,#7B2FBE);padding:32px;text-align:center;">
                  <h1 style="color:#fff;margin:0;font-size:28px;letter-spacing:2px;">ConnectSphere</h1>
                  <p style="color:rgba(255,255,255,0.8);margin:8px 0 0;">Payment Receipt</p>
                </div>
                <div style="padding:40px;">
                  <h2 style="color:#E8EAF0;margin:0 0 16px;">Thank you for your payment!</h2>
                  <p style="color:#6B7280;line-height:1.6;">Hi %s,</p>
                  <p style="color:#6B7280;line-height:1.6;">We have successfully received your payment for the ConnectSphere Verification.</p>
                  
                  <div style="background:#1A2235;border-radius:12px;padding:24px;margin:24px 0;border:1px solid #2A344A;">
                    <div style="display:flex;justify-content:space-between;margin-bottom:12px;">
                      <span style="color:#6B7280;">Order ID:</span>
                      <span style="color:#E8EAF0;font-family:monospace;">%s</span>
                    </div>
                    <div style="display:flex;justify-content:space-between;margin-bottom:12px;">
                      <span style="color:#6B7280;">Description:</span>
                      <span style="color:#E8EAF0;">Account Verification</span>
                    </div>
                    <hr style="border:none;border-top:1px dashed #2A344A;margin:16px 0;">
                    <div style="display:flex;justify-content:space-between;">
                      <span style="color:#E8EAF0;font-weight:bold;">Total Paid:</span>
                      <span style="color:#00D4FF;font-weight:bold;font-size:18px;">%s %s</span>
                    </div>
                  </div>
                  
                  <p style="color:#6B7280;font-size:14px;line-height:1.6;">Your account verification is currently pending admin approval. You will be notified once it is approved.</p>
                  <hr style="border:none;border-top:1px solid #1E2535;margin:24px 0;">
                  <p style="color:#6B7280;font-size:12px;text-align:center;">© 2026 ConnectSphere. All rights reserved.</p>
                </div>
              </div>
            </body>
            </html>
            """.formatted(name, orderId, formattedAmount, currency);
    }

    private String buildRefundHtml(String name, String amount, String currency, String orderId) {
        return """
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8"></head>
            <body style="margin:0;padding:0;background-color:#0A0E1A;font-family:'Roboto',sans-serif;">
              <div style="max-width:600px;margin:40px auto;background:#0D1117;border:1px solid #1E2535;border-radius:16px;overflow:hidden;">
                <div style="background:linear-gradient(135deg,#FF4B2B,#FF416C);padding:32px;text-align:center;">
                  <h1 style="color:#fff;margin:0;font-size:28px;letter-spacing:2px;">ConnectSphere</h1>
                  <p style="color:rgba(255,255,255,0.8);margin:8px 0 0;">Refund Processed</p>
                </div>
                <div style="padding:40px;">
                  <h2 style="color:#E8EAF0;margin:0 0 16px;">Your refund has been initiated</h2>
                  <p style="color:#6B7280;line-height:1.6;">Hi %s,</p>
                  <p style="color:#6B7280;line-height:1.6;">We're writing to let you know that your verification request has been denied by the admin, and a refund for your payment has been initiated.</p>
                  
                  <div style="background:#1A2235;border-radius:12px;padding:24px;margin:24px 0;border:1px solid #2A344A;">
                    <div style="display:flex;justify-content:space-between;margin-bottom:12px;">
                      <span style="color:#6B7280;">Order ID:</span>
                      <span style="color:#E8EAF0;font-family:monospace;">%s</span>
                    </div>
                    <div style="display:flex;justify-content:space-between;margin-bottom:12px;">
                      <span style="color:#6B7280;">Status:</span>
                      <span style="color:#FF4B2B;">Refunded</span>
                    </div>
                    <hr style="border:none;border-top:1px dashed #2A344A;margin:16px 0;">
                    <div style="display:flex;justify-content:space-between;">
                      <span style="color:#E8EAF0;font-weight:bold;">Amount Refunded:</span>
                      <span style="color:#FF4B2B;font-weight:bold;font-size:18px;">%s %s</span>
                    </div>
                  </div>
                  
                  <p style="color:#6B7280;font-size:14px;line-height:1.6;">The amount should appear in your bank account within 5-7 business days depending on your bank's processing time.</p>
                  <hr style="border:none;border-top:1px solid #1E2535;margin:24px 0;">
                  <p style="color:#6B7280;font-size:12px;text-align:center;">© 2026 ConnectSphere. All rights reserved.</p>
                </div>
              </div>
            </body>
            </html>
            """.formatted(name, orderId, amount, currency);
    }
}
