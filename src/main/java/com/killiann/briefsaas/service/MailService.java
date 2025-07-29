package com.killiann.briefsaas.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender mailSender;

    public void sendValidationEmail(String to, UUID publicUuid, String code) {
        String subject = "[BriefMate] Validez le brief de votre projet";
        String link = "https://brief-mate.com/public/briefs/" + publicUuid;

        String content = """
        Bonjour,

        Vous avez reçu un brief à valider via BriefMate.

        Cliquez ici pour le consulter et le valider :
        %s

        Code de validation : %s

        ---
        BriefMate — Validez vos projets facilement, sans créer de compte.
        """.formatted(link, code);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(content);
        message.setFrom("no-reply@brief-mate.com");

        mailSender.send(message);
    }
}