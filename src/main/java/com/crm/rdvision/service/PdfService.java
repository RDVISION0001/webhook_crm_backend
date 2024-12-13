package com.crm.rdvision.service;

import com.crm.rdvision.dto.OrderDto;
import com.lowagie.text.DocumentException;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Date;

@Service
public class PdfService {

    private final SpringTemplateEngine templateEngine;

    public PdfService(SpringTemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    public byte[] generateInvoicePdf(OrderDto orderDto) throws DocumentException, IOException {
        // Set up the Thymeleaf context with the order details
        Context context = new Context();
        context.setVariable("productOrderId", orderDto.getProductId());
        context.setVariable("orderId", 1);
        context.setVariable("customerName", "sahani");
        context.setVariable("customerEmail", "hanusahani01@gmail.com");
        context.setVariable("customerAddress", "bhujana");
        context.setVariable("createdDate", new Date());
        context.setVariable("dueDate", new Date());
        context.setVariable("productOrders", orderDto.getProductOrders());
        context.setVariable("totalPayableAmount", orderDto.getTotalPayableAmount());

        // Render the HTML content
        String htmlContent = templateEngine.process("invoiceTemplate", context);

        // Convert HTML to PDF
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ITextRenderer renderer = new ITextRenderer();
        renderer.setDocumentFromString(htmlContent);
        renderer.layout();
        renderer.createPDF(outputStream);

        return outputStream.toByteArray();
    }
}
