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
        String subject = "Votre brief à valider";
        String link = "https://tonsite.com/public/briefs/" + publicUuid;

        String content = """
                Bonjour,

                Voici votre brief à valider : %s

                Code de validation : %s

                Merci !
                """.formatted(link, code);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(content);

        mailSender.send(message);
    }
}