package com.crm.rdvision.controller;

import com.avanse.core.exception.TechnicalException;
import com.crm.rdvision.Exception.BussinessException;
import com.crm.rdvision.dto.*;
import com.crm.rdvision.entity.*;
import com.crm.rdvision.repository.*;
import com.crm.rdvision.service.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.stripe.exception.StripeException;
import jakarta.mail.MessagingException;
import org.modelmapper.config.Configuration;
import org.modelmapper.convention.MatchingStrategies;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

import static org.hibernate.internal.CoreLogging.logger;

@RestController
@CrossOrigin
@RequestMapping("/indiamart/")
public class WebhookController {

    @Autowired
    private TicketRepo ticketRepo;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private SimpMessagingTemplate template;

    @Autowired
    private CallServiceHoduCC callServiceHoduCC;

    @Autowired
    private AutoAssignService autoAssignService;
    @Autowired
    private EmailService emailService;
    @Autowired
    private TicketUpdateHistoryRepo ticketUpdateHistoryRepo;
    @Autowired
    UserRepo userRepo;
    @Autowired
    private GoogleSheetsDataService googleSheetsDataService;
    @Autowired
    ProductRepo productRepo;
    @Autowired
    StripeService stripeService;
    @Autowired
    CustomerRepository customerRepository;

    private int lastAssignedUser = -1;
    private static final Logger logger = LoggerFactory.getLogger(WebhookController.class);

    @PostMapping("mRywE7xq5nfFSfep5n2C7liLo1rMlDZkXA==")
    public ResponseEntity<ResponseWebHook> handleWebhook(@RequestBody String rawData) throws IOException {
        logger.info("handleWebhook - Received a webhook call with raw data: {}", rawData);

        ResponseWebHook response;
        ObjectMapper objectMapper = new ObjectMapper();
        ReceivedData data;

        try {
            data = objectMapper.readValue(rawData, ReceivedData.class);
            logger.info("handleWebhook - Successfully deserialized raw data into ReceivedData object: {}", data);
        } catch (Exception e) {
            logger.error("handleWebhook - Error deserializing JSON: {}", e.getMessage());
            response = new ResponseWebHook(400, "Invalid JSON data");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        if (data == null || data.getCode() == null) {
            logger.warn("handleWebhook - Invalid data: Code is missing");
            response = new ResponseWebHook(400, "Invalid data: Code is missing");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        try {
            switch (data.getCode()) {
                case "200":
                    logger.info("handleWebhook - Processing with code 200");
                    int userId = 0;
                    if (data.getResponse() == null) {
                        logger.warn("handleWebhook - Invalid data: Response is missing for code 200");
                        response = new ResponseWebHook(400, "Invalid data: Response is missing for code 200");
                        break;
                    }

                    ResponseData responseData = data.getResponse();

                    modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT).setFieldMatchingEnabled(true).setFieldAccessLevel(Configuration.AccessLevel.PRIVATE);
                    TicketEntity ticketEntity = modelMapper.map(responseData, TicketEntity.class);
                    Optional<Customers> customers =customerRepository.findByCustomerEmailOrCustomerMobile(ticketEntity.getSenderEmail(),ticketEntity.getSenderMobile());
                    if(customers.isPresent()){
                        customers.get().setCustomerType("Existing");
                    }else{
                        Customers customers1=new Customers();
                        customers1.setCustomerEmail(ticketEntity.getSenderEmail());
                        customers1.setCustomerMobile(ticketEntity.getSenderMobile());
                        customers1.setCustomerName(ticketEntity.getSenderName());
                        customers1.setCountry(ticketEntity.getSenderCountryIso());
                        customers1.setCustomerType("New");
                        customers1.setTicketId(ticketEntity.getUniqueQueryId());
                        customerRepository.save(customers1);
                    }
                   if(!Objects.equals(ticketEntity.getSenderCountryIso(), "IN")){
                    ticketEntity.setTicketstatus("New");
                    TicketEntity ticket = ticketRepo.findByUniqueQueryId(ticketEntity.getUniqueQueryId());

                    if (ticket == null) {
                        ticketRepo.save(ticketEntity);
                    } else {
                        logger.info("Ticket is already Present");
                    }
                    if (ticket == null) {
                        exportToSheets(ticketEntity);
                    }
                    logger.info("handleWebhook - TicketEntity saved with ID: {}", ticketEntity.getId());
                    template.convertAndSend("/topic/third_party_api/ticket/", ticketEntity);
                   }else{
                       logger.info("handleWebhook - TicketEntity Rejected because it was an indian ticket with ID: {}", ticketEntity.getId());
                   }
                    // Auto Assigning to Closers
                    if (!Objects.equals(ticketEntity.getSenderCountryIso(), "IN")) {
                        try {
                            userId = autoAssignService.assignTicket(ticketEntity);
                            logger.info("handleWebhook - Ticket assigned to closer: {}", ticketEntity.getId());
                        } catch (Exception e) {
                            logger.error("handleWebhook - Failed to assign ticket: {}", ticketEntity.getId(), e);
                        }
                    }

                    if (ticketEntity.getSenderCountryIso() != "IN") {
                        try {
                            showInquiryConfirmation(ticketEntity);
                        } catch (Exception d) {
                            // Log the exception or handle it in some way
                            System.err.println("Error occurred during autoCall: " + d.getMessage());
                            d.printStackTrace(); // Optional, prints the full stack trace for debugging
                        } catch (TechnicalException e) {
                            throw new RuntimeException(e);
                        } catch (BussinessException e) {
                            throw new RuntimeException(e);
                        }
                    }

                    //call
                    if (!Objects.equals(ticketEntity.getSenderCountryIso(), "IN")) {
                        try {
                            Map<String, String> numberDetails = new HashMap<>();
                            numberDetails.put("number", ticketEntity.getSenderMobile().replaceAll("[+-]", ""));
                            if (userId == 0) {
                                List<User> users = userRepo.findByRoleId(4);
                                int numberOfUsers = users.size();
                                lastAssignedUser = (lastAssignedUser + 1) % numberOfUsers;
                                numberDetails.put("userId", users.get(lastAssignedUser).getUserId() + "");
                            } else {
                                numberDetails.put("userId", userId + "");
                            }
                            ResponseEntity<String> responseEntity = callServiceHoduCC.clickToCall(numberDetails, ticketEntity);
                            System.out.println(responseEntity.toString());
                            String jsonResponse = responseEntity.getBody();

                            try {
                                JsonNode rootNode = objectMapper.readTree(jsonResponse);
                                String statusCode = rootNode.path("code").asText();
                                if (statusCode.equals("200")) {
                                    String call_id = rootNode.path("call_id").asText();

                                    try {
                                        TicketStatusUpdateHistory ticketStatusUpdateHistory = new TicketStatusUpdateHistory();
                                        ticketStatusUpdateHistory.setUpdatedBy(0);
                                        ticketStatusUpdateHistory.setUpdateTime(LocalTime.now());
                                        ticketStatusUpdateHistory.setUpdateDate(LocalDate.now());
                                        ticketStatusUpdateHistory.setStatus("New");
                                        ticketStatusUpdateHistory.setTicketIdWhichUpdating(ticketEntity.getUniqueQueryId());
                                        ticketStatusUpdateHistory.setComment("New");
                                        ticketStatusUpdateHistory.setUserName("Auto call");

                                        if (!call_id.equals("0")) {
                                            try {
                                                String recordingString = callServiceHoduCC.getRecording(call_id);
                                                String[] parts = recordingString.split(":");
                                                if (parts.length >= 3) {
                                                    String recordingFile = parts[1] + ":" + parts[2].replace("}", "");
                                                    ticketStatusUpdateHistory.setRecordingFile(recordingFile);
                                                } else {
                                                    System.out.println("Unexpected format in recordingString: " + recordingString);
                                                }
                                            } catch (Exception e) {
                                                e.printStackTrace();
                                                System.out.println("Error while fetching or processing recording for call_id: " + call_id);
                                            }
                                        }
                                        ticketUpdateHistoryRepo.save(ticketStatusUpdateHistory);
                                        logger.info("Ticket history updated successfully for ticketId: {}", ticketEntity.getUniqueQueryId());
                                    } catch (Exception e) {
                                        logger.error("Error updating ticket history for ticketId: {}", ticketEntity.getUniqueQueryId(), e);
                                    }
                                } else {
                                    System.out.println("Response data is " + rootNode.toString());
                                }

                            } catch (Exception e) {
                                TicketStatusUpdateHistory ticketStatusUpdateHistory = new TicketStatusUpdateHistory();
                                ticketStatusUpdateHistory.setUpdatedBy(0);
                                ticketStatusUpdateHistory.setUpdateTime(LocalTime.now());
                                ticketStatusUpdateHistory.setUpdateDate(LocalDate.now());
                                ticketStatusUpdateHistory.setStatus("New");
                                ticketStatusUpdateHistory.setTicketIdWhichUpdating(ticketEntity.getUniqueQueryId());
                                ticketStatusUpdateHistory.setComment("Auto call failed");
                                ticketStatusUpdateHistory.setUserName("Auto call");
                                try {
//                                autoCall(ticketEntity);
                                } catch (Exception c) {
                                    // Log the exception or handle it in some way
                                    System.err.println("Error occurred during autoCall: " + c.getMessage());
                                    e.printStackTrace(); // Optional, prints the full stack trace for debugging
                                }

                                ticketUpdateHistoryRepo.save(ticketStatusUpdateHistory);
                                logger.error("Error processing the JSON response:");
                            }
                        } catch (Exception e) {
                            logger.error("Error during call setup or response handling", e);
                        }
                    }


                    // WebSocket notification

                    logger.info("handleWebhook - WebSocket message sent for ticket ID: {}", ticketEntity.getId());

                    response = new ResponseWebHook(200, "Success");
                    logger.info("handleWebhook - Code 200: Your request is successfully completed");
                    break;

                case "400":
                    logger.error("handleWebhook - Error 400: Missing parameters");
                    response = new ResponseWebHook(400, "Missing parameters");
                    break;

                case "500":
                    logger.error("handleWebhook - Error 500: Error in connecting to the URL");
                    response = new ResponseWebHook(500, "Error in connecting to the URL");
                    break;

                default:
                    logger.error("handleWebhook - Error 500: Unknown error");
                    response = new ResponseWebHook(500, "Unknown error");
                    break;
            }
        } catch (Exception e) {
            logger.error("handleWebhook - Internal Server Error: {}", e.getMessage(), e);
            response = new ResponseWebHook(500, "Internal Server Error");
        }

        logger.info("handleWebhook - Completed processing with response code: {}", response.getCode());
        return new ResponseEntity<>(response, HttpStatus.valueOf(response.getCode()));
    }

    public void showInquiryConfirmation(TicketEntity ticketEntity) throws MessagingException, com.avanse.core.exception.TechnicalException, BussinessException {
        // Prepare dynamic data
        Map<String, Object> model = new HashMap<>();
        String customerName = ticketEntity.getSenderName();
        String customerEmail = ticketEntity.getSenderEmail();
        String customerAddress = ticketEntity.getSenderAddress();
        String inquirySubject = ticketEntity.getSubject();
        String productName = ticketEntity.getQueryProductName();

        List<String> gallery = Arrays.asList("https://1drv.ms/i/s!AqitzNn5GW7cb3xDT057LNjP4k8?embed=1&width=240&height=210", "https://1drv.ms/i/s!AqitzNn5GW7cdM__mGbzfWUhLBk?embed=1&width=262&height=243", "https://1drv.ms/i/s!AqitzNn5GW7ceizi7Kxox9sqjrs?embed=1&width=360&height=360", "https://1drv.ms/i/s!AqitzNn5GW7cdr5A6Y9JDziM0Vc?embed=1&width=227&height=222", "https://1drv.ms/i/s!AqitzNn5GW7cd8f2sYtCamWRCZI?embed=1&width=228&height=222", "https://1drv.ms/i/s!AqitzNn5GW7ceE7VaJEj0S0MEzU?embed=1&width=1300&height=1390", "https://1drv.ms/i/s!AqitzNn5GW7ceaErIxOWmrZSajc?embed=1&width=626&height=512");

        // Add data to the model
        model.put("customerName", customerName);
        model.put("customerEmail", customerEmail);
        model.put("customerAddress", customerAddress);
        model.put("inquirySubject", inquirySubject);
        model.put("productName", productName);
        model.put("gallery", gallery);
        System.out.println("Sending enquiry mail");
        String modifiedInput = ticketEntity.getSubject().replaceFirst("Requirement for ", "");
        List<Product> products = productRepo.findByNameContainsIgnoreCase(modifiedInput);
        if (products.isEmpty()) {
            Product product = new Product();
            product.setName(modifiedInput);
            productRepo.save(product);
            emailService.sendEnquiryEmail(ticketEntity.getSenderEmail(), "Inquiry Email", model);
        } else {
            if (!products.get(0).getImageListInByte().isEmpty()) {

//    emailService.sendEnquiryAutoEmail(ticketEntity.getSenderEmail(),ticketEntity.getSenderName(),ticketEntity.getSenderAddress(),ticketEntity.getSenderMobile(),ticketEntity.getSenderEmail(),ticketEntity.getSubject(),products,"Your Enquiry Product",3,ticketEntity.getUniqueQueryId());
                sendAutoEnquiryMail(products.get(0), ticketEntity);

            }
        }

    }

    public void sendAutoEnquiryMail(Product product, TicketEntity ticket) throws com.avanse.core.exception.TechnicalException, BussinessException {
        List<Map<String, String>> priceList = product.getPriceList().stream().map(price -> {
            try {
                return Map.of("quantity", String.valueOf(price.getQuantity()), "amount", String.valueOf(price.getPrice()), "unit", price.getUnit(), "paymentLink", stripeService.createPaymentLink((long) price.getPrice() * 100, price.getCurrency(), price.getProductCode(), ticket.getUniqueQueryId(), product.getName()), "currency", price.getCurrency());
            } catch (StripeException e) {
                throw new RuntimeException(e);
            }
        }).toList();

        Map<String, Object> templateModel = new HashMap<>();
        Map<String, String> product1 = new HashMap<>();
        product1.put("name", product.getName());
        product1.put("strength", product.getStrength());
        product1.put("size", product.getPackagingSize());
        product1.put("brand", product.getBrand());
        product1.put("composition", product.getComposition());
        product1.put("treatment", product.getTreatment());
        product1.put("form", product.getPackagingType());
        product1.put("image","https://rdvision.in/images/image/"+product.getImageListInByte().get(0).getImageId());
        templateModel.put("company", Map.of("name", "Buymed24", "address", "Varanasi India", "contact", "+1-234-567-890", "email", "invoice@buymed24.com", "customer", ticket.getSenderName(), "senderEmail", ticket.getSenderEmail()));
        templateModel.put("product", product1);
        templateModel.put("priceList", priceList);
        Map<String,String> tempImages =new HashMap<>();
        tempImages.put("logo","https://rdvision.in/images/getTempImage/2");
        tempImages.put("topBanner","https://rdvision.in/images/getTempImage/3");
        tempImages.put("testimonial","https://rdvision.in/images/getTempImage/4");
        tempImages.put("m1","https://rdvision.in/images/getTempImage/5");
        tempImages.put("m2","https://rdvision.in/images/getTempImage/6");
        tempImages.put("m3","https://rdvision.in/images/getTempImage/7");
        tempImages.put("m4","https://rdvision.in/images/getTempImage/8");
        tempImages.put("m5","https://rdvision.in/images/getTempImage/9");
        tempImages.put("m6","https://rdvision.in/images/getTempImage/10");
        tempImages.put("tracking","https://rdvision.in/images/getTempImage/11");
        tempImages.put("review","https://rdvision.in/images/getTempImage/12");
        templateModel.put("images",tempImages);

        // Send the email

        String subject = "Your Enquiry from Buymed24.com";
        emailService.sendNewEnquiry(ticket.getSenderEmail(), subject, templateModel);

    }

    public String exportToSheets(TicketEntity ticketEntity) throws GeneralSecurityException, IOException {
        try {
            List<List<Object>> data = Arrays.asList(Arrays.asList("Name", "Age", "City"), Arrays.asList("John Doe", 29, "New York"), Arrays.asList("Jane Smith", 34, "San Francisco"));
            googleSheetsDataService.writeDataToSheet(ticketEntity);
            return "Data written to Google Sheets successfully.";
        } catch (IOException | GeneralSecurityException e) {
            e.printStackTrace();
            return "Error writing to Google Sheets.";
        }

    }
}
