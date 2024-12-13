package com.crm.rdvision.service;

import com.crm.rdvision.dto.EmailDto;
import com.crm.rdvision.dto.OrderDto;
import com.crm.rdvision.dto.ProductDto;
import com.crm.rdvision.dto.ProductOrderDto;
import com.crm.rdvision.entity.Address;
import com.crm.rdvision.entity.EmailTracking;
import com.crm.rdvision.entity.Product;
import com.crm.rdvision.entity.TicketEntity;
import com.crm.rdvision.repository.EmailTrackingRepo;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender otpMailSender;

    @Autowired
    private JavaMailSender enquiryMailSender;

    @Autowired
    private JavaMailSender invoiceMailSender;

    @Autowired
    private JavaMailSender trackingMailSender;

    @Autowired
    private SpringTemplateEngine templateEngine;

    @Autowired
    private ResourceLoader resourceLoader;

    @Autowired
    private EmailTrackingRepo emailTrackingRepo;

    public String sendOtp(String email, String subject, String messageText) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(email); // Recipient's email
            message.setSubject(subject);
            message.setText(messageText);
            message.setFrom("no-reply@rdvision.tech");
            otpMailSender.send(message);
            EmailTracking emailTracking = new EmailTracking();
            emailTracking.setRecipientEmail(email);
            emailTracking.setEmailType("otp");
            emailTracking.setSubject(subject);
            emailTracking.setStatus("Sent to Closer email for otp");
            emailTracking.setSentTime(LocalDateTime.now());
            emailTracking.setRecipientName("This is otp to closer");
            emailTracking.setTrackingId(LocalTime.now().toString());
            emailTrackingRepo.save(emailTracking);
            return "Email sent successfully";
        } catch (Exception e) {
            e.printStackTrace();
            return "Something went wrong: " + e.getMessage();
        }
    }


    public void sendOrderConfirmation(String to, String subject, Map<String, Object> templateModel) throws MessagingException {
        MimeMessage message = invoiceMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setTo(to);
        helper.setSubject(subject);
        // Adding CC email
        helper.setCc("cc@buymed24.com");
        helper.setText("order", true);
        Context context = new Context();
        context.setVariables(templateModel);
        String htmlContent = templateEngine.process("NewOrderConfirmation", context);  // Use the appropriate template name here
        helper.setText(htmlContent, true);
        helper.setFrom("invoice@buymed24.com");
        invoiceMailSender.send(message);
        EmailTracking emailTracking = new EmailTracking();
        emailTracking.setRecipientEmail(to);
        emailTracking.setEmailType("order");
        emailTracking.setSubject(subject);
        emailTracking.setStatus("Sent to customer email for order Confirmation");
        emailTracking.setSentTime(LocalDateTime.now());
        emailTracking.setRecipientName(to);
        emailTracking.setTrackingId(LocalTime.now().toString());
        emailTrackingRepo.save(emailTracking);
    }

    public void sendInvoiceWithPdf(String to, String subject, Map<String, Object> templateModel) throws MessagingException {
        MimeMessage message = invoiceMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setCc("cc@buymed24.com");
        Context context = new Context();
        context.setVariables(templateModel);
        String htmlContent = templateEngine.process("invoice", context);
        helper.setText(htmlContent, true);
        ByteArrayOutputStream pdfOutputStream = generatePdfFromHtml(htmlContent);
        ByteArrayResource pdfAttachment = new ByteArrayResource(pdfOutputStream.toByteArray());

        helper.addAttachment("invoice.pdf", pdfAttachment);
        helper.setFrom("invoice@buymed24.com");
        invoiceMailSender.send(message);
        EmailTracking emailTracking = new EmailTracking();
        emailTracking.setRecipientEmail(to);
        emailTracking.setEmailType("invoice");
        emailTracking.setSubject(subject);
        emailTracking.setStatus("Sent to Customer email for Invoice pdf");
        emailTracking.setSentTime(LocalDateTime.now());
        emailTracking.setRecipientName(to);
        emailTracking.setTrackingId(LocalTime.now().toString());
        emailTrackingRepo.save(emailTracking);
    }


    public void sendQuotationWithPdf(String to, String subject, Map<String, Object> templateModel) throws MessagingException {
        // Create the email
        MimeMessage message = invoiceMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
        helper.setTo(to);
        helper.setSubject(subject);
        // Adding CC email
        helper.setCc("cc@buymed24.com");
        // Process the Thymeleaf template to get HTML content
        Context context = new Context();
        context.setVariables(templateModel);
        String htmlContent = templateEngine.process("Quotation", context);

        // Set the email attribut
        helper.setText(htmlContent, true);

        // Generate PDF from the same HTML content and attach it to the email
        ByteArrayOutputStream pdfOutputStream = generatePdfFromHtml(htmlContent);
        ByteArrayResource pdfAttachment = new ByteArrayResource(pdfOutputStream.toByteArray());

        // Add the PDF attachment
        helper.addAttachment("Quotation.pdf", pdfAttachment);
        helper.setFrom("invoice@buymed24.com");
        // Send the email
        invoiceMailSender.send(message);
        EmailTracking emailTracking = new EmailTracking();
        emailTracking.setRecipientEmail(to);
        emailTracking.setEmailType("quotation");
        emailTracking.setSubject(subject);
        emailTracking.setStatus("Sent to Customer email for Quotation pdf");
        emailTracking.setSentTime(LocalDateTime.now());
        emailTracking.setRecipientName(to);
        emailTracking.setTrackingId(LocalTime.now().toString());
        emailTrackingRepo.save(emailTracking);    }

    private ByteArrayOutputStream generatePdfFromHtml(String htmlContent) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            ITextRenderer renderer = new ITextRenderer();
            renderer.setDocumentFromString(htmlContent);
            renderer.layout();
            renderer.createPDF(outputStream);
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return outputStream;
    }

    public void sendDeliveryStatusEmail(String senderName, String email, String trackingNumber, String latestStatus, String serviceType, String weight, String estimatedDelivery) throws MessagingException {
        String subject = "Delivery Status Update";
        // HTML body for the email
        StringBuilder body = new StringBuilder();
        body.append("<html>")
                .append("<body style='font-family: Arial, sans-serif; color: #333;'>")
                .append("<div style='text-align: center; margin-bottom: 20px;'>")
                .append("<h1 style='color: #000;'>YOUR DELIVERY STATUS UPDATE</h1>")
                .append("</div>")
                .append("<h2>Dear ").append(senderName).append(",</h2>")
                .append("<p>We are writing to inform you about the delivery status of your package.</p>")
                .append("<table style='border-collapse: collapse; width: 100%; margin: 20px 0;'>")
                .append("<tr>")
                .append("<th style='border: 1px solid #dddddd; text-align: left; padding: 8px;'>Tracking Number</th>")
                .append("<td style='border: 1px solid #dddddd; text-align: left; padding: 8px;'>").append(trackingNumber).append("</td>")
                .append("</tr>")
                .append("<tr>")
                .append("<th style='border: 1px solid #dddddd; text-align: left; padding: 8px;'>Latest Status</th>")
                .append("<td style='border: 1px solid #dddddd; text-align: left; padding: 8px;'>").append(latestStatus).append("</td>")
                .append("</tr>")
                .append("<tr>")
                .append("<th style='border: 1px solid #dddddd; text-align: left; padding: 8px;'>Service Type</th>")
                .append("<td style='border: 1px solid #dddddd; text-align: left; padding: 8px;'>").append(serviceType).append("</td>")
                .append("</tr>")
                .append("<tr>")
                .append("<th style='border: 1px solid #dddddd; text-align: left; padding: 8px;'>Weight</th>")
                .append("<td style='border: 1px solid #dddddd; text-align: left; padding: 8px;'>").append(weight).append(" lbs</td>")
                .append("</tr>")
                .append("<tr>")
                .append("<th style='border: 1px solid #dddddd; text-align: left; padding: 8px;'>Estimated Delivery</th>")
                .append("<td style='border: 1px solid #dddddd; text-align: left; padding: 8px;'>").append(estimatedDelivery).append("</td>")
                .append("</tr>")
                .append("</table>")
                .append("<p>Thank you for choosing our service!</p>")
                .append("<p>Best regards,<br>RDVISION Team</p>")
                .append("</body>")
                .append("</html>");

        // Create a MimeMessage for HTML
        MimeMessage message = trackingMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);

        try {
            helper.setCc("cc@buymed24.com");
            helper.setTo(email); // Send to the customer's email
            helper.setSubject(subject);
            helper.setText(body.toString(), true); // true indicates HTML
            helper.setFrom("tracking@buymed24.com");
            trackingMailSender.send(message);
            EmailTracking emailTracking = new EmailTracking();
            emailTracking.setRecipientEmail(email);
            emailTracking.setEmailType("delivery");
            emailTracking.setSubject(subject);
            emailTracking.setStatus(latestStatus);
            emailTracking.setSentTime(LocalDateTime.now());
            emailTracking.setRecipientName(senderName);
            emailTracking.setTrackingId(LocalTime.now().toString());
            emailTrackingRepo.save(emailTracking);        } catch (MessagingException e) {
            e.printStackTrace(); // Handle exception appropriately
        }
    }

    public void sendEnquiryEmailTest(String to, String customerName, String customerAddress,
                                     String customerMobile, String customerEmail, String customerEnquiry,
                                     List<Product> products, String text, int temp) throws MessagingException {
        System.out.println(text + "'" + to);

        // Prepare the model
        Map<String, Object> model = new HashMap<>();
        model.put("customerName", customerName);
        model.put("customerAddress", customerAddress);
        model.put("customerMobile", customerMobile);
        model.put("customerEmail", customerEmail);
        model.put("customerEnquiry", customerEnquiry);
        model.put("products", products);
        model.put("companyName", "BYMED24");
        model.put("companyAddress", "INDIA");
        model.put("companyEmail", "buymed24.com");
        model.put("companyPhone", "+92038929039");
        model.put("currentYear", java.time.Year.now().getValue());
        model.put("text", text);

        String htmlContent;
        try {
            // Load the HTML template
            htmlContent = templateEngine.process("Template" + temp, new Context(Locale.ENGLISH, model));
        } catch (Exception e) {
            throw new MessagingException("Failed to process email template", e);
        }

        // Create the email
        MimeMessage mimeMessage = invoiceMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
        helper.setTo(to);
        helper.setSubject("Your Enquiry");
        helper.setText(htmlContent, true); // true indicates HTML content
        helper.setFrom("invoice@buymed24.com");
        // Send the email
        invoiceMailSender.send(mimeMessage);
        EmailTracking emailTracking = new EmailTracking();
        emailTracking.setRecipientEmail(to);
        emailTracking.setEmailType("Auto Mail for enquiry");
        emailTracking.setSubject("Your Enquiry");
        emailTracking.setStatus("Sent to Customer email for Enquiry");
        emailTracking.setSentTime(LocalDateTime.now());
        emailTracking.setRecipientName((String) model.get("customerName"));
        emailTracking.setTrackingId(LocalTime.now().toString());
        emailTrackingRepo.save(emailTracking);
    }



    public void sendEnquiryEmail(String to, String subject, Map<String, Object> templateModel) throws MessagingException {
        MimeMessage message = enquiryMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setTo(to);
        // Adding CC email
        helper.setCc("cc@buymed24.com");
        helper.setSubject(subject);
        helper.setText("Enquiry", true);
        Context context = new Context();
        context.setVariables(templateModel);
        String htmlContent = templateEngine.process("inquiry", context);  // Use the appropriate template name here
        helper.setText(htmlContent, true);
        helper.setFrom("enquiry@buymed24.com");
        enquiryMailSender.send(message);
        EmailTracking emailTracking = new EmailTracking();
        emailTracking.setRecipientEmail(to);
        emailTracking.setEmailType("Auto Mail for enquiry");
        emailTracking.setSubject("Your Enquiry");
        emailTracking.setStatus("Sent to Customer email for Enquiry");
        emailTracking.setSentTime(LocalDateTime.now());
        emailTracking.setRecipientName(to);
        emailTracking.setTrackingId(LocalTime.now().toString());
        emailTrackingRepo.save(emailTracking);

    }


    private ByteArrayResource generateInvoicePdf(OrderDto orderDto) {
        Context context = new Context();
        context.setVariable("order", orderDto);

        String htmlContent = templateEngine.process("invoicePdfTemplate", context);

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            ITextRenderer renderer = new ITextRenderer();
            renderer.setDocumentFromString(htmlContent);
            renderer.layout();
            renderer.createPDF(outputStream);
            return new ByteArrayResource(outputStream.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate PDF", e);
        }
    }

    // Method to send follow-up email
    public void sendFollowUpEmail(String recipientEmail, String recipientName, LocalDateTime nextFollowUpDate) {
        try {
            MimeMessage message = enquiryMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setTo(recipientEmail);
            helper.setSubject("Your Next Follow-Up Date and Time");
            helper.setText(buildFollowUpEmailContent(recipientName, nextFollowUpDate), true);
            helper.setFrom("enquiry@buymed24.com");
            enquiryMailSender.send(message);
            EmailTracking emailTracking = new EmailTracking();
            emailTracking.setRecipientEmail(recipientEmail);
            emailTracking.setEmailType("Auto Mail for followup");
            emailTracking.setSubject("Your Enquiry");
            emailTracking.setStatus("Sent to Customer email for followup");
            emailTracking.setSentTime(LocalDateTime.now());
            emailTracking.setRecipientName(recipientName);
            emailTracking.setTrackingId(LocalTime.now().toString());
            emailTrackingRepo.save(emailTracking);
            System.out.println("Follow-up email sent successfully!");
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }

    // Method to build the HTML content for the follow-up email
    private String buildFollowUpEmailContent(String recipientName, LocalDateTime nextFollowUpDate) {
        // Format the date to display in a friendly manner
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");


        return "<html>" +
                "<body style=\"font-family: Arial, sans-serif; color: #333;\">" +
                "<h2>Dear " + recipientName + ",</h2>" +
                "<p>We hope you're doing well. We wanted to remind you about your next follow-up scheduled for:</p>" +
                "<p><strong>Date and Time:</strong> " + formatter.format(nextFollowUpDate) + "</p>" +
                "<p>Our team will be reaching out to you at this time. Please feel free to reach out to us if you have any questions or need to reschedule.</p>" +
                "<p>Looking forward to connecting with you again soon!</p>" +
                "<br/>" +
                "<p>Best regards,</p>" +
                "<p>The Buymed24.com Team</p>" +
                "<hr/>" +
                "<p style=\"font-size: 12px; color: #777;\">This email was sent from Buymed24. If you have any questions, feel free to contact us at support@buymed24.com.</p>" +
                "</body>" +
                "</html>";
    }
    //thank you mail for showing interest

    public void sendThankYouEmail(String recipientEmail, String recipientName, String productName) {
        try {
            MimeMessage message = enquiryMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setTo(recipientEmail);
            helper.setSubject("Thank You for Your Interest in " + productName);
            helper.setText(buildEmailContent(recipientName, productName), true);
            helper.setFrom("enquiry@buymed24.com");
            enquiryMailSender.send(message);
            EmailTracking emailTracking = new EmailTracking();
            emailTracking.setRecipientEmail(recipientEmail);
            emailTracking.setEmailType("Auto Mail for followup");
            emailTracking.setSubject("Your Enquiry");
            emailTracking.setStatus("Sent to Customer email for followup");
            emailTracking.setSentTime(LocalDateTime.now());
            emailTracking.setRecipientName(recipientName);
            emailTracking.setTrackingId(LocalTime.now().toString());
            emailTrackingRepo.save(emailTracking);
            System.out.println("Thank you email sent successfully!");
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }

    private String buildEmailContent(String recipientName, String productName) {
        return "<html>" +
                "<body style=\"font-family: Arial, sans-serif; color: #333;\">" +
                "<h2>Dear " + recipientName + ",</h2>" +
                "<p>Thank you for showing interest in our product, <strong>" + productName + "</strong>. We're excited to share more about how our product can meet your needs!</p>" +
                "<p>Our team is available to answer any questions you may have. Please don’t hesitate to reach out.</p>" +
                "<p>Meanwhile, here’s a quick overview of the benefits of <strong>" + productName + "</strong>:</p>" +
                "<ul>" +
                "<li>High-quality performance and reliability</li>" +
                "<li>Affordable pricing options</li>" +
                "<li>Exceptional customer support</li>" +
                "</ul>" +
                "<p>We look forward to connecting with you soon!</p>" +
                "<br/>" +
                "<p>Best regards,</p>" +
                "<p>The Buymed24 Team</p>" +
                "<hr/>" +
                "<p style=\"font-size: 12px; color: #777;\">This email was sent from Buymed24. If you have any questions, feel free to contact us at support@buymed24.com.</p>" +
                "</body>" +
                "</html>";
    }


    public void sendNewQuotation(String to, String subject, Map<String, Object> templateModel) {
        // Create a MimeMessage object
        MimeMessage mimeMessage = invoiceMailSender.createMimeMessage();

        try {
            MimeMessageHelper messageHelper = new MimeMessageHelper(mimeMessage, true);
            messageHelper.setTo(to);
            messageHelper.addCc("ravi@rdvision.tech");
            messageHelper.setSubject(subject);
            messageHelper.setFrom("invoice@buymed24.com");
            Context context = new Context();
            context.setVariables(templateModel);
            String htmlContent = templateEngine.process("NewQuotation", context);

            messageHelper.setText(htmlContent, true); // 'true' indicates HTML content
            EmailTracking emailTracking = new EmailTracking();
            emailTracking.setRecipientEmail(to);
            emailTracking.setEmailType("Auto Mail for Quotation");
            emailTracking.setSubject("quotation");
            emailTracking.setStatus("Sent to Customer email for followup");
            emailTracking.setSentTime(LocalDateTime.now());
            emailTracking.setRecipientName(to);
            emailTracking.setTrackingId(LocalTime.now().toString());
            emailTrackingRepo.save(emailTracking);
            invoiceMailSender.send(mimeMessage);

        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }

    public void sendNewEnquiry(String to, String subject, Map<String, Object> templateModel) {
        MimeMessage mimeMessage = invoiceMailSender.createMimeMessage();
        try {
            MimeMessageHelper messageHelper = new MimeMessageHelper(mimeMessage, true);
            messageHelper.setTo(to);
            messageHelper.addCc("ravi@rdvision.tech");
            messageHelper.setSubject(subject);
            messageHelper.setFrom("enquiry@buymed24.com");
            Context context = new Context();
            context.setVariables(templateModel);
            String htmlContent = templateEngine.process("NewEnquiryTemplate", context);

            messageHelper.setText(htmlContent, true); // 'true' indicates HTML content

            enquiryMailSender.send(mimeMessage);
            EmailTracking emailTracking = new EmailTracking();
            emailTracking.setRecipientEmail(to);
            emailTracking.setEmailType("Auto Mail for enquiry");
            emailTracking.setSubject(subject);
            emailTracking.setStatus("Sent to customer email and cc mail to");
            emailTracking.setSentTime(LocalDateTime.now());
            Map<String,String> ob= (Map<String, String>) templateModel.get("company");
            emailTracking.setRecipientName(ob.get("customer"));
            emailTracking.setTrackingId(to);
            emailTrackingRepo.save(emailTracking);
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }

}
