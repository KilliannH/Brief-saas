package com.killiann.briefsaas.util;

import com.itextpdf.kernel.events.Event;
import com.itextpdf.kernel.events.IEventHandler;
import com.itextpdf.kernel.events.PdfDocumentEvent;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.Canvas;
import com.itextpdf.kernel.colors.ColorConstants;
import java.time.Year;

public class FooterHandler implements IEventHandler {
    @Override
    public void handleEvent(Event event) {
        PdfDocumentEvent docEvent = (PdfDocumentEvent) event;
        PdfDocument pdfDoc = docEvent.getDocument();
        PdfPage page = docEvent.getPage();
        Rectangle pageSize = page.getPageSize();

        PdfCanvas pdfCanvas = new PdfCanvas(page.newContentStreamAfter(), page.getResources(), pdfDoc);

        Canvas canvas = new Canvas(pdfCanvas, pageSize);
        canvas.setFontSize(10);
        canvas.setFontColor(ColorConstants.GRAY);
        canvas.showTextAligned(
                new Paragraph("Brief généré avec BriefMate — © " + Year.now()),
                pageSize.getWidth() / 2,
                pageSize.getBottom() + 20,
                com.itextpdf.layout.properties.TextAlignment.CENTER
        );
        canvas.close();
    }
}