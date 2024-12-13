package com.crm.rdvision.controller;

import com.crm.rdvision.entity.Invoice;
import com.crm.rdvision.entity.OrderItem;
import com.crm.rdvision.entity.TicketEntity;
import com.crm.rdvision.entity.UploadTicket;
import com.crm.rdvision.repository.InvoiceRepo;
import com.crm.rdvision.repository.OrderRepo;
import com.crm.rdvision.repository.TicketRepo;
import com.crm.rdvision.repository.UploadTicketRepo;
import com.crm.rdvision.service.EmailService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.mail.MessagingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/track17")
public class TrackingWebhook {
    @Autowired
    OrderRepo orderRepo;
    @Autowired
    InvoiceRepo invoiceRepo;
    @Autowired
    TicketRepo ticketRepo;
    @Autowired
    EmailService emailService;
    @Autowired
    UploadTicketRepo uploadTicketRepo;


    private static final Logger logger = LoggerFactory.getLogger(TrackingWebhook.class);
    private final ObjectMapper objectMapper = new ObjectMapper(); // Jackson ObjectMapper to convert to JSON

    @PostMapping("/tracking")
    public void getTracking(@RequestBody Map<String, Object> payload) {
        try {
            // Convert the payload map to a formatted JSON string
            String jsonPayload = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(payload);

            // Extract data from the payload
            Map<String, Object> data = (Map<String, Object>) payload.get("data");
            String trackingNumber = (String) data.get("number");

            Map<String, Object> trackInfo = (Map<String, Object>) data.get("track_info");
            Map<String, Object> latestStatus = (Map<String, Object>) trackInfo.get("latest_status");
            Map<String, Object> latestEvent = (Map<String, Object>) trackInfo.get("latest_event");
            Map<String, Object> timeMetrics = (Map<String, Object>) trackInfo.get("time_metrics");
            Map<String, Object> miscInfo = (Map<String, Object>) trackInfo.get("misc_info");

            // Retrieve latest status
            String status = (String) latestStatus.get("status");
            String subStatus = (String) latestStatus.get("sub_status");

            // Retrieve latest event details
            String latestEventDescription = (String) latestEvent.get("description");
            String latestEventLocation = (String) latestEvent.get("location");
            String latestEventTime = (String) latestEvent.get("time_iso");

            // Retrieve estimated delivery date (if available)
            Map<String, Object> estimatedDelivery = (Map<String, Object>) timeMetrics.get("estimated_delivery_date");
            String estimatedDeliveryFrom = (String) estimatedDelivery.get("from");
            String estimatedDeliveryTo = (String) estimatedDelivery.get("to");

            // Retrieve service and weight information
            String serviceType = (String) miscInfo.get("service_type");
            String weight = (String) miscInfo.get("weight_kg");

            // Retrieve shipping information
            Map<String, Object> shipInfo = (Map<String, Object>) trackInfo.get("shipping_info");
            Map<String, Object> shipperAddress = (Map<String, Object>) shipInfo.get("shipper_address");
            if (shipperAddress != null) {
                String shipperStreet = (String) shipperAddress.get("street");
                String shipperCity = (String) shipperAddress.get("city");
                String shipperState = (String) shipperAddress.get("state");
                String shipperCountry = (String) shipperAddress.get("country");
                String shipperPostalCode = (String) shipperAddress.get("postal_code");

                logger.info("Shipper Address: {}, {}, {}, {}, {}",
                        shipperStreet, shipperCity, shipperState, shipperPostalCode, shipperCountry);
            }

            // Retrieve recipient information
            Map<String, Object> recipientAddress = (Map<String, Object>) shipInfo.get("recipient_address");
            if (recipientAddress != null) {
                String recipientStreet = (String) recipientAddress.get("street");
                String recipientCity = (String) recipientAddress.get("city");
                String recipientState = (String) recipientAddress.get("state");
                String recipientCountry = (String) recipientAddress.get("country");
                String recipientPostalCode = (String) recipientAddress.get("postal_code");

                logger.info("Recipient Address: {}, {}, {}, {}, {}",
                        recipientStreet, recipientCity, recipientState, recipientPostalCode, recipientCountry);
            }
            updateStatusOfOrder(trackingNumber, status, serviceType, weight, estimatedDelivery.toString());
            // Log all the retrieved data
            logger.info("Tracking Number: {}", trackingNumber);
            logger.info("Latest Status: {} (Sub Status: {})", status, subStatus);
            logger.info("Latest Event: {} at {} on {}", latestEventDescription, latestEventLocation, latestEventTime);
            logger.info("Service Type: {}, Weight: {}", serviceType, weight);
            logger.info("Estimated Delivery: From {} To {}", estimatedDeliveryFrom, estimatedDeliveryTo);

        } catch (Exception e) {
            logger.error("Error processing tracking information", e);
        }
    }

    public void updateStatusOfOrder(String trackingNumber, String status, String serviceType, String weight, String estimatedDelivery) throws MessagingException {
        System.out.println("Called by webhook");
        Invoice invoice = invoiceRepo.findByTrackingNumber(trackingNumber);
        OrderItem orderItem = orderRepo.findByTrackingNumber(trackingNumber);
        TicketEntity ticketEntity = ticketRepo.findByTrackingNumber(trackingNumber);
        if (invoice == null) {
            TicketEntity ticket = ticketRepo.findByTrackingNumber(trackingNumber);
            if (ticket == null) {

                UploadTicket uploadTicket = uploadTicketRepo.findByTrackingNumber(trackingNumber);
                uploadTicket.setDeliveryStatus(status);
                uploadTicketRepo.save(uploadTicket);
            } else {
                ticket.setDeliveryStatus(status);
                ticket.setLastActionDate(LocalDate.now());
                ticketRepo.save(ticket);
            }
        } else {
            System.out.println("Invoice Found");
            orderItem.setOrderStatus(status);
            orderRepo.save(orderItem);
            invoice.setDeliveryStatus(status);
            invoice.setLastupdateDate(LocalDate.now());
            invoiceRepo.save(invoice);
        }
        if (ticketEntity != null) {
            emailService.sendDeliveryStatusEmail(ticketEntity.getSenderName(), ticketEntity.getSenderEmail(), trackingNumber, status, serviceType, weight, estimatedDelivery);
        } else {
            UploadTicket uploadTicket = uploadTicketRepo.findByTrackingNumber(trackingNumber);
            emailService.sendDeliveryStatusEmail(uploadTicket.getFirstName(), uploadTicket.getEmail(), trackingNumber, status, serviceType, weight, estimatedDelivery);
        }
    }


}
