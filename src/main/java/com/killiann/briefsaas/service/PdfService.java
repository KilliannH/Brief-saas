package com.killiann.briefsaas.service;

import com.itextpdf.kernel.colors.Color;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.events.PdfDocumentEvent;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.*;
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

    // Couleurs personnalisées
    private static final Color BRAND_BLUE = new DeviceRgb(59, 130, 246); // #3b82f6
    private static final Color LIGHT_BLUE = new DeviceRgb(239, 246, 255); // #eff6ff
    private static final Color DARK_GRAY = new DeviceRgb(55, 65, 81); // #374151
    private static final Color MEDIUM_GRAY = new DeviceRgb(107, 114, 128); // #6b7280
    private static final Color LIGHT_GRAY = new DeviceRgb(249, 250, 251); // #f9fafb
    private static final Color SUCCESS_GREEN = new DeviceRgb(34, 197, 94); // #22c55e
    private static final Color ACCENT_ORANGE = new DeviceRgb(251, 146, 60); // #fb923c

    public byte[] generateBriefPdf(Brief brief, Locale locale) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdf = new PdfDocument(writer);

        // Configuration des marges plus élégantes
        Document document = new Document(pdf, PageSize.A4, false);
        document.setMargins(40, 40, 60, 40);

        try {
            // Polices
            PdfFont regularFont = PdfFontFactory.createFont();
            PdfFont boldFont = PdfFontFactory.createFont();

            // === HEADER AVEC LOGO ET BRANDING ===
            addHeader(document, locale);

            // === TITRE PRINCIPAL DU BRIEF ===
            addTitle(document, brief.getTitle());

            // === INFORMATIONS GÉNÉRALES DANS UN TABLEAU ===
            addGeneralInfo(document, brief, locale);

            // === DESCRIPTION ===
            if (brief.getDescription() != null && !brief.getDescription().isBlank()) {
                addSection(document, messageSource.getMessage("pdf.description", null, locale),
                        brief.getDescription(), BRAND_BLUE);
            }

            // === OBJECTIFS ===
            if (brief.getObjectives() != null && !brief.getObjectives().isEmpty()) {
                addListSection(document, messageSource.getMessage("pdf.objectives", null, locale),
                        brief.getObjectives(), BRAND_BLUE);
            }

            // === LIVRABLES ===
            if (brief.getDeliverables() != null && !brief.getDeliverables().isEmpty()) {
                addListSection(document, messageSource.getMessage("pdf.deliverables", null, locale),
                        brief.getDeliverables(), ACCENT_ORANGE);
            }

            // === CONTRAINTES ===
            if (brief.getConstraints() != null && !brief.getConstraints().isBlank()) {
                addSection(document, messageSource.getMessage("pdf.constraints", null, locale),
                        brief.getConstraints(), ColorConstants.RED);
            }

            // === VALIDATION CLIENT ===
            addValidationSection(document, brief, locale);

            // === FOOTER ===
            pdf.addEventHandler(PdfDocumentEvent.END_PAGE, new FooterHandler());

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            document.close();
        }

        return baos.toByteArray();
    }

    private void addHeader(Document document, Locale locale) {
        // Conteneur header avec fond coloré
        Div headerDiv = new Div()
                .setBackgroundColor(BRAND_BLUE)
                .setMargin(0)
                .setPadding(20)
                .setMarginBottom(30);

        // Logo et nom de l'application
        Table headerTable = new Table(2);
        headerTable.setWidth(UnitValue.createPercentValue(100));

        // Colonne gauche - Logo + BriefMate
        Div logoDiv = new Div();
        logoDiv.add(new Paragraph("📋")
                .setFontSize(24)
                .setMargin(0)
                .setPaddingBottom(5));
        logoDiv.add(new Paragraph("BriefMate")
                .setFontSize(20)
                .setBold()
                .setFontColor(ColorConstants.WHITE)
                .setMargin(0));

        Cell logoCell = new Cell()
                .add(logoDiv)
                .setBorder(Border.NO_BORDER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE);

        // Colonne droite - Date et info
        String currentDate = LocalDate.now()
                .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(locale));

        Div infoDiv = new Div();
        infoDiv.add(new Paragraph(messageSource.getMessage("pdf.generated", null, locale))
                .setFontSize(10)
                .setFontColor(new DeviceRgb(200, 220, 255))
                .setMargin(0));
        infoDiv.add(new Paragraph(currentDate)
                .setFontSize(12)
                .setFontColor(ColorConstants.WHITE)
                .setMargin(0)
                .setPaddingTop(2));

        Cell infoCell = new Cell()
                .add(infoDiv)
                .setBorder(Border.NO_BORDER)
                .setTextAlignment(TextAlignment.RIGHT)
                .setVerticalAlignment(VerticalAlignment.MIDDLE);

        headerTable.addCell(logoCell);
        headerTable.addCell(infoCell);

        headerDiv.add(headerTable);
        document.add(headerDiv);
    }

    private void addTitle(Document document, String title) {
        // Titre avec design moderne
        Div titleDiv = new Div()
                .setBackgroundColor(LIGHT_GRAY)
                .setPadding(20)
                .setMarginBottom(25)
                .setBorder(new SolidBorder(new DeviceRgb(229, 231, 235), 1));

        titleDiv.add(new Paragraph("BRIEF PROJET")
                .setFontSize(10)
                .setFontColor(MEDIUM_GRAY)
                .setBold()
                .setMargin(0)
                .setMarginBottom(5));

        titleDiv.add(new Paragraph(title)
                .setFontSize(24)
                .setBold()
                .setFontColor(DARK_GRAY)
                .setMargin(0));

        document.add(titleDiv);
    }

    private void addGeneralInfo(Document document, Brief brief, Locale locale) {
        // Tableau d'informations générales avec design moderne
        Table infoTable = new Table(2);
        infoTable.setWidth(UnitValue.createPercentValue(100));
        infoTable.setMarginBottom(25);

        // Style des cellules
        addInfoRow(infoTable, "👤 " + messageSource.getMessage("pdf.client", null, locale),
                brief.getClient().getName(), BRAND_BLUE);

        if (brief.getBudget() != null) {
            addInfoRow(infoTable, "💰 " + messageSource.getMessage("pdf.budget", null, locale),
                    brief.getBudget(), SUCCESS_GREEN);
        }

        if (brief.getDeadline() != null) {
            String formattedDeadline = brief.getDeadline()
                    .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale));
            addInfoRow(infoTable, "⏰ " + messageSource.getMessage("pdf.deadline", null, locale),
                    formattedDeadline, ACCENT_ORANGE);
        }

        if (brief.getTargetAudience() != null && !brief.getTargetAudience().isBlank()) {
            addInfoRow(infoTable, "🎯 " + messageSource.getMessage("pdf.audience", null, locale),
                    brief.getTargetAudience(), new DeviceRgb(168, 85, 247));
        }

        document.add(infoTable);
    }

    private void addInfoRow(Table table, String label, String value, Color accentColor) {
        // Cellule label avec icône
        Cell labelCell = new Cell()
                .add(new Paragraph(label)
                        .setFontSize(11)
                        .setBold()
                        .setFontColor(accentColor))
                .setBackgroundColor(LIGHT_GRAY)
                .setPadding(12)
                .setBorder(Border.NO_BORDER)
                .setWidth(UnitValue.createPercentValue(30));

        // Cellule valeur
        Cell valueCell = new Cell()
                .add(new Paragraph(value)
                        .setFontSize(11)
                        .setFontColor(DARK_GRAY))
                .setPadding(12)
                .setBorder(Border.NO_BORDER);

        table.addCell(labelCell);
        table.addCell(valueCell);
    }

    private void addSection(Document document, String title, String content, Color accentColor) {
        // Titre de section avec ligne colorée
        Div sectionDiv = new Div()
                .setMarginBottom(20);

        // Barre colorée + titre
        Table titleTable = new Table(new float[]{1, 20});
        titleTable.setWidth(UnitValue.createPercentValue(100));
        titleTable.setMarginBottom(15);

        Cell colorBar = new Cell()
                .setBackgroundColor(accentColor)
                .setBorder(Border.NO_BORDER)
                .setPadding(0)
                .setWidth(4);

        Cell titleCell = new Cell()
                .add(new Paragraph(title)
                        .setFontSize(16)
                        .setBold()
                        .setFontColor(DARK_GRAY)
                        .setMargin(0))
                .setBorder(Border.NO_BORDER)
                .setPaddingLeft(15)
                .setVerticalAlignment(VerticalAlignment.MIDDLE);

        titleTable.addCell(colorBar);
        titleTable.addCell(titleCell);

        // Contenu avec fond légèrement coloré
        Div contentDiv = new Div()
                .setBackgroundColor(LIGHT_BLUE)
                .setPadding(15)
                .setBorder(new SolidBorder(new DeviceRgb(219, 234, 254), 1));

        contentDiv.add(new Paragraph(content)
                .setFontSize(11)
                .setFontColor(DARK_GRAY)
                .setMargin(0)
                .setTextAlignment(TextAlignment.JUSTIFIED));

        sectionDiv.add(titleTable);
        sectionDiv.add(contentDiv);
        document.add(sectionDiv);
    }

    private void addListSection(Document document, String title, java.util.List<String> items, Color accentColor) {
        // Titre de section
        Table titleTable = new Table(new float[]{1, 20});
        titleTable.setWidth(UnitValue.createPercentValue(100));
        titleTable.setMarginBottom(15);

        Cell colorBar = new Cell()
                .setBackgroundColor(accentColor)
                .setBorder(Border.NO_BORDER)
                .setPadding(0)
                .setWidth(4);

        Cell titleCell = new Cell()
                .add(new Paragraph(title)
                        .setFontSize(16)
                        .setBold()
                        .setFontColor(DARK_GRAY)
                        .setMargin(0))
                .setBorder(Border.NO_BORDER)
                .setPaddingLeft(15)
                .setVerticalAlignment(VerticalAlignment.MIDDLE);

        titleTable.addCell(colorBar);
        titleTable.addCell(titleCell);
        document.add(titleTable);

        // Liste avec puces modernes
        List list = new List()
                .setMarginLeft(20)
                .setMarginBottom(20);

        for (String item : items) {
            ListItem listItem = (ListItem) new ListItem()
                    .add(new Paragraph(item)
                            .setFontSize(11)
                            .setFontColor(DARK_GRAY)
                            .setMargin(0))
                    .setMarginBottom(8);
            list.add(listItem);
        }

        // Conteneur avec fond
        Div listDiv = new Div()
                .setBackgroundColor(new DeviceRgb(254, 249, 195)) // bg-yellow-50
                .setPadding(15)
                .setBorder(new SolidBorder(new DeviceRgb(254, 240, 138), 1)); // border-yellow-200

        listDiv.add(list);
        document.add(listDiv);
    }

    private void addValidationSection(Document document, Brief brief, Locale locale) {
        if (brief.getClientValidated() != null && brief.getClientValidated()
                && brief.getValidatedAt() != null && brief.getClient().getName() != null) {

            DateTimeFormatter formatter = DateTimeFormatter
                    .ofLocalizedDate(FormatStyle.LONG)
                    .withLocale(locale);
            String formattedDate = brief.getValidatedAt().format(formatter);

            // Section validation avec design spécial
            Div validationDiv = new Div()
                    .setBackgroundColor(new DeviceRgb(240, 253, 244)) // bg-green-50
                    .setBorder(new SolidBorder(SUCCESS_GREEN, 2))
                    .setPadding(20)
                    .setMarginTop(30);

            // Header validation
            Table validationHeader = new Table(2);
            validationHeader.setWidth(UnitValue.createPercentValue(100));
            validationHeader.setMarginBottom(10);

            Cell iconCell = new Cell()
                    .add(new Paragraph("✅")
                            .setFontSize(20)
                            .setMargin(0))
                    .setBorder(Border.NO_BORDER)
                    .setWidth(30);

            Cell statusCell = new Cell()
                    .add(new Paragraph(messageSource.getMessage("pdf.validation.title", null, locale))
                            .setFontSize(14)
                            .setBold()
                            .setFontColor(SUCCESS_GREEN)
                            .setMargin(0))
                    .setBorder(Border.NO_BORDER);

            validationHeader.addCell(iconCell);
            validationHeader.addCell(statusCell);

            // Message de validation
            String validationText = messageSource.getMessage("pdf.validated", null, locale)
                    + " " + formattedDate + " "
                    + messageSource.getMessage("pdf.by", null, locale)
                    + " " + brief.getClient().getName() + ".";

            Paragraph validationMsg = new Paragraph(validationText)
                    .setFontSize(11)
                    .setFontColor(new DeviceRgb(21, 128, 61)) // text-green-700
                    .setMargin(0)
                    .setItalic();

            validationDiv.add(validationHeader);
            validationDiv.add(validationMsg);
            document.add(validationDiv);
        }
    }
}