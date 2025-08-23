package com.killiann.briefsaas.service;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.events.PdfDocumentEvent;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.properties.TextAlignment;
import com.killiann.briefsaas.entity.Brief;
import com.killiann.briefsaas.util.FooterHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;

@Service
public class PdfService {

    @Autowired
    private MessageSource messageSource;

    public byte[] generateBriefPdf(Brief brief, Locale locale) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);

        // Titre principal = titre du brief
        document.add(new Paragraph(brief.getTitle())
                .setFontSize(18)
                .setBold()
                .setMarginBottom(10));

        // Description
        if (brief.getDescription() != null) {
            document.add(new Paragraph(brief.getDescription()).setMarginBottom(10));
        }

        // Champs classiques
        document.add(new Paragraph(messageSource.getMessage("pdf.client", null, locale) + ": " + brief.getClient().getName()));
        document.add(new Paragraph(messageSource.getMessage("pdf.budget", null, locale) + ": " + brief.getBudget()));
        document.add(new Paragraph(messageSource.getMessage("pdf.deadline", null, locale) + ": " + brief.getDeadline()));
        document.add(new Paragraph(messageSource.getMessage("pdf.audience", null, locale) + ": " + brief.getTargetAudience()));

        // Objectifs avec puces
        if (brief.getObjectives() != null && !brief.getObjectives().isEmpty()) {
            document.add(new Paragraph(messageSource.getMessage("pdf.objectives", null, locale) + ":"));
            for (String obj : brief.getObjectives()) {
                document.add(new Paragraph("• " + obj).setMarginLeft(10));
            }
        }

        // Livrables avec puces
        if (brief.getDeliverables() != null && !brief.getDeliverables().isEmpty()) {
            document.add(new Paragraph(messageSource.getMessage("pdf.deliverables", null, locale) + ":"));
            for (String deliv : brief.getDeliverables()) {
                document.add(new Paragraph("• " + deliv).setMarginLeft(10));
            }
        }

        // Contraintes
        if (brief.getConstraints() != null && !brief.getConstraints().isBlank()) {
            document.add(new Paragraph(messageSource.getMessage("pdf.constraints", null, locale) + ": " + brief.getConstraints()));
        }

        // Validation par le client (si applicable)
        if (brief.getClientValidated() != null
                && brief.getClientValidated()
                && brief.getValidatedAt() != null
                && brief.getClient().getName() != null) {

            DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(locale);
            String formattedDate = brief.getValidatedAt().format(formatter);

            String validatedLine = messageSource.getMessage("pdf.validated", null, locale)
                    + " " + formattedDate + " "
                    + messageSource.getMessage("pdf.by", null, locale)
                    + " " + brief.getClient().getName() + ".";

            document.add(new Paragraph(validatedLine)
                    .setItalic()
                    .setFontColor(ColorConstants.DARK_GRAY)
                    .setMarginTop(20));
        }

        // Footer
        pdf.addEventHandler(PdfDocumentEvent.END_PAGE, new FooterHandler());
        document.close();
        return baos.toByteArray();
    }
}