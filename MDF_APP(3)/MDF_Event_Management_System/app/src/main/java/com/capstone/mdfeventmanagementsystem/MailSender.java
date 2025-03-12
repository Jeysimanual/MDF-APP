package com.capstone.mdfeventmanagementsystem;

import android.util.Log;

import java.util.Properties;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class MailSender {

    private final String senderEmail = "gracevillanueva001@gmail.com"; // Replace with your email
    private final String senderPassword = "eeox zfpe ildl icxy"; // Use App Password

    public void sendEmail(String recipientEmail, String subject, String messageBody) throws MessagingException {
        Log.d("TestApp", "Initializing email sending process...");

        // Configure SMTP properties
        Properties properties = new Properties();
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");
        properties.put("mail.smtp.host", "smtp.gmail.com");
        properties.put("mail.smtp.port", "587");
        properties.put("mail.smtp.ssl.trust", "smtp.gmail.com");

        // Create a session with authentication
        Session session = Session.getInstance(properties, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                Log.d("TestApp", "Authenticating sender email...");
                return new PasswordAuthentication(senderEmail, senderPassword);
            }
        });

        try {
            // Create email message
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(senderEmail));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail));
            message.setSubject(subject);
            message.setText(messageBody);

            Log.d("TestApp", "Sending email to: " + recipientEmail);

            // Send email
            Transport.send(message);

            Log.d("TestApp", "Email sent successfully to " + recipientEmail);
        } catch (MessagingException e) {
            Log.e("TestApp", "Error sending email: " + e.getMessage(), e);
            throw e; // Rethrow exception to handle it where called
        }
    }
}
