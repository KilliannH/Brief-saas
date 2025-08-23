package com.killiann.briefsaas.service;

import com.killiann.briefsaas.controller.StripeController;
import com.killiann.briefsaas.entity.User;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    private final JavaMailSender mailSender;
    private final MessageSource messageSource;

    @Value("${frontend.baseUrl}")
    private String frontendBaseUrl;

    public void sendValidationEmail(String to, UUID publicUuid, String code, String lang) {
        Locale locale = Locale.forLanguageTag(lang);
        String subject = messageSource.getMessage("mail.validation.subject", null, locale);

        String link = frontendBaseUrl + "/public/briefs/" + publicUuid;
        String bodyHtml = messageSource.getMessage("mail.validation.body.html", new Object[]{link, code}, locale);

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(bodyHtml, true); // true = HTML

            helper.setFrom("no-reply@brief-mate.com");

            mailSender.send(mimeMessage);

        } catch (MessagingException e) {
            throw new RuntimeException("Erreur lors de l'envoi de l'email de validation", e);
        }
    }

    public void sendVerificationEmail(User user, String token) {
        Locale locale = Locale.forLanguageTag(user.getLanguage()); // "fr", "en", etc.

        String subject = messageSource.getMessage("mail.verify.subject", null, locale);
        String link = frontendBaseUrl + "/verify?token=" + token;
        String content = messageSource.getMessage("mail.verify.body", new Object[]{link}, locale);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(user.getEmail());
            helper.setSubject(subject);
            helper.setText(content, true); // HTML enabled

            helper.setFrom("no-reply@brief-mate.com");

            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send verification email", e);
        }
    }
}