package com.killiann.briefsaas.service;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.events.PdfDocumentEvent;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.killiann.briefsaas.entity.Brief;
import com.killiann.briefsaas.util.FooterHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Year;
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

        // Titre du brief
        document.add(new Paragraph(brief.getTitle())
                .setFontSize(18)
                .setBold()
                .setMarginBottom(10));

        // Description
        document.add(new Paragraph(brief.getDescription())
                .setItalic()
                .setFontSize(12)
                .setMarginBottom(15));

        // Champs simples
        document.add(new Paragraph(messageSource.getMessage("pdf.client", null, locale) + ": " + brief.getClientName()));
        document.add(new Paragraph(messageSource.getMessage("pdf.budget", null, locale) + ": " + brief.getBudget()));
        document.add(new Paragraph(messageSource.getMessage("pdf.deadline", null, locale) + ": " + brief.getDeadline()));
        document.add(new Paragraph(messageSource.getMessage("pdf.audience", null, locale) + ": " + brief.getTargetAudience()));
        document.add(new Paragraph(messageSource.getMessage("pdf.constraints", null, locale) + ": " + brief.getConstraints()).setMarginBottom(15));

        // Objectifs avec puces
        document.add(new Paragraph(messageSource.getMessage("pdf.objectives", null, locale) + " :").setBold());
        if (brief.getObjectives() != null) {
            for (String objective : brief.getObjectives()) {
                document.add(new Paragraph("• " + objective).setMarginLeft(10));
            }
        }

        // Livrables avec puces
        document.add(new Paragraph("\n" + messageSource.getMessage("pdf.deliverables", null, locale) + " :").setBold());
        if (brief.getDeliverables() != null) {
            for (String deliv : brief.getDeliverables()) {
                document.add(new Paragraph("• " + deliv).setMarginLeft(10));
            }
        }

        // Footer
        pdf.addEventHandler(PdfDocumentEvent.END_PAGE, new FooterHandler());
        document.close();
        return baos.toByteArray();
    }
}