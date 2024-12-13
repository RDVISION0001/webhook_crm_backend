package com.crm.rdvision.controller;

import com.crm.rdvision.Exception.BussinessException;
import com.crm.rdvision.common.PaymentStatus;
import com.crm.rdvision.dto.InvoiceVerificationDto;
import com.crm.rdvision.dto.OrderDto;
import com.crm.rdvision.dto.ProductDto;
import com.crm.rdvision.dto.ProductOrderDto;
import com.crm.rdvision.entity.*;
import com.crm.rdvision.repository.*;
import com.crm.rdvision.service.*;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;
import org.modelmapper.ModelMapper;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.GeneralSecurityException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;


@RestController
@CrossOrigin
@RequestMapping("/invoice/")
public class InvoiceController {

    @Autowired
    private OrderRepo orderRepo;

    @Autowired
    private ProductOrderRepo productOrderRepo;

    @Autowired
    private ProductRepo productRepo;

    @Autowired
    private PdfService pdfService;

    @Autowired
    private EmailService emailService;
    @Autowired
    private PaymentRepository paymentRepo;

    @Autowired
    private ModelMapper modelMapper;
    @Autowired
    TicketRepo ticketRepo;
    @Autowired
    InvoiceRepo invoiceRepo;
    @Autowired
    private SimpMessagingTemplate template;
    @Autowired
    OrderService orderService;
    @Autowired
    AddressRepo addressRepo;
    @Autowired
    UploadTicketRepo uploadTicketRepo;
    @Autowired
    UserRepo userRepo;
    @Autowired
    TicketUpdateHistoryRepo ticketUpdateHistoryRepo;
    @Autowired
    OrderController orderController;
    @Autowired
    ProductPriceRepo productPriceRepo;
    @Autowired
    TicketTrackHistoryService ticketTrackHistoryService;
    @Autowired
    CallServiceHoduCC callServiceHoduCC;
    @Autowired
    private StripeService stripeService;
    private static final Logger logger = LoggerFactory.getLogger(InvoiceController.class);

    @Autowired
    InventorySheetService inventorySheetService;

    // generate and send invoice to customer on email
    @PostMapping("/send-invoice")
    public ResponseEntity<String> sendInvoice(@RequestParam String ticketId, @RequestParam int userId, @RequestParam(required = false) String externalPaymentLink) throws com.avanse.core.exception.TechnicalException, BussinessException, MessagingException {
        logger.info("Send Invoice Called... ");
        TicketEntity ticketEntity = ticketRepo.findByUniqueQueryId(ticketId);
        if (ticketEntity != null) {
            OrderDto orderDto = orderService.getOrder(ticketId);
            Optional<Address> customerAddress = addressRepo.findByTicketId(ticketId);
            Invoice invoice = new Invoice();
            invoice.setTicketId(ticketId);
            invoice.setCreateDate(LocalDate.now());
            invoice.setCreatedByUserId(ticketEntity.getAssigntouser() == null ? 0 : ticketEntity.getAssigntouser());
            invoice.setInviceStatus("Pending");
            invoice.setTotalAmount(orderDto.getTotalPayableAmount());
            invoice.setCurrency(orderDto.getProductOrders().get(0).getCurrency());
            invoice.setCreatedByUserId(userId);
            Invoice invoice1 = invoiceRepo.findByTicketId(ticketId);
            invoice.setIsViewByCustomer(false);
            if (invoice1 == null) {
                invoiceRepo.save(invoice);
            } else {
                invoice1.setTotalAmount(orderDto.getTotalPayableAmount());
                invoice1.setCurrency(orderDto.getProductOrders().get(0).getCurrency());
                invoice1.setLastupdateDate(LocalDate.now());
                invoice1.setCreatedByUserId(userId);
                invoice1.setIsViewByCustomer(false);
                invoiceRepo.save(invoice1);
            }
            template.convertAndSend("/topic/invoice/", invoice);
            Map<String, Object> templateModel = new HashMap<>();
            Map<String, String> customer = new HashMap<>();
            customer.put("name", ticketEntity.getSenderName());
            customer.put("email", ticketEntity.getSenderEmail());
            customer.put("mobile", ticketEntity.getSenderMobile());
            if (customerAddress.isPresent()) {
                customer.put("address", customerAddress.get().getHouseNumber() + "," + customerAddress.get().getLandmark() + "," + customerAddress.get().getCity() + "," + customerAddress.get().getState() + "," + customerAddress.get().getCountry() + "," + customerAddress.get().getZipCode());
            } else {
                customer.put("address", "Address Not available");
            }
            templateModel.put("customer", customer);

// Company information
            Map<String, String> company = new HashMap<>();
            company.put("name", "BuyMed24.com");
            company.put("email", "invoice@buymed24.com");
            company.put("country", "India");
            templateModel.put("company", company);

// Products
            List<Map<String, Object>> products = new ArrayList<>();
            for (int i = 0; i < orderDto.getProductOrders().size(); i++) {
                ProductDto product = orderDto.getProductOrders().get(i).getProduct().get(0);
                Product product1 = productRepo.findByProductId(product.getProductId());
                products.add(Map.of(
                        "name", product.getName(),
                        "image", "https://rdvision.in/images/getProductImage/" + product1.getProductId(),
                        "description", product1.getComposition(),
                        "quantity", orderDto.getProductOrders().get(i).getQuantity(),
                        "price", orderDto.getProductOrders().get(i).getTotalAmount()
                ));
            }
            templateModel.put("products", products);

            templateModel.put("total_price", orderDto.getTotalPayableAmount());

// Ratings
            Map<String, Object> ratings = new HashMap<>();
            ratings.put("currency", orderDto.getProductOrders().get(0).getCurrency());
            ratings.put("customer_count", 22345);
            String paymentLink = "https://crm.rdvision.in/viewinvoice/" + ticketId;
            if (externalPaymentLink.isEmpty()) {
                System.out.println(paymentLink);
                ratings.put("paymentLink", paymentLink);
            } else {
                System.out.println(externalPaymentLink);
                ratings.put("paymentLink", externalPaymentLink);
            }
            templateModel.put("ratings", ratings);

// Gallery images
            List<String> gallery = Arrays.asList(
                    "https://1drv.ms/i/s!AqitzNn5GW7cb3xDT057LNjP4k8?embed=1&width=240&height=210",
                    "https://1drv.ms/i/s!AqitzNn5GW7cdM__mGbzfWUhLBk?embed=1&width=262&height=243",
                    "https://1drv.ms/i/s!AqitzNn5GW7ceizi7Kxox9sqjrs?embed=1&width=360&height=360",
                    "https://1drv.ms/i/s!AqitzNn5GW7cdr5A6Y9JDziM0Vc?embed=1&width=227&height=222",
                    "https://1drv.ms/i/s!AqitzNn5GW7ce6zC0Fhj3YflujE?embed=1&width=1024&height=224",
                    "https://1drv.ms/i/s!AqitzNn5GW7cd8f2sYtCamWRCZI?embed=1&width=228&height=222",
                    "https://1drv.ms/i/s!AqitzNn5GW7ceE7VaJEj0S0MEzU?embed=1&width=1300&height=1390",
                    "https://1drv.ms/i/s!AqitzNn5GW7ceaErIxOWmrZSajc?embed=1&width=626&height=512"
            );
            templateModel.put("gallery", gallery);
            TicketStatusUpdateHistory ticketStatusUpdateHistory = new TicketStatusUpdateHistory();
            ticketStatusUpdateHistory.setStatus("paylink");
            ticketStatusUpdateHistory.setUpdateDate(LocalDate.now());
            ticketStatusUpdateHistory.setTicketIdWhichUpdating(ticketId);
            ticketStatusUpdateHistory.setUpdatedBy(userId);
            User user = userRepo.findByUserId(userId).get();
            ticketStatusUpdateHistory.setComment("Invoice sent by " + user.getFirstName());
            ticketStatusUpdateHistory.setUpdateTime(LocalTime.now());
            ticketStatusUpdateHistory.setUserName(user.getFirstName() + " " + user.getLastName());
            ticketUpdateHistoryRepo.save(ticketStatusUpdateHistory);
            // Send email with the updated template model
            if (customerAddress != null) {
                emailService.sendInvoiceWithPdf(ticketEntity.getSenderEmail(), "invoice", templateModel);
            } else {
                emailService.sendQuotationWithPdf(ticketEntity.getSenderEmail(), "invoice", templateModel);

            }
            ticketEntity.setLastActionDate(LocalDate.now());
            if (ticketEntity.getAssigntouser() == null) {
                ticketEntity.setAssigntouser(userId);
                ticketEntity.setAssignDate(LocalDate.now());
            }
            ticketRepo.save(ticketEntity);
        } else {
            Optional<Address> customerAddress = addressRepo.findByTicketId(ticketId);
            UploadTicket uploadTicket = uploadTicketRepo.findByUniqueQueryId(ticketId);
            Invoice invoice = new Invoice();
            invoice.setTicketId(ticketId);
            invoice.setCreateDate(LocalDate.now());
            invoice.setCreatedByUserId(uploadTicket.getAssigntouser() == null ? 0 : uploadTicket.getAssigntouser());
            invoice.setInviceStatus("Pending");
            invoice.setCreatedByUserId(userId);
            invoice.setIsViewByCustomer(false);
            invoiceRepo.save(invoice);
            template.convertAndSend("/topic/invoice/", invoice);

            OrderDto orderDto = orderService.getOrder(ticketId);
//

            Map<String, Object> templateModel = new HashMap<>();
// Customer information
            Map<String, String> customer = new HashMap<>();
            customer.put("name", uploadTicket.getFirstName() + " " + uploadTicket.getLastName());
            customer.put("email", uploadTicket.getEmail());
            customer.put("mobile", uploadTicket.getMobileNumber());
            customer.put("address", uploadTicket.getSenderAddress());
            templateModel.put("customer", customer);

// Company information
            Map<String, String> company = new HashMap<>();
            company.put("name", "rDvisioN");
            company.put("email", "rdvision@gmail.com");
            company.put("country", "India");
            templateModel.put("company", company);

// Products
            List<Map<String, Object>> products = new ArrayList<>();
            for (int i = 0; i < orderDto.getProductOrders().size(); i++) {
                ProductDto product = orderDto.getProductOrders().get(i).getProduct().get(0);
                Product product1 = productRepo.findByProductId(product.getProductId());
                products.add(Map.of(
                        "name", product.getName(),
                        "image", "https://rdvision.in/images/image/" + product1.getImageListInByte().get(0).getImageId(),
                        "description", product1.getComposition(),
                        "quantity", orderDto.getProductOrders().get(i).getQuantity(),
                        "price", orderDto.getProductOrders().get(i).getTotalAmount()
                ));
            }


            templateModel.put("products", products);


            templateModel.put("total_price", orderDto.getTotalPayableAmount());

// Ratings
            Map<String, Object> ratings = new HashMap<>();
            ratings.put("currency", orderDto.getProductOrders().get(0).getCurrency());
            ratings.put("average", "★★★★☆");
            ratings.put("customer_count", 22445);
            String paymentLink = "https://crm.rdvision.in/viewinvoice/" + ticketId;
            if (externalPaymentLink.isEmpty()) {
                System.out.println(paymentLink);
                ratings.put("paymentLink", paymentLink);
            } else {
                System.out.println(externalPaymentLink);
                ratings.put("paymentLink", externalPaymentLink);
            }
            templateModel.put("ratings", ratings);

// Gallery images
            List<String> gallery = Arrays.asList("https://1drv.ms/i/s!AqitzNn5GW7cb3xDT057LNjP4k8?embed=1&width=240&height=210",
                    "https://1drv.ms/i/s!AqitzNn5GW7cdM__mGbzfWUhLBk?embed=1&width=262&height=243",
                    "https://1drv.ms/i/s!AqitzNn5GW7ceizi7Kxox9sqjrs?embed=1&width=360&height=360",
                    "https://1drv.ms/i/s!AqitzNn5GW7cdr5A6Y9JDziM0Vc?embed=1&width=227&height=222",
                    "https://1drv.ms/i/s!AqitzNn5GW7ce6zC0Fhj3YflujE?embed=1&width=1024&height=224",
                    "https://1drv.ms/i/s!AqitzNn5GW7cd8f2sYtCamWRCZI?embed=1&width=228&height=222",
                    "https://1drv.ms/i/s!AqitzNn5GW7ceE7VaJEj0S0MEzU?embed=1&width=1300&height=1390",
                    "https://1drv.ms/i/s!AqitzNn5GW7ceaErIxOWmrZSajc?embed=1&width=626&height=512"
            );
            templateModel.put("gallery", gallery);
            TicketStatusUpdateHistory ticketStatusUpdateHistory = new TicketStatusUpdateHistory();
            ticketStatusUpdateHistory.setStatus("paylink");
            ticketStatusUpdateHistory.setUpdateDate(LocalDate.now());
            ticketStatusUpdateHistory.setTicketIdWhichUpdating(ticketId);
            ticketStatusUpdateHistory.setUpdatedBy(userId);
            User user = userRepo.findByUserId(userId).get();
            ticketStatusUpdateHistory.setComment("Invoice sent by " + user.getFirstName());
            ticketStatusUpdateHistory.setUpdateTime(LocalTime.now());
            ticketStatusUpdateHistory.setUserName(user.getFirstName() + " " + user.getLastName());
            ticketUpdateHistoryRepo.save(ticketStatusUpdateHistory);
            if (customerAddress.isPresent()) {
                emailService.sendInvoiceWithPdf(uploadTicket.getEmail(), "invoice", templateModel);
            } else {
                emailService.sendQuotationWithPdf(uploadTicket.getEmail(), "invoice", templateModel);

            }
            uploadTicket.setLastActionDate(LocalDate.now());
            if (uploadTicket.getAssigntouser() == null) {
                uploadTicket.setAssigntouser(userId);
                uploadTicket.setAssignDate(LocalDate.now());
            }
            uploadTicketRepo.save(uploadTicket);
        }
        try {
            ticketTrackHistoryService.addTicketTrackHistory(ticketId, ticketEntity.getSenderName(), ticketEntity.getTicketstatus(), ticketEntity.getQueryTime(), userId, "Invoice Sent");
        } catch (Exception e) {
            System.out.println("error");
        }
        return ResponseEntity.ok("Invoice sent successfully!");
    }

    // save information for manual marked as sale tickets
    @PostMapping("/save-information")
    public ResponseEntity<String> saveInformationForDirectSale(@RequestParam String ticketId, @RequestParam int userId, @RequestBody Payment payment) throws com.avanse.core.exception.TechnicalException, BussinessException, MessagingException {
        logger.info("Saving information's for direct sales ... ");
        TicketEntity ticketEntity = ticketRepo.findByUniqueQueryId(ticketId);
        if (ticketEntity != null) {
            OrderDto orderDto = orderService.getOrder(ticketId);
            Optional<Address> customerAddress = addressRepo.findByTicketId(ticketId);
            Invoice invoice = new Invoice();
            invoice.setTicketId(ticketId);
            invoice.setCreateDate(LocalDate.now());
            invoice.setCreatedByUserId(ticketEntity.getAssigntouser() == null ? 0 : ticketEntity.getAssigntouser());
            invoice.setInviceStatus("Pending");
            invoice.setTotalAmount(orderDto.getTotalPayableAmount());
            invoice.setCurrency(orderDto.getProductOrders().get(0).getCurrency());
            invoice.setCreatedByUserId(userId);
            invoice.setPaymentStatus("paid");
            invoice.setInviceStatus("paid");
            Invoice invoice1 = invoiceRepo.findByTicketId(ticketId);
            invoice.setIsViewByCustomer(true);
            if (invoice1 == null) {
                Invoice invoice2 = invoiceRepo.save(invoice);
                payment.setInvoiceId(String.valueOf(invoice2.getInviceId()));
                payment.setTicketId(invoice2.getTicketId());
                payment.setPaymentStatus("paid");
                paymentRepo.save(payment);

            } else {
                invoice1.setTotalAmount(orderDto.getTotalPayableAmount());
                invoice1.setCurrency(orderDto.getProductOrders().get(0).getCurrency());
                invoice1.setLastupdateDate(LocalDate.now());
                invoice1.setCreatedByUserId(userId);
                invoice1.setIsViewByCustomer(true);
                invoice1.setPaymentStatus("paid");
                invoice1.setInviceStatus("paid");
                Invoice invoice2 = invoiceRepo.save(invoice1);
                payment.setInvoiceId(String.valueOf(invoice2.getInviceId()));
                payment.setTicketId(invoice2.getTicketId());
                payment.setPaymentStatus("paid");
                paymentRepo.save(payment);


            }
            OrderItem orderItem = orderRepo.findByTicketId(ticketId);
            orderItem.setPaymentStatus(PaymentStatus.COMPLETED);
            orderItem.setOrderPaidDate(LocalDateTime.now());
            orderRepo.save(orderItem);

            ticketEntity.setLastActionDate(LocalDate.now());
            ticketEntity.setTicketstatus("Sale");
            if (ticketEntity.getAssigntouser() == null) {
                ticketEntity.setAssigntouser(userId);
                ticketEntity.setAssignDate(LocalDate.now());
            }
            orderConfirmation(orderDto, ticketEntity, addressRepo.findByTicketId(ticketId).get());
            ticketRepo.save(ticketEntity);
        } else {
            Optional<Address> customerAddress = addressRepo.findByTicketId(ticketId);
            UploadTicket uploadTicket = uploadTicketRepo.findByUniqueQueryId(ticketId);
            Invoice invoice = new Invoice();
            invoice.setTicketId(ticketId);
            invoice.setCreateDate(LocalDate.now());
            invoice.setInviceStatus("Pending");
            invoice.setCreatedByUserId(userId);
            invoice.setIsViewByCustomer(true);
            invoice.setPaymentStatus("paid");
            invoice.setInviceStatus("paid");
            Invoice invoice2 = invoiceRepo.save(invoice);
            payment.setInvoiceId(String.valueOf(invoice2.getInviceId()));
            payment.setTicketId(invoice2.getTicketId());
            payment.setPaymentStatus("paid");
            paymentRepo.save(payment);
            template.convertAndSend("/topic/invoice/", invoice);
            OrderItem orderItem = orderRepo.findByTicketId(ticketId);
            orderItem.setPaymentStatus(PaymentStatus.COMPLETED);
            orderItem.setOrderPaidDate(LocalDateTime.now());
            orderRepo.save(orderItem);
            OrderDto orderDto = orderService.getOrder(ticketId);
            uploadTicket.setLastActionDate(LocalDate.now());
            if (uploadTicket.getAssigntouser() == null) {
                uploadTicket.setAssigntouser(userId);
                uploadTicket.setAssignDate(LocalDate.now());
            }
            uploadTicket.setTicketstatus("Sale");
            uploadTicketRepo.save(uploadTicket);
            orderConfirmationForUploaded(orderDto, uploadTicket, addressRepo.findByTicketId(ticketId).get());
        }
        try {
            ticketTrackHistoryService.addTicketTrackHistory(ticketId, ticketEntity.getSenderName(), ticketEntity.getTicketstatus(), ticketEntity.getQueryTime(), userId, "Invoice Sent");
        } catch (Exception e) {
            System.out.println("error");
        }
        return ResponseEntity.ok("Invoice sent successfully!");
    }

    //sending quotation mail to customer by closer
    @PostMapping("/sendquote")
    public String sendQuotationWithPriceList(@RequestParam String ticketId, @RequestParam int userId,
                                             @RequestParam Map<String, Object> templateModel)
            throws com.avanse.core.exception.TechnicalException, BussinessException {

        TicketEntity ticketEntity = ticketRepo.findByUniqueQueryId(ticketId);
        if (ticketEntity != null) {
            OrderDto orderDto = orderService.getOrder(ticketId);
            List<ProductOrderDto> productOrders = orderDto.getProductOrders();
            List<Map<String, Object>> products = new ArrayList<>();

            for (ProductOrderDto productOrder : productOrders) {
                ProductDto product = productOrder.getProduct().get(0); // Assuming one product per order
                Product productEntity = productRepo.findByProductId(product.getProductId());

                List<ProductsPrice> prices = productPriceRepo.findByProductAndTicketId(productEntity, ticketId);
                Product product1 = productRepo.findByProductId(product.getProductId());
                // Convert the price list into a list of maps with string values
                List<Map<String, String>> priceList = product1.getPriceList().stream().map(price -> {
                    try {
                        return Map.of("quantity", String.valueOf(price.getQuantity()), "amount", String.valueOf(price.getPrice()), "unit", price.getUnit(), "paymentLink", stripeService.createPaymentLink((long) price.getPrice() * 100, price.getCurrency(), price.getProductCode(), ticketId, product.getName()), "currency", price.getCurrency());
                    } catch (StripeException e) {
                        throw new RuntimeException(e);
                    }
                }).toList();
                products.add(Map.of(
                        "name", product.getName(),
                        "strength", product1.getStrength(),
                        "size", product1.getPackagingSize(),
                        "form", product1.getPackagingType(),
                        "image", "https://rdvision.in/images/image/" + product1.getImageListInByte().get(0).getImageId(),
                        "composition", productEntity.getComposition(),
                        "brand", product.getBrand(),
                        "treatment", productEntity.getTreatment(),
                        "priceList", priceList
                ));
            }

            // Add product data to the map
            templateModel.put("products", products);

            // Mock company data or fetch from repository
            templateModel.put("company", Map.of(
                    "name", "Buymed24",
                    "address", "Varanasi India",
                    "contact", "+1-234-567-890",
                    "email", "invoice@buymed24.com",
                    "customer", ticketEntity.getSenderName()
            ));

            // Send the email
            String recipientEmail = ticketEntity.getSenderEmail(); // Replace with actual customer email
            String subject = "Your Quotation from R&D Vision";
            emailService.sendNewQuotation(recipientEmail, subject, templateModel);
            try {
                ticketTrackHistoryService.addTicketTrackHistory(ticketId, ticketEntity.getSenderName(), ticketEntity.getTicketstatus(), ticketEntity.getQueryTime(), userId, "Quotation Sent");
            } catch (Exception e) {
                System.out.println("error");
            }
        } else {
            UploadTicket uploadTicket = uploadTicketRepo.findByUniqueQueryId(ticketId);
            OrderDto orderDto = orderService.getOrder(ticketId);
            List<ProductOrderDto> productOrders = orderDto.getProductOrders();
            List<Map<String, Object>> products = new ArrayList<>();

            for (ProductOrderDto productOrder : productOrders) {
                ProductDto product = productOrder.getProduct().get(0); // Assuming one product per order
                Product productEntity = productRepo.findByProductId(product.getProductId());

                List<ProductsPrice> prices = productPriceRepo.findByProductAndTicketId(productEntity, ticketId);
                Product product1 = productRepo.findByProductId(product.getProductId());
                List<Map<String, String>> priceList = product1.getPriceList().stream().map(price -> {
                    try {
                        return Map.of("quantity", String.valueOf(price.getQuantity()), "amount", String.valueOf(price.getPrice()), "unit", price.getUnit(), "paymentLink", stripeService.createPaymentLink((long) price.getPrice() * 100, price.getCurrency(), price.getProductCode(), ticketId, product.getName()), "currency", price.getCurrency());
                    } catch (StripeException e) {
                        throw new RuntimeException(e);
                    }
                }).toList();
                products.add(Map.of(
                        "name", product.getName(),
                        "strength", product1.getStrength(),
                        "size", product1.getPackagingSize(),
                        "form", product1.getPackagingType(),
                        "image", "https://rdvision.in/images/image/" + product1.getImageListInByte().get(0).getImageId(),
                        "composition", productEntity.getComposition(),
                        "brand", product.getBrand(),
                        "treatment", productEntity.getTreatment(),
                        "priceList", priceList
                ));
            }

            // Add product data to the map
            templateModel.put("products", products);

            // Mock company data or fetch from repository
            templateModel.put("company", Map.of(
                    "name", "Buymed24",
                    "address", "Varanasi India",
                    "contact", "+1-234-567-890",
                    "email", "invoice@buymed24.com",
                    "customer", uploadTicket.getFirstName()
            ));

            // Send the email
            String recipientEmail = uploadTicket.getEmail(); // Replace with actual customer email
            String subject = "Your Quotation from R&D Vision";
            emailService.sendNewQuotation(recipientEmail, subject, templateModel);
            try {
                ticketTrackHistoryService.addTicketTrackHistory(ticketId, uploadTicket.getFirstName(), uploadTicket.getTicketstatus(), uploadTicket.getUploadDate() + uploadTicket.getQueryTime(), userId, "Quotation Sent");
            } catch (Exception e) {
                System.out.println("error");
            }
        }


        return "product-details"; // Thymeleaf template
    }


    //getting all invoices
    @GetMapping("/getinvoices")
    public List<Invoice> getAllInvoice() {
        return invoiceRepo.findAll();
    }

    //view invoice api
    @GetMapping("/viewInvoice/{ticketid}")
    public void setViewInvoice(@PathVariable String ticketid, HttpServletRequest request) {
        String clientIp = getClientIp(request);
        System.out.println("Client IP: " + clientIp); // Or log it using a logging framework

        Invoice invoice = invoiceRepo.findByTicketId(ticketid);
        if (invoice != null) {
            invoice.setIsViewByCustomer(true);
            invoice.setCustomerIp(clientIp);
            invoiceRepo.save(invoice);
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        // Handle IPv6 localhost or any IPv6 to IPv4 conversion
        if ("0:0:0:0:0:0:0:1".equals(ip) || ip.startsWith("::1")) {
            ip = "127.0.0.1"; // Resolve to IPv4 localhost
        } else if (ip.contains(":")) {
            // If the address is still IPv6, handle conversion if possible
            try {
                ip = InetAddress.getByName(ip).getHostAddress();
                if (ip.contains(":")) {
                    // Convert to IPv4 if IPv6 mapped IPv4 format (e.g., ::ffff:192.168.1.1)
                    ip = InetAddress.getByName(ip).getHostAddress().replace("::ffff:", "");
                }
            } catch (UnknownHostException e) {
                e.printStackTrace(); // Log the error
            }
        }

        return ip;
    }

    //getting user agree status is he agree or not
    @GetMapping("/getAgreeStatus/{ticketId}")
    public String getAgreeStatus(@PathVariable String ticketId) {
        return invoiceRepo.findByTicketId(ticketId).getAgreeStatus();
    }

    //when user agree to invoices
    @PostMapping("/agree")
    public String agreeDisagree(@RequestBody Map<String, String> object) {
        if (object.get("status").equals("Agree")) {
            Invoice invoice = invoiceRepo.findByTicketId(object.get("ticketId"));
            invoice.setAgreeStatus("Agree");
            invoiceRepo.save(invoice);
            return "Agreed";
        } else {
            Invoice invoice = invoiceRepo.findByTicketId(object.get("ticketId"));
            invoice.setAgreeStatus("Dis Agree");
            invoice.setQuotedPrice(Double.parseDouble(object.get("price").toString()));
            invoiceRepo.save(invoice);
            return "Your quoted price send to our team";

        }
    }


    //checkout session for stripe api
    @PostMapping("/create-checkout-session/{ticketId}")
    public String createCheckoutSession(@PathVariable String ticketId, @RequestBody Map<String, Boolean> object) {
        OrderItem orderItem = orderRepo.findByTicketId(ticketId);
        Invoice invoice = invoiceRepo.findByTicketId(ticketId);
        invoice.setNumberOfAttemptForPayment(invoice.getNumberOfAttemptForPayment() + 1);
        System.out.println(invoice.getNumberOfAttemptForPayment());
        invoiceRepo.save(invoice);
        List<ProductOrder> productOrders = productOrderRepo.findAllByOrderId(orderItem.getOrderId());
        double payableAmount = 0;
        for (int i = 0; i < productOrders.size(); i++) {
            payableAmount += productOrders.get(i).getTotalAmount();
        }
        try {
            Session session = stripeService.createCheckoutSession(ticketId, productOrders.get(0).getCurrency(), payableAmount, object.get("isLive"));
            System.out.println(session.toString());
            return "{\"id\": \"" + session.getId() + "\"}"; // Return the session ID to the frontend
        } catch (StripeException e) {
            System.out.println(e.toString());
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }

    @GetMapping("/invoiceNumbers")
    public Map<String, Long> numberOfInvoicesInDifferentStage() {
        Map<String, Long> map = invoiceRepo.findNumberOfInvoice();
//        map.put("numberOfAwaitingOrders",orderRepo.countOrderItemWhereOrderStatusIsPaidAndTrackingNumberIsNotNull());
        return map;
    }

    //Number of total invoices
    @GetMapping("/invoideCOunt")
    public Map<String, Long> invoiceCount() {
        return invoiceRepo.findNumberOfInvoice();
    }


    //Add tracking number to invoices for tracking opn track 17
    @PostMapping("/addTrackingNumber")
    public ResponseEntity<?> addTrackingNumberToOrderAndInvoice(@RequestBody Map<String, String> object) throws Exception {
        Invoice invoice = invoiceRepo.findByTicketId(object.get("ticketId"));
        if (invoice == null) {
            TicketEntity ticketEntity = ticketRepo.findByUniqueQueryId(object.get("ticketId"));
            if (ticketEntity == null) {
                UploadTicket uploadTicket = uploadTicketRepo.findByUniqueQueryId(object.get("ticketId"));
                uploadTicket.setTrackingNumber(object.get("trackingNumber"));
                uploadTicketRepo.save(uploadTicket);
            } else {
                ticketEntity.setTrackingNumber(object.get("trackingNumber"));
                ticketRepo.save(ticketEntity);
            }
        } else {
            OrderItem orderItem = orderRepo.findByTicketId(object.get("ticketId"));
            TicketEntity ticketEntity = ticketRepo.findByUniqueQueryId(object.get("ticketId"));
            if (ticketEntity == null) {
                UploadTicket uploadTicket = uploadTicketRepo.findByUniqueQueryId(object.get("ticketId"));
                uploadTicket.setTrackingNumber(object.get("trackingNumber"));
                uploadTicketRepo.save(uploadTicket);
            } else {
                ticketEntity.setTrackingNumber(object.get("trackingNumber"));
                ticketRepo.save(ticketEntity);
            }
            invoice.setTrackingNumber(object.get("trackingNumber"));
            orderItem.setTrackingNumber(object.get("trackingNumber"));

            invoiceRepo.save(invoice);
            orderRepo.save(orderItem);
        }

        List<String> number = new ArrayList<>();
        number.add(object.get("trackingNumber"));
        registerTrackingNumbers(number);
        return ResponseEntity.ok("Tracking Number Added");
    }

    private static final String API_URL = "https://api.17track.net/track/v2.2/register";
    private static final String API_TOKEN = "F28E9A9EEE74E07F8959451F6C0E470D";  // Replace with your actual token

    //register method for tracking number on track 17
    public static void registerTrackingNumbers(List<String> trackingNumbers) throws Exception {
        HttpClient client = HttpClient.newHttpClient();

        // Create JSON payload dynamically based on the list of tracking numbers
        String jsonPayload = generateJsonPayload(trackingNumbers);

        // Create the POST request
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("17token", API_TOKEN)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        // Send the request and get the response
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Check the response status code and print the response
        if (response.statusCode() == 200) {
            System.out.println("Success! Response: " + response.body());
        } else {
            System.out.println("Error: " + response.statusCode());
            System.out.println("Response: " + response.body());
        }
    }

    // Helper method to generate JSON payload
    private static String generateJsonPayload(List<String> trackingNumbers) {
        return trackingNumbers.stream()
                .map(number -> String.format("{\"number\": \"%s\"}", number))
                .collect(Collectors.joining(", ", "[", "]"));
    }

    //click to call for ass tickets
    @GetMapping("/clickToCall/{ticketId}")
    public ResponseEntity<String> clicktoCallByTicketid(@PathVariable String ticketId) {
        ResponseEntity<String> response = callServiceHoduCC.clickToCallByTicketId(ticketId);
        String recordingFile = callServiceHoduCC.getRecording(extractCallId(response));
        Invoice invoice = invoiceRepo.findByTicketId(ticketId);
        invoice.setCallRecording(recordingFile);
        invoice.setAssCallStatus("Called");
        invoiceRepo.save(invoice);
        return response;
    }


    //extracting call id from click to call
    public String extractCallId(ResponseEntity<String> response) {
        String responseBody = response.getBody();
        if (responseBody != null) {
            int jsonStartIndex = responseBody.indexOf('{');
            if (jsonStartIndex != -1) {
                String jsonString = responseBody.substring(jsonStartIndex);
                JSONObject jsonObject = new JSONObject(jsonString);
                if (jsonObject.has("call_id")) {
                    return jsonObject.getString("call_id");
                }
            }
        }
        return null; // return null if not found
    }


    //invoice for verification which are pending for verification
    @GetMapping("/verificationList")
    public List<InvoiceVerificationDto> getVerificationInvoiceList() throws com.avanse.core.exception.TechnicalException, BussinessException {
        List<Invoice> invoices = invoiceRepo.findByPaymentStatusIgnoreCaseAndIsVerified("paid", false);
        List<InvoiceVerificationDto> invoiceVerificationDtos = new ArrayList<>();
        for (int i = 0; i < invoices.size(); i++) {
            InvoiceVerificationDto invoiceVerificationDto = new InvoiceVerificationDto();
            TicketEntity ticketEntity = ticketRepo.findByUniqueQueryId(invoices.get(i).getTicketId());
            if (ticketEntity != null) {
                System.out.println(invoices.get(i).getInviceId().toString());
                Payment payment = paymentRepo.findByInvoiceId(invoices.get(i).getInviceId().toString());
                Optional<User> user = userRepo.findByUserId(invoices.get(i).getCreatedByUserId());
                Optional<Address> address = addressRepo.findByTicketId(invoices.get(i).getTicketId());
                OrderDto orderDto = (OrderDto) orderController.getOrder(ticketEntity.getUniqueQueryId()).get("dtoList");
                invoiceVerificationDto.setInvoiceId(invoices.get(i).getInviceId());
                invoiceVerificationDto.setOrderDto(orderDto);
                invoiceVerificationDto.setCloserName(user.map(value -> value.getFirstName() + " " + value.getLastName()).orElse("Stripe"));
                invoiceVerificationDto.setCustomerName(ticketEntity.getSenderName());
                invoiceVerificationDto.setCustomerEmail(ticketEntity.getSenderEmail());
                invoiceVerificationDto.setCustomerMobile(ticketEntity.getSenderMobile());
                invoiceVerificationDto.setSaleDate(ticketEntity.getLastActionDate());
                invoiceVerificationDto.setOrderAmount(orderDto.getTotalPayableAmount());
                invoiceVerificationDto.setUniqueQueryId(ticketEntity.getUniqueQueryId());
                invoiceVerificationDto.setPayment(payment);
                invoiceVerificationDto.setAddress(address.orElse(null));
                invoiceVerificationDtos.add(invoiceVerificationDto);
                invoiceVerificationDto.setIpAddress(invoices.get(i).getCustomerIp());
                invoiceVerificationDto.setOpened(invoices.get(i).getIsViewByCustomer() != null && invoices.get(i).getIsViewByCustomer());
                invoiceVerificationDto.setCountryIso(ticketEntity.getSenderCountryIso());
                invoiceVerificationDto.setIsVerifiedByAdmin(invoices.get(i).getIsVerifiedByAdmin());
                invoiceVerificationDto.setShippingCareer(invoices.get(i).getShippingCareer());

            } else {
                UploadTicket uploadTicket = uploadTicketRepo.findByUniqueQueryId(invoices.get(i).getTicketId());
                System.out.println(invoices.get(i).getInviceId().toString());
                Payment payment = paymentRepo.findByInvoiceId(invoices.get(i).getInviceId().toString());
                Optional<User> user = userRepo.findByUserId(invoices.get(i).getCreatedByUserId());
                Optional<Address> address = addressRepo.findByTicketId(invoices.get(i).getTicketId());
                OrderDto orderDto = (OrderDto) orderController.getOrder(uploadTicket.getUniqueQueryId()).get("dtoList");
                invoiceVerificationDto.setInvoiceId(invoices.get(i).getInviceId());
                invoiceVerificationDto.setOrderDto(orderDto);
                invoiceVerificationDto.setCloserName(user.map(value -> value.getFirstName() + " " + value.getLastName()).orElse("Stripe"));
                invoiceVerificationDto.setCustomerName(uploadTicket.getFirstName() + " " + uploadTicket.getLastName());
                invoiceVerificationDto.setCustomerEmail(uploadTicket.getEmail());
                invoiceVerificationDto.setCustomerMobile(uploadTicket.getMobileNumber());
                invoiceVerificationDto.setSaleDate(uploadTicket.getLastActionDate());
                invoiceVerificationDto.setOrderAmount(orderDto.getTotalPayableAmount());
                invoiceVerificationDto.setUniqueQueryId(uploadTicket.getUniqueQueryId());
                invoiceVerificationDto.setPayment(payment);
                invoiceVerificationDto.setAddress(address.orElse(null));
                invoiceVerificationDtos.add(invoiceVerificationDto);
                invoiceVerificationDto.setIpAddress(invoices.get(i).getCustomerIp());
                invoiceVerificationDto.setOpened(invoices.get(i).getIsViewByCustomer() != null && invoices.get(i).getIsViewByCustomer());
                invoiceVerificationDto.setCountryIso(uploadTicket.getSenderCountryIso());
                invoiceVerificationDto.setIsVerifiedByAdmin(invoices.get(i).getIsVerifiedByAdmin());
                invoiceVerificationDto.setShippingCareer(invoices.get(i).getShippingCareer());
            }
        }
        return invoiceVerificationDtos;
    }


    //Verification api for ss
    @GetMapping("/setVerified/{invoiceId}")
    public String verifyInvoiceById(@PathVariable int invoiceId) {
        Invoice invoice = invoiceRepo.findByInviceId(invoiceId);
        invoice.setVerificationDate(LocalDate.now());
        invoice.setVerificationTime(LocalTime.now());
        invoice.setVerified(true);
        template.convertAndSend("/topic/invoice/verified/", invoice.getCreatedByUserId());
        invoiceRepo.save(invoice);
        return "Verified";
    }

    //All verified invoices by Senior supervisor
    @GetMapping("/getVerifiedInvoives")
    public List<InvoiceVerificationDto> getVerifiedInvoicesDetails() throws com.avanse.core.exception.TechnicalException, BussinessException {
        List<Invoice> invoices = invoiceRepo.findByPaymentStatusIgnoreCaseAndIsVerified("paid", true);
        List<InvoiceVerificationDto> invoiceVerificationDtos = new ArrayList<>();
        for (int i = 0; i < invoices.size(); i++) {
            InvoiceVerificationDto invoiceVerificationDto = new InvoiceVerificationDto();
            TicketEntity ticketEntity = ticketRepo.findByUniqueQueryId(invoices.get(i).getTicketId());
            if (ticketEntity != null) {
                Payment payment = paymentRepo.findByInvoiceId(invoices.get(i).getInviceId().toString());
                Optional<User> user = userRepo.findByUserId(invoices.get(i).getCreatedByUserId());
                Optional<Address> address = addressRepo.findByTicketId(invoices.get(i).getTicketId());
                OrderDto orderDto = (OrderDto) orderController.getOrder(ticketEntity.getUniqueQueryId()).get("dtoList");
                invoiceVerificationDto.setInvoiceId(invoices.get(i).getInviceId());
                invoiceVerificationDto.setOrderDto(orderDto);
                invoiceVerificationDto.setCloserName(user.map(value -> value.getFirstName() + " " + value.getLastName()).orElse("Stripe"));
                invoiceVerificationDto.setCustomerName(ticketEntity.getSenderName());
                invoiceVerificationDto.setCustomerEmail(ticketEntity.getSenderEmail());
                invoiceVerificationDto.setCustomerMobile(ticketEntity.getSenderMobile());
                invoiceVerificationDto.setSaleDate(ticketEntity.getLastActionDate());
                invoiceVerificationDto.setOrderAmount(orderDto.getTotalPayableAmount());
                invoiceVerificationDto.setUniqueQueryId(ticketEntity.getUniqueQueryId());
                System.out.println(invoices.get(i).getInviceId().toString());
                invoiceVerificationDto.setPayment(paymentRepo.findByInvoiceId(invoices.get(i).getInviceId().toString()));
                invoiceVerificationDto.setAddress(address.orElse(null));
                invoiceVerificationDtos.add(invoiceVerificationDto);
                invoiceVerificationDto.setIpAddress(invoices.get(i).getCustomerIp());
                invoiceVerificationDto.setOpened(invoices.get(i).getIsViewByCustomer() != null && invoices.get(i).getIsViewByCustomer());
                invoiceVerificationDto.setCountryIso(ticketEntity.getSenderCountryIso());
                invoiceVerificationDto.setTrackingNumber(invoices.get(i).getTrackingNumber());
                invoiceVerificationDto.setDeliveryStatus(invoices.get(i).getDeliveryStatus());
                invoiceVerificationDto.setAssCallStatus(invoices.get(i).getAssCallStatus());
                invoiceVerificationDto.setCallRecording(invoices.get(i).getCallRecording());
                invoiceVerificationDto.setPaymentStatus(invoices.get(i).getPaymentStatus());
                invoiceVerificationDto.setVerificationDate(invoices.get(i).getVerificationDate());
                invoiceVerificationDto.setVerificationTime(invoices.get(i).getVerificationTime());
                invoiceVerificationDto.setIsVerifiedByAdmin(invoices.get(i).getIsVerifiedByAdmin());
                invoiceVerificationDto.setShippingCareer(invoices.get(i).getShippingCareer());
            } else {
                UploadTicket uploadTicket = uploadTicketRepo.findByUniqueQueryId(invoices.get(i).getTicketId());
                Payment payment = paymentRepo.findByInvoiceId(invoices.get(i).getInviceId().toString());
                Optional<User> user = userRepo.findByUserId(invoices.get(i).getCreatedByUserId());
                Optional<Address> address = addressRepo.findByTicketId(invoices.get(i).getTicketId());
                OrderDto orderDto = (OrderDto) orderController.getOrder(uploadTicket.getUniqueQueryId()).get("dtoList");
                invoiceVerificationDto.setInvoiceId(invoices.get(i).getInviceId());
                invoiceVerificationDto.setOrderDto(orderDto);
                invoiceVerificationDto.setCloserName(user.map(value -> value.getFirstName() + " " + value.getLastName()).orElse("Stripe"));
                invoiceVerificationDto.setCustomerName(uploadTicket.getFirstName() + " " + uploadTicket.getLastName());
                invoiceVerificationDto.setCustomerEmail(uploadTicket.getEmail());
                invoiceVerificationDto.setCustomerMobile(uploadTicket.getMobileNumber());
                invoiceVerificationDto.setSaleDate(uploadTicket.getLastActionDate());
                invoiceVerificationDto.setOrderAmount(orderDto.getTotalPayableAmount());
                invoiceVerificationDto.setUniqueQueryId(uploadTicket.getUniqueQueryId());
                System.out.println(invoices.get(i).getInviceId().toString());
                invoiceVerificationDto.setPayment(paymentRepo.findByInvoiceId(invoices.get(i).getInviceId().toString()));
                invoiceVerificationDto.setAddress(address.orElse(null));
                invoiceVerificationDtos.add(invoiceVerificationDto);
                invoiceVerificationDto.setIpAddress(invoices.get(i).getCustomerIp());
                invoiceVerificationDto.setOpened(invoices.get(i).getIsViewByCustomer() != null && invoices.get(i).getIsViewByCustomer());
                invoiceVerificationDto.setCountryIso(uploadTicket.getSenderCountryIso());
                invoiceVerificationDto.setTrackingNumber(invoices.get(i).getTrackingNumber());
                invoiceVerificationDto.setDeliveryStatus(invoices.get(i).getDeliveryStatus());
                invoiceVerificationDto.setAssCallStatus(invoices.get(i).getAssCallStatus());
                invoiceVerificationDto.setCallRecording(invoices.get(i).getCallRecording());
                invoiceVerificationDto.setPaymentStatus(invoices.get(i).getPaymentStatus());
                invoiceVerificationDto.setVerificationDate(invoices.get(i).getVerificationDate());
                invoiceVerificationDto.setVerificationTime(invoices.get(i).getVerificationTime());
                invoiceVerificationDto.setIsVerifiedByAdmin(invoices.get(i).getIsVerifiedByAdmin());
                invoiceVerificationDto.setShippingCareer(invoices.get(i).getShippingCareer());


            }

        }
        return invoiceVerificationDtos;
    }

    //Gets all ASS invoice by User
    @GetMapping("/getAssInvoice/{userId}")
    public List<InvoiceVerificationDto> getAssInvoices(@PathVariable int userId) throws com.avanse.core.exception.TechnicalException, BussinessException {
        User user = userRepo.findByUserId(userId).get();
        if (user.getRoleId() == 4) {
            List<Invoice> invoices = invoiceRepo.findByCreatedByUserIdAndPaymentStatusIgnoreCaseAndIsVerified(userId, "paid", true);
            List<InvoiceVerificationDto> invoiceVerificationDtos = new ArrayList<>();
            for (int i = 0; i < invoices.size(); i++) {
                InvoiceVerificationDto invoiceVerificationDto = new InvoiceVerificationDto();
                TicketEntity ticketEntity = ticketRepo.findByUniqueQueryId(invoices.get(i).getTicketId());
                if (ticketEntity != null) {
                    Optional<Address> address = addressRepo.findByTicketId(invoices.get(i).getTicketId());
                    OrderDto orderDto = (OrderDto) orderController.getOrder(ticketEntity.getUniqueQueryId()).get("dtoList");
                    invoiceVerificationDto.setInvoiceId(invoices.get(i).getInviceId());
                    invoiceVerificationDto.setOrderDto(orderDto);
                    invoiceVerificationDto.setCloserName(user.getFirstName() + " " + user.getLastName());
                    invoiceVerificationDto.setCustomerName(ticketEntity.getSenderName());
                    invoiceVerificationDto.setCustomerEmail(ticketEntity.getSenderEmail());
                    invoiceVerificationDto.setCustomerMobile(ticketEntity.getSenderMobile());
                    invoiceVerificationDto.setSaleDate(ticketEntity.getLastActionDate());
                    invoiceVerificationDto.setOrderAmount(orderDto.getTotalPayableAmount());
                    invoiceVerificationDto.setUniqueQueryId(ticketEntity.getUniqueQueryId());
                    invoiceVerificationDto.setPayment(paymentRepo.findByInvoiceId(invoices.get(i).getInviceId().toString()));
                    invoiceVerificationDto.setAddress(address.orElse(null));
                    invoiceVerificationDtos.add(invoiceVerificationDto);
                    invoiceVerificationDto.setIpAddress(invoices.get(i).getCustomerIp());
                    invoiceVerificationDto.setOpened(invoices.get(i).getIsViewByCustomer() != null && invoices.get(i).getIsViewByCustomer());
                    invoiceVerificationDto.setCountryIso(ticketEntity.getSenderCountryIso());
                    invoiceVerificationDto.setTrackingNumber(invoices.get(i).getTrackingNumber());
                    invoiceVerificationDto.setDeliveryStatus(invoices.get(i).getDeliveryStatus());
                    invoiceVerificationDto.setAssCallStatus(invoices.get(i).getAssCallStatus());
                    invoiceVerificationDto.setCallRecording(invoices.get(i).getCallRecording());
                    invoiceVerificationDto.setIsVerifiedByAdmin(invoices.get(i).getIsVerifiedByAdmin());
                    invoiceVerificationDto.setShippingCareer(invoices.get(i).getShippingCareer());
                } else {
                    UploadTicket uploadTicket = uploadTicketRepo.findByUniqueQueryId(invoices.get(i).getTicketId());
                    Optional<Address> address = addressRepo.findByTicketId(invoices.get(i).getTicketId());
                    OrderDto orderDto = (OrderDto) orderController.getOrder(uploadTicket.getUniqueQueryId()).get("dtoList");
                    invoiceVerificationDto.setInvoiceId(invoices.get(i).getInviceId());
                    invoiceVerificationDto.setOrderDto(orderDto);
                    invoiceVerificationDto.setCloserName(user.getFirstName() + " " + user.getLastName());
                    invoiceVerificationDto.setCustomerName(uploadTicket.getFirstName());
                    invoiceVerificationDto.setCustomerEmail(uploadTicket.getEmail());
                    invoiceVerificationDto.setCustomerMobile(uploadTicket.getMobileNumber());
                    invoiceVerificationDto.setSaleDate(uploadTicket.getLastActionDate());
                    invoiceVerificationDto.setOrderAmount(orderDto.getTotalPayableAmount());
                    invoiceVerificationDto.setUniqueQueryId(uploadTicket.getUniqueQueryId());
                    invoiceVerificationDto.setPayment(paymentRepo.findByInvoiceId(invoices.get(i).getInviceId().toString()));
                    invoiceVerificationDto.setAddress(address.orElse(null));
                    invoiceVerificationDtos.add(invoiceVerificationDto);
                    invoiceVerificationDto.setIpAddress(invoices.get(i).getCustomerIp());
                    invoiceVerificationDto.setOpened(invoices.get(i).getIsViewByCustomer() != null && invoices.get(i).getIsViewByCustomer());
                    invoiceVerificationDto.setCountryIso(uploadTicket.getSenderCountryIso());
                    invoiceVerificationDto.setTrackingNumber(invoices.get(i).getTrackingNumber());
                    invoiceVerificationDto.setDeliveryStatus(invoices.get(i).getDeliveryStatus());
                    invoiceVerificationDto.setAssCallStatus(invoices.get(i).getAssCallStatus());
                    invoiceVerificationDto.setCallRecording(invoices.get(i).getCallRecording());
                    invoiceVerificationDto.setIsVerifiedByAdmin(invoices.get(i).getIsVerifiedByAdmin());
                    invoiceVerificationDto.setShippingCareer(invoices.get(i).getShippingCareer());

                }
            }
            return invoiceVerificationDtos;
        } else if (user.getRoleId() == 5) {
            List<Invoice> invoices = invoiceRepo.findByPaymentStatusIgnoreCaseAndIsVerified("paid", true);
            List<InvoiceVerificationDto> invoiceVerificationDtos = new ArrayList<>();
            for (int i = 0; i < invoices.size(); i++) {

                InvoiceVerificationDto invoiceVerificationDto = new InvoiceVerificationDto();
                TicketEntity ticketEntity = ticketRepo.findByUniqueQueryId(invoices.get(i).getTicketId());
                if (ticketEntity != null) {
                    Optional<Address> address = addressRepo.findByTicketId(invoices.get(i).getTicketId());
                    OrderDto orderDto = (OrderDto) orderController.getOrder(ticketEntity.getUniqueQueryId()).get("dtoList");
                    invoiceVerificationDto.setInvoiceId(invoices.get(i).getInviceId());
                    invoiceVerificationDto.setOrderDto(orderDto);
                    invoiceVerificationDto.setCloserName(user.getFirstName() + " " + user.getLastName());
                    invoiceVerificationDto.setCustomerName(ticketEntity.getSenderName());
                    invoiceVerificationDto.setCustomerEmail(ticketEntity.getSenderEmail());
                    invoiceVerificationDto.setCustomerMobile(ticketEntity.getSenderMobile());
                    invoiceVerificationDto.setSaleDate(ticketEntity.getLastActionDate());
                    invoiceVerificationDto.setOrderAmount(orderDto.getTotalPayableAmount());
                    invoiceVerificationDto.setUniqueQueryId(ticketEntity.getUniqueQueryId());
                    invoiceVerificationDto.setPayment(null);
                    invoiceVerificationDto.setAddress(address.orElse(null));
                    invoiceVerificationDtos.add(invoiceVerificationDto);
                    invoiceVerificationDto.setIpAddress(invoices.get(i).getCustomerIp());
                    invoiceVerificationDto.setOpened(invoices.get(i).getIsViewByCustomer() != null && invoices.get(i).getIsViewByCustomer());
                    invoiceVerificationDto.setCountryIso(ticketEntity.getSenderCountryIso());
                    invoiceVerificationDto.setTrackingNumber(invoices.get(i).getTrackingNumber());
                    invoiceVerificationDto.setDeliveryStatus(invoices.get(i).getDeliveryStatus());
                    invoiceVerificationDto.setAssCallStatus(invoices.get(i).getAssCallStatus());
                    invoiceVerificationDto.setCallRecording(invoices.get(i).getCallRecording());
                    invoiceVerificationDto.setIsVerifiedByAdmin(invoices.get(i).getIsVerifiedByAdmin());
                    invoiceVerificationDto.setShippingCareer(invoices.get(i).getShippingCareer());
                } else {
                    UploadTicket uploadTicket = uploadTicketRepo.findByUniqueQueryId(invoices.get(i).getTicketId());
                    Optional<Address> address = addressRepo.findByTicketId(invoices.get(i).getTicketId());
                    OrderDto orderDto = (OrderDto) orderController.getOrder(uploadTicket.getUniqueQueryId()).get("dtoList");
                    invoiceVerificationDto.setInvoiceId(invoices.get(i).getInviceId());
                    invoiceVerificationDto.setOrderDto(orderDto);
                    invoiceVerificationDto.setCloserName(user.getFirstName() + " " + user.getLastName());
                    invoiceVerificationDto.setCustomerName(uploadTicket.getFirstName());
                    invoiceVerificationDto.setCustomerEmail(uploadTicket.getEmail());
                    invoiceVerificationDto.setCustomerMobile(uploadTicket.getMobileNumber());
                    invoiceVerificationDto.setSaleDate(uploadTicket.getLastActionDate());
                    invoiceVerificationDto.setOrderAmount(orderDto.getTotalPayableAmount());
                    invoiceVerificationDto.setUniqueQueryId(uploadTicket.getUniqueQueryId());
                    invoiceVerificationDto.setPayment(null);
                    invoiceVerificationDto.setAddress(address.orElse(null));
                    invoiceVerificationDtos.add(invoiceVerificationDto);
                    invoiceVerificationDto.setIpAddress(invoices.get(i).getCustomerIp());
                    invoiceVerificationDto.setOpened(invoices.get(i).getIsViewByCustomer() != null && invoices.get(i).getIsViewByCustomer());
                    invoiceVerificationDto.setCountryIso(uploadTicket.getSenderCountryIso());
                    invoiceVerificationDto.setTrackingNumber(invoices.get(i).getTrackingNumber());
                    invoiceVerificationDto.setDeliveryStatus(invoices.get(i).getDeliveryStatus());
                    invoiceVerificationDto.setAssCallStatus(invoices.get(i).getAssCallStatus());
                    invoiceVerificationDto.setCallRecording(invoices.get(i).getCallRecording());
                    invoiceVerificationDto.setIsVerifiedByAdmin(invoices.get(i).getIsVerifiedByAdmin());
                    invoiceVerificationDto.setShippingCareer(invoices.get(i).getShippingCareer());
                }
            }
            return invoiceVerificationDtos;
        } else {
            return null;
        }
    }


    //Invoice generated by users
    @GetMapping("/getInvoiceByUser/{userId}")
    public List<InvoiceVerificationDto> getAllInvoicesByUserId(@PathVariable int userId) throws com.avanse.core.exception.TechnicalException, BussinessException {
        User user = userRepo.findByUserId(userId).get();
        if (user.getRoleId() == 4) {
            List<Invoice> invoices = invoiceRepo.findByCreatedByUserId(userId);
            List<InvoiceVerificationDto> invoiceVerificationDtos = new ArrayList<>();
            for (int i = 0; i < invoices.size(); i++) {
                User user1 = userRepo.findByUserId(invoices.get(i).getCreatedByUserId()).get();
                InvoiceVerificationDto invoiceVerificationDto = new InvoiceVerificationDto();
                TicketEntity ticketEntity = ticketRepo.findByUniqueQueryId(invoices.get(i).getTicketId());
                if (ticketEntity != null) {
                    Optional<Address> address = addressRepo.findByTicketId(invoices.get(i).getTicketId());
                    OrderDto orderDto = (OrderDto) orderController.getOrder(ticketEntity.getUniqueQueryId()).get("dtoList");
                    invoiceVerificationDto.setInvoiceId(invoices.get(i).getInviceId());
                    invoiceVerificationDto.setOrderDto(orderDto);
                    invoiceVerificationDto.setCloserName(user1.getFirstName() + " " + user1.getLastName());
                    invoiceVerificationDto.setCustomerName(ticketEntity.getSenderName());
                    invoiceVerificationDto.setCustomerEmail(ticketEntity.getSenderEmail());
                    invoiceVerificationDto.setCustomerMobile(ticketEntity.getSenderMobile());
                    invoiceVerificationDto.setSaleDate(ticketEntity.getLastActionDate());
                    invoiceVerificationDto.setOrderAmount(orderDto.getTotalPayableAmount());
                    invoiceVerificationDto.setUniqueQueryId(ticketEntity.getUniqueQueryId());
                    invoiceVerificationDto.setPayment(null);
                    invoiceVerificationDto.setAddress(address.orElse(null));
                    invoiceVerificationDtos.add(invoiceVerificationDto);
                    invoiceVerificationDto.setIpAddress(invoices.get(i).getCustomerIp());
                    invoiceVerificationDto.setOpened(invoices.get(i).getIsViewByCustomer() != null && invoices.get(i).getIsViewByCustomer());
                    invoiceVerificationDto.setCountryIso(ticketEntity.getSenderCountryIso());
                    invoiceVerificationDto.setTrackingNumber(invoices.get(i).getTrackingNumber());
                    invoiceVerificationDto.setDeliveryStatus(invoices.get(i).getDeliveryStatus());
                    invoiceVerificationDto.setAssCallStatus(invoices.get(i).getAssCallStatus());
                    invoiceVerificationDto.setCallRecording(invoices.get(i).getCallRecording());
                    invoiceVerificationDto.setPaymentStatus(invoices.get(i).getPaymentStatus());
                    invoiceVerificationDto.setVerificationDate(invoices.get(i).getVerificationDate());
                    invoiceVerificationDto.setVerificationTime(invoices.get(i).getVerificationTime());
                    invoiceVerificationDto.setNumberOfPaymentAttempt(invoices.get(i).getNumberOfAttemptForPayment());
                    invoiceVerificationDto.setIsVerifiedByAdmin(invoices.get(i).getIsVerifiedByAdmin());
                    invoiceVerificationDto.setShippingCareer(invoices.get(i).getShippingCareer());
                } else {
                    UploadTicket uploadTicket = uploadTicketRepo.findByUniqueQueryId((invoices.get(i).getTicketId()));
                    Optional<Address> address = addressRepo.findByTicketId(invoices.get(i).getTicketId());
                    OrderDto orderDto = (OrderDto) orderController.getOrder(uploadTicket.getUniqueQueryId()).get("dtoList");
                    invoiceVerificationDto.setInvoiceId(invoices.get(i).getInviceId());
                    invoiceVerificationDto.setOrderDto(orderDto);
                    invoiceVerificationDto.setCloserName(user1.getFirstName() + " " + user1.getLastName());
                    invoiceVerificationDto.setCustomerName(uploadTicket.getFirstName() + " " + uploadTicket.getLastName());
                    invoiceVerificationDto.setCustomerEmail(uploadTicket.getEmail());
                    invoiceVerificationDto.setCustomerMobile(uploadTicket.getMobileNumber());
                    invoiceVerificationDto.setSaleDate(uploadTicket.getLastActionDate());
                    invoiceVerificationDto.setOrderAmount(orderDto.getTotalPayableAmount());
                    invoiceVerificationDto.setUniqueQueryId(uploadTicket.getUniqueQueryId());
                    invoiceVerificationDto.setPayment(null);
                    invoiceVerificationDto.setAddress(address.orElse(null));
                    invoiceVerificationDtos.add(invoiceVerificationDto);
                    invoiceVerificationDto.setIpAddress(invoices.get(i).getCustomerIp());
                    invoiceVerificationDto.setOpened(invoices.get(i).getIsViewByCustomer() != null && invoices.get(i).getIsViewByCustomer());
                    invoiceVerificationDto.setCountryIso(uploadTicket.getSenderCountryIso());
                    invoiceVerificationDto.setTrackingNumber(invoices.get(i).getTrackingNumber());
                    invoiceVerificationDto.setDeliveryStatus(invoices.get(i).getDeliveryStatus());
                    invoiceVerificationDto.setAssCallStatus(invoices.get(i).getAssCallStatus());
                    invoiceVerificationDto.setCallRecording(invoices.get(i).getCallRecording());
                    invoiceVerificationDto.setPaymentStatus(invoices.get(i).getPaymentStatus());
                    invoiceVerificationDto.setVerificationDate(invoices.get(i).getVerificationDate());
                    invoiceVerificationDto.setVerificationTime(invoices.get(i).getVerificationTime());
                    invoiceVerificationDto.setNumberOfPaymentAttempt(invoices.get(i).getNumberOfAttemptForPayment());
                    invoiceVerificationDto.setIsVerifiedByAdmin(invoices.get(i).getIsVerifiedByAdmin());
                    invoiceVerificationDto.setShippingCareer(invoices.get(i).getShippingCareer());
                }
            }
            return invoiceVerificationDtos;
        } else {
            List<Invoice> invoices = invoiceRepo.findAll();
            List<InvoiceVerificationDto> invoiceVerificationDtos = new ArrayList<>();
            for (int i = 0; i < invoices.size(); i++) {
                Optional<User> user1 = userRepo.findByUserId(invoices.get(i).getCreatedByUserId());
                InvoiceVerificationDto invoiceVerificationDto = new InvoiceVerificationDto();
                TicketEntity ticketEntity = ticketRepo.findByUniqueQueryId(invoices.get(i).getTicketId());
                if (ticketEntity != null) {
                    Optional<Address> address = addressRepo.findByTicketId(invoices.get(i).getTicketId());
                    OrderDto orderDto = (OrderDto) orderController.getOrder(ticketEntity.getUniqueQueryId()).get("dtoList");
                    invoiceVerificationDto.setInvoiceId(invoices.get(i).getInviceId());
                    invoiceVerificationDto.setOrderDto(orderDto);
                    invoiceVerificationDto.setCloserName(user1.map(value -> value.getFirstName() + " " + value.getLastName()).orElse("Stripe"));
                    invoiceVerificationDto.setCustomerName(ticketEntity.getSenderName());
                    invoiceVerificationDto.setCustomerEmail(ticketEntity.getSenderEmail());
                    invoiceVerificationDto.setCustomerMobile(ticketEntity.getSenderMobile());
                    invoiceVerificationDto.setSaleDate(ticketEntity.getLastActionDate());
                    invoiceVerificationDto.setOrderAmount(orderDto.getTotalPayableAmount());
                    invoiceVerificationDto.setUniqueQueryId(ticketEntity.getUniqueQueryId());
                    invoiceVerificationDto.setPayment(null);
                    invoiceVerificationDto.setAddress(address.orElse(null));
                    invoiceVerificationDtos.add(invoiceVerificationDto);
                    invoiceVerificationDto.setIpAddress(invoices.get(i).getCustomerIp());
                    invoiceVerificationDto.setOpened(invoices.get(i).getIsViewByCustomer() != null && invoices.get(i).getIsViewByCustomer());
                    invoiceVerificationDto.setCountryIso(ticketEntity.getSenderCountryIso());
                    invoiceVerificationDto.setTrackingNumber(invoices.get(i).getTrackingNumber());
                    invoiceVerificationDto.setDeliveryStatus(invoices.get(i).getDeliveryStatus());
                    invoiceVerificationDto.setAssCallStatus(invoices.get(i).getAssCallStatus());
                    invoiceVerificationDto.setCallRecording(invoices.get(i).getCallRecording());
                    invoiceVerificationDto.setVerificationDate(invoices.get(i).getVerificationDate());
                    invoiceVerificationDto.setVerificationTime(invoices.get(i).getVerificationTime());
                    invoiceVerificationDto.setPaymentStatus(invoices.get(i).getPaymentStatus());
                    invoiceVerificationDto.setNumberOfPaymentAttempt(invoices.get(i).getNumberOfAttemptForPayment());
                    invoiceVerificationDto.setIsVerifiedByAdmin(invoices.get(i).getIsVerifiedByAdmin());
                    invoiceVerificationDto.setShippingCareer(invoices.get(i).getShippingCareer());
                } else {
                    UploadTicket uploadTicket = uploadTicketRepo.findByUniqueQueryId((invoices.get(i).getTicketId()));
                    Optional<Address> address = addressRepo.findByTicketId(invoices.get(i).getTicketId());
                    OrderDto orderDto = (OrderDto) orderController.getOrder(uploadTicket.getUniqueQueryId()).get("dtoList");
                    invoiceVerificationDto.setInvoiceId(invoices.get(i).getInviceId());
                    invoiceVerificationDto.setOrderDto(orderDto);
                    invoiceVerificationDto.setCloserName(user1.map(value -> value.getFirstName() + " " + value.getLastName()).orElse("Stripe"));
                    invoiceVerificationDto.setCustomerName(uploadTicket.getFirstName() + " " + uploadTicket.getLastName());
                    invoiceVerificationDto.setCustomerEmail(uploadTicket.getEmail());
                    invoiceVerificationDto.setCustomerMobile(uploadTicket.getMobileNumber());
                    invoiceVerificationDto.setSaleDate(uploadTicket.getLastActionDate());
                    invoiceVerificationDto.setOrderAmount(orderDto.getTotalPayableAmount());
                    invoiceVerificationDto.setUniqueQueryId(uploadTicket.getUniqueQueryId());
                    invoiceVerificationDto.setPayment(null);
                    invoiceVerificationDto.setAddress(address.orElse(null));
                    invoiceVerificationDtos.add(invoiceVerificationDto);
                    invoiceVerificationDto.setIpAddress(invoices.get(i).getCustomerIp());
                    invoiceVerificationDto.setOpened(invoices.get(i).getIsViewByCustomer() != null && invoices.get(i).getIsViewByCustomer());
                    invoiceVerificationDto.setCountryIso(uploadTicket.getSenderCountryIso());
                    invoiceVerificationDto.setTrackingNumber(invoices.get(i).getTrackingNumber());
                    invoiceVerificationDto.setDeliveryStatus(invoices.get(i).getDeliveryStatus());
                    invoiceVerificationDto.setAssCallStatus(invoices.get(i).getAssCallStatus());
                    invoiceVerificationDto.setCallRecording(invoices.get(i).getCallRecording());
                    invoiceVerificationDto.setVerificationDate(invoices.get(i).getVerificationDate());
                    invoiceVerificationDto.setVerificationTime(invoices.get(i).getVerificationTime());
                    invoiceVerificationDto.setPaymentStatus(invoices.get(i).getPaymentStatus());
                    invoiceVerificationDto.setNumberOfPaymentAttempt(invoices.get(i).getNumberOfAttemptForPayment());
                    invoiceVerificationDto.setIsVerifiedByAdmin(invoices.get(i).getIsVerifiedByAdmin());
                    invoiceVerificationDto.setShippingCareer(invoices.get(i).getShippingCareer());
                }
            }
            return invoiceVerificationDtos;
        }
    }


    //Processing manual payment
    @PostMapping("/processPayment")
    public String processPayment(@RequestBody Map<String, String> paymentData) throws com.avanse.core.exception.TechnicalException, BussinessException, MessagingException {
        // Extract data from the map
        String amount = paymentData.get("amount");
        String currency = paymentData.get("currency");
        String paymentWindow = paymentData.get("paymentWindow");
        String transactionId = paymentData.get("transectionid");
        String uniqueQueryId = paymentData.get("uniqueQueryId");
        Payment payment = new Payment();
        Invoice invoice = invoiceRepo.findByTicketId(uniqueQueryId);
        TicketEntity ticketEntity = ticketRepo.findByUniqueQueryId(uniqueQueryId);
        if (ticketEntity != null) {
            payment.setPaymentStatus("paid");
            payment.setPaymentIntentId(transactionId);
            payment.setInvoiceId(invoice.getInviceId().toString());
            payment.setAmount(Long.parseLong(amount));
            payment.setPaymentDate(LocalDateTime.now());
            payment.setCurrency(currency);
            payment.setPaymentWindow(paymentWindow);
            payment.setCustomerEmail(ticketEntity.getSenderEmail());
            payment.setTicketId(invoice.getTicketId());
            orderConfirmation(orderService.getOrder(ticketEntity.getUniqueQueryId()), ticketEntity, addressRepo.findByTicketId(ticketEntity.getUniqueQueryId()).get());
            paymentRepo.save(payment);
            invoice.setInviceStatus("paid");
            invoice.setPaymentStatus("paid");
            invoiceRepo.save(invoice);
            ticketEntity.setTicketstatus("Sale");
            ticketEntity.setLastActionDate(LocalDate.now());
            ticketRepo.save(ticketEntity);
        } else {
            UploadTicket uploadTicket = uploadTicketRepo.findByUniqueQueryId(uniqueQueryId);
            payment.setPaymentStatus("paid");
            payment.setPaymentIntentId(transactionId);
            payment.setInvoiceId(invoice.getInviceId().toString());
            payment.setAmount(Long.parseLong(amount));
            payment.setPaymentDate(LocalDateTime.now());
            payment.setCurrency(currency);
            payment.setPaymentWindow(paymentWindow);
            payment.setCustomerEmail(uploadTicket.getEmail());
            payment.setTicketId(invoice.getTicketId());
            orderConfirmationForUploaded(orderService.getOrder(uploadTicket.getUniqueQueryId()), uploadTicket, addressRepo.findByTicketId(uploadTicket.getUniqueQueryId()).get());
            paymentRepo.save(payment);
            invoice.setInviceStatus("paid");
            invoice.setPaymentStatus("paid");
            invoiceRepo.save(invoice);
            uploadTicket.setTicketstatus("Sale");
            uploadTicket.setLastActionDate(LocalDate.now());
            uploadTicketRepo.save(uploadTicket);
        }
        return "Payment processed successfully!";
    }

    //order confirmation email for TicketEntity
    public void orderConfirmation(OrderDto orderDto, TicketEntity ticketEntity, Address address) throws MessagingException {
        template.convertAndSend("/topic/invoice/paid/", address);

        Map<String, Object> templateModel = new HashMap<>();
        Map<String, String> address1 = new HashMap<>();
        address1.put("houseNumber", address.getHouseNumber());
        address1.put("landmark", address.getLandmark());
        address1.put("city", address.getCity());
        address1.put("state", address.getState());
        address1.put("country", address.getCountry());
        address1.put("zipCode", address.getZipCode());
        address1.put("name", address.getFirstName() == null ? ticketEntity.getSenderName() : address.getFirstName());
        address1.put("mobile", address.getPhoneNumber() == null ? ticketEntity.getSenderMobile() : address.getPhoneNumber());
        templateModel.put("address", address1);
        // Customer information
        Map<String, String> customer = new HashMap<>();
        customer.put("name", ticketEntity.getSenderName());
        customer.put("email", ticketEntity.getSenderEmail());
        customer.put("mobile", ticketEntity.getSenderMobile());
        customer.put("address", ticketEntity.getSenderAddress());
        templateModel.put("customer", customer);

        // Company information
        Map<String, String> company = new HashMap<>();
        company.put("name", "RDVISION");
        company.put("email", "contact@example.com");
        company.put("country", "India");
        templateModel.put("company", company);

        // Products
        List<Map<String, Object>> products = new ArrayList<>();
        for (int i = 0; i < orderDto.getProductOrders().size(); i++) {
            ProductDto product = orderDto.getProductOrders().get(i).getProduct().get(0);
            Product product1 = productRepo.findByProductId(product.getProductId());
            products.add(Map.of(
                    "name", product.getName(),
                    "image", "https://rdvision.in/images/getProductImage/" + product1.getProductId(),
                    "description", product.getTreatment(),
                    "quantity", orderDto.getProductOrders().get(i).getQuantity(),
                    "price", orderDto.getProductOrders().get(i).getTotalAmount()

            ));
        }
        templateModel.put("products", products);
        templateModel.put("currency", orderDto.getProductOrders().get(0).getCurrency());
        templateModel.put("total_price", orderDto.getTotalPayableAmount());
        emailService.sendOrderConfirmation(ticketEntity.getSenderEmail(), "Order Confirmation", templateModel);

    }

    //order confirmation email for Uplaoded tickets
    public void orderConfirmationForUploaded(OrderDto orderDto, UploadTicket uploadedTicket, Address address) throws MessagingException {
        template.convertAndSend("/topic/invoice/paid/", address);

        Map<String, Object> templateModel = new HashMap<>();
        Map<String, String> address1 = new HashMap<>();
        address1.put("houseNumber", address.getHouseNumber());
        address1.put("landmark", address.getLandmark());
        address1.put("city", address.getCity());
        address1.put("state", address.getState());
        address1.put("country", address.getCountry());
        address1.put("zipCode", address.getZipCode());
        address1.put("name", address.getFirstName() == null ? uploadedTicket.getFirstName() : address.getFirstName());
        address1.put("mobile", address.getPhoneNumber() == null ? uploadedTicket.getMobileNumber() : address.getPhoneNumber());
        templateModel.put("address", address1);
        // Customer information
        Map<String, String> customer = new HashMap<>();
        customer.put("name", uploadedTicket.getFirstName());
        customer.put("email", uploadedTicket.getEmail());
        customer.put("mobile", uploadedTicket.getMobileNumber());
        customer.put("address", uploadedTicket.getSenderAddress());
        templateModel.put("customer", customer);

        // Company information
        Map<String, String> company = new HashMap<>();
        company.put("name", "RDVISION");
        company.put("email", "contact@example.com");
        company.put("country", "India");
        templateModel.put("company", company);

        // Products
        List<Map<String, Object>> products = new ArrayList<>();
        for (int i = 0; i < orderDto.getProductOrders().size(); i++) {
            ProductDto product = orderDto.getProductOrders().get(i).getProduct().get(0);
            Product product1 = productRepo.findByProductId(product.getProductId());
            products.add(Map.of(
                    "name", product.getName(),
                    "image", "https://rdvision.in/images/getProductImage/" + product1.getProductId(),
                    "description", product.getTreatment(),
                    "quantity", orderDto.getProductOrders().get(i).getQuantity(),
                    "price", orderDto.getProductOrders().get(i).getTotalAmount()

            ));
        }
        templateModel.put("products", products);
        templateModel.put("currency", orderDto.getProductOrders().get(0).getCurrency());
        templateModel.put("total_price", orderDto.getTotalPayableAmount());
        emailService.sendOrderConfirmation(uploadedTicket.getEmail(), "Order Confirmation", templateModel);

    }


     @GetMapping("/verifyInvoiceByAdmin/{invoiceId}")
    public void verifyInvoiceByAdmin(@PathVariable int invoiceId) throws com.avanse.core.exception.TechnicalException, GeneralSecurityException, IOException, BussinessException {
        Invoice invoice=invoiceRepo.findByInviceId(invoiceId);
        invoice.setIsVerifiedByAdmin(true);
        inventorySheetService.writeCustomerOrdersToSheet(invoice.getTicketId());
        invoiceRepo.save(invoice);

     }

     @PostMapping("/addCareer/{invoiceId}")
    public void addCareerToInvoices(@RequestParam String careerName,@PathVariable int invoiceId){
        Invoice invoice=invoiceRepo.findByInviceId(invoiceId);
        invoice.setShippingCareer(careerName);
        invoiceRepo.save(invoice);
     }

}
