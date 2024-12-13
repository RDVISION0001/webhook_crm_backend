package com.crm.rdvision.controller;

import com.crm.rdvision.Exception.BussinessException;
import com.crm.rdvision.common.EndPointReference;
import com.crm.rdvision.dto.OrderDto;
import com.crm.rdvision.dto.ProductDto;
import com.crm.rdvision.dto.SuccessResponse;
import com.crm.rdvision.entity.*;
import com.crm.rdvision.repository.*;
import com.crm.rdvision.service.EmailService;
import com.crm.rdvision.service.OrderService;
import com.crm.rdvision.utility.Constants;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Charge;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import jakarta.mail.MessagingException;
import org.apache.commons.math3.analysis.function.Add;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;
import com.crm.rdvision.common.PaymentStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static com.crm.rdvision.controller.OrderController.generateProductCode;


@RestController
@CrossOrigin
@RequestMapping("/stripe")
public class StripeWebhookController {

    private static final String STRIPE_WEBHOOK_SECRET = "whsec_LxTxtegHnVP0rkKNHQyXNRqzWIatdSnG";
//    private static final String STRIPE_WEBHOOK_SECRET = "whsec_zyUHhymUVWAKPLlA8WH9HtCjT0vF05jb";
    @Autowired
    InvoiceRepo invoiceRepo;
    @Autowired
    EmailService emailService;
    @Autowired
    OrderService orderService;
    @Autowired
    AddressRepo addressRepo;
    @Autowired
    SimpMessagingTemplate template;
    @Autowired
    ProductRepo productRepo;
    @Autowired
    ProductFixedPriceListRepo productFixedPriceListRepo;
    @Autowired
    private ModelMapper modelMapper;
    @Autowired
    PaymentRepository paymentRepository;
    @Autowired
    TicketRepo ticketRepo;
    @Autowired
    OrderRepo orderRepo;
    @Autowired
    ProductOrderRepo productOrderRepo;

    private static final Logger logger = LoggerFactory.getLogger(StripeWebhookController.class);

    @PostMapping(EndPointReference.Stripe_webhook_Call)
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String signature) throws com.avanse.core.exception.TechnicalException, MessagingException, BussinessException {
        Event event;

        try {
            // Verify the event by checking its signature
            event = Webhook.constructEvent(payload, signature, STRIPE_WEBHOOK_SECRET);
        } catch (SignatureVerificationException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Webhook signature verification failed.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid payload.");
        }

        // Log the event type and handle it
        System.out.println("Received event type: " + event.getType());
        handleEvent(event);

        return ResponseEntity.ok("Webhook received");
    }

    private void handleEvent(Event event) throws com.avanse.core.exception.TechnicalException, MessagingException, BussinessException {
        switch (event.getType()) {
            case "checkout.session.completed":
                handleCheckoutSessionCompleted(event);
                break;
            case "checkout.session.async_payment_succeeded":
                handleAsyncPaymentSucceeded(event);
                break;
            case "payment_intent.payment_failed":
                handleAsyncPaymentFailed(event);
                break;
            case "payment_intent.succeeded":
                handlePaymentSucceeded(event);
                break;
            case "payment_intent.processing":
                handlePaymentProcessing(event);
                break;
            case "charge.succeeded":
                handleChargeSucceeded(event);
                break;
            default:
                System.out.println("Unhandled event type: " + event.getType());
                break;
        }
    }

    private void handleCheckoutSessionCompleted(Event event) throws com.avanse.core.exception.TechnicalException, BussinessException, MessagingException {
        // Retrieve the session object from the event
        System.out.println("Payemnt session completed");
        Session session = (Session) event.getData().getObject();
        // Retrieve the invoiceId from the session metadata
        String ticketId = session.getMetadata().get("ticketId");
        String productCode=session.getMetadata().get("productCode");
        System.out.println("Payemnt session completed"+ticketId+","+productCode);
        Invoice invoice2=invoiceRepo.findByTicketId(ticketId);
        if(invoice2!=null){
            Session.CustomerDetails customerDetails= session.getCustomerDetails();
            Optional<Address> addresss=addressRepo.findByTicketId(ticketId);
            if(addresss.isEmpty()){
                Address address1=new Address();
                address1.setTicketId(ticketId);
                address1.setState(customerDetails.getAddress().getState());
                address1.setCountry(customerDetails.getAddress().getCountry());
                address1.setCity(customerDetails.getAddress().getCity());
                address1.setLandmark(customerDetails.getAddress().getLine1());
                address1.setZipCode(customerDetails.getAddress().getPostalCode());
                address1.setEmail(customerDetails.getEmail());
                address1.setFirstName(customerDetails.getName());
                address1.setPhoneNumber(customerDetails.getPhone());
                addressRepo.save(address1);
            }else{
                Address address=addresss.get();
                address.setTicketId(ticketId);
                address.setState(customerDetails.getAddress().getState());
                address.setCountry(customerDetails.getAddress().getCountry());
                address.setCity(customerDetails.getAddress().getCity());
                address.setLandmark(customerDetails.getAddress().getLine1());
                address.setZipCode(customerDetails.getAddress().getPostalCode());
                address.setEmail(customerDetails.getEmail());
                address.setFirstName(customerDetails.getName());
                address.setPhoneNumber(customerDetails.getPhone());
                addressRepo.save(address);
            }
            Invoice invoice=invoiceRepo.findByTicketId(ticketId);
            invoice.setInviceStatus(session.getPaymentStatus());

            TicketEntity ticketEntity =ticketRepo.findByUniqueQueryId(ticketId);
            Invoice invoice1 =invoiceRepo.findByTicketId(ticketId);
            OrderItem orderItem=orderRepo.findByTicketId(ticketId);
            invoice1.setInviceStatus("paid");
            invoice1.setPaymentStatus("paid");
            invoice1.setDeliveryStatus("Processing");
            orderItem.setPaymentStatus(PaymentStatus.COMPLETED);
            orderItem.setOrderPaidDate(LocalDateTime.now());
            ticketEntity.setLastActionDate(LocalDate.now());
            ticketEntity.setTicketstatus("Sale");
            orderRepo.save(orderItem);
            invoiceRepo.save(invoice1);
            ticketRepo.save(ticketEntity);
            Payment payment = new Payment();
            payment.setPaymentIntentId(session.getPaymentIntent()); // Set payment intent ID
            payment.setAmount(session.getAmountTotal()/100);            // Set total amount
            payment.setCurrency(session.getCurrency());             // Set currency
            payment.setCustomerEmail(session.getCustomerEmail());   // Set customer email
            payment.setPaymentStatus(session.getPaymentStatus());   // Set payment status
            payment.setInvoiceId(invoice.getInviceId().toString());
            payment.setTicketId(ticketId);// Set the invoice ID from metadata
            payment.setPaymentWindow("Stripe");
            paymentRepository.save(payment);
            template.convertAndSend("/topic/third_party_api/ticket/", payment);
            orderConfirmation(orderService.getOrder(ticketId),ticketEntity,addressRepo.findByTicketId(ticketId).get());
        }else{
            Session.CustomerDetails customerDetails= session.getCustomerDetails();
                Address address=new Address();
                address.setTicketId(ticketId);
                address.setState(customerDetails.getAddress().getState());
                address.setCountry(customerDetails.getAddress().getCountry());
                address.setCity(customerDetails.getAddress().getCity());
                address.setLandmark(customerDetails.getAddress().getLine1());
                address.setZipCode(customerDetails.getAddress().getPostalCode());
                address.setEmail(customerDetails.getEmail());
                address.setFirstName(customerDetails.getName());
                address.setPhoneNumber(customerDetails.getPhone());
              try{
                  addressRepo.save(address);
              }catch (Exception e){
                  System.out.println("Address Already Exist....");
              }
                TicketEntity ticketEntity =ticketRepo.findByUniqueQueryId(ticketId);
                ticketEntity.setLastActionDate(LocalDate.now());
                ticketEntity.setTicketstatus("Sale");
                ticketRepo.save(ticketEntity);
                Payment payment = new Payment();
                payment.setPaymentIntentId(session.getPaymentIntent()); // Set payment intent ID
                payment.setAmount(session.getAmountTotal()/100);            // Set total amount
                payment.setCurrency(session.getCurrency());             // Set currency
                payment.setCustomerEmail(session.getCustomerEmail());   // Set customer email
                payment.setPaymentStatus(session.getPaymentStatus());   // Set payment status
                payment.setPaymentWindow("Stripe");
                payment.setProductCode(productCode);
                payment.setTicketId(ticketId);
                Payment payment1=paymentRepository.save(payment);
                template.convertAndSend("/topic/third_party_api/ticket/", payment);
                orderConfirmationOfInquiry(productCode,ticketEntity);
                addToOrder(generateOrderDtoWithProductCodeAndPrice(productCode,payment1));

        }
    }

    public OrderDto generateOrderDtoWithProductCodeAndPrice(String productCode,Payment payment){
        OrderDto orderDto=new OrderDto();
        ProductFixedPriceList priceList=productFixedPriceListRepo.findByProductCode(productCode);
        orderDto.setQuantity(priceList.getQuantity());
        orderDto.setProductId(priceList.getProduct().getProductId());
        orderDto.setTicketId(payment.getTicketId());
        orderDto.setPrice(payment.getAmount());
        orderDto.setCurrency(payment.getCurrency());
        return orderDto;
    }

    public void addToOrder(OrderDto orderDto) throws com.avanse.core.exception.TechnicalException, BussinessException {
        logger.info("Add to Order Called");
        Map<String, Object> map = new HashMap<>();
        OrderItem order=null;
        Double totalAmount;
        ProductOrder productOrder=null;
        try {
            order=orderRepo.findByTicketId(orderDto.getTicketId());
            if(null==order){
                logger.info("Order is null of given ticket id {}",orderDto.getTicketId());
                order=orderRepo.save(mapToOrderItem(orderDto));
                order.setPaymentStatus(PaymentStatus.PENDING);

            }
            Integer orderId=order.getOrderId();
            productOrder=productOrderRepo.findByProductIdAndOrderId(orderDto.getProductId(),orderId);
            Product product=productRepo.getById(orderDto.getProductId());
            if (null==productOrder){
                logger.info("product Order is null of given ticket id {}",orderDto.getTicketId());
                ProductOrder productOrder1=new ProductOrder();
                productOrder1.setOrderId(orderId);
                productOrder1.setProductId(orderDto.getProductId());
                productOrder1.setQuantity(orderDto.getQuantity());
                productOrder1.setTotalAmount(orderDto.getPrice());
                productOrder1.setCurrency(orderDto.getCurrency());
                String codeName =generateProductCode(product.getName());
                productOrder1.setProductCode(codeName+"."+orderDto.getQuantity()+product.getPackagingType().charAt(0)+".pack" + "."+product.getBrand());
                productOrderRepo.save(productOrder1);
            }
            else {
                productOrder.setOrderId(orderId);
                productOrder.setProductId(orderDto.getProductId());
                productOrder.setQuantity(orderDto.getQuantity());
                productOrder.setTotalAmount(orderDto.getPrice());
                productOrderRepo.save(productOrder);
            }

        } catch (Exception e) {
            throw new com.avanse.core.exception.TechnicalException(Constants.TECHNICAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR, Constants.SPACE);
        }
        map.put(Constants.SUCCESS, new SuccessResponse(Constants.SUCCESS));
        map.put(Constants.ERROR, null);

        Invoice invoice = new Invoice();
        invoice.setTicketId(orderDto.getTicketId());
        invoice.setCreateDate(LocalDate.now());
        invoice.setInviceStatus("paid");
        invoice.setInviceStatus("paid");
        invoice.setTotalAmount(orderDto.getTotalPayableAmount());
        invoice.setCreateDate(LocalDate.now());
        invoice.setDeliveryStatus("processing");
        invoice.setCurrency(orderDto.getCurrency());
        invoice.setTotalAmount(orderDto.getTotalPayableAmount());
        invoice.setCurrency(orderDto.getProductOrders().get(0).getCurrency());
        invoice.setIsViewByCustomer(false);
        invoiceRepo.save(invoice);

    }

    OrderItem mapToOrderItem(OrderDto orderDto){
        OrderItem orderItem=new OrderItem();
        orderItem.setTicketId(orderDto.getTicketId());
        orderItem.setDate(new Date());
        orderItem.setUserId(orderDto.getUserId());
        return orderItem;
    }
    public void orderConfirmation(OrderDto orderDto, TicketEntity ticketEntity, Address address) throws MessagingException {
        // Preparing template model data
        template.convertAndSend("/topic/third_party_api/ticket/", ticketEntity);

        Map<String, Object> templateModel = new HashMap<>();
        // Customer information
        Map<String, String> customer = new HashMap<>();
        customer.put("name", ticketEntity.getSenderName());
        customer.put("email", ticketEntity.getSenderEmail());
        customer.put("mobile", ticketEntity.getSenderMobile());
        customer.put("address", ticketEntity.getSenderAddress());
        templateModel.put("customer", customer);

        Map<String,String> address1=new HashMap<>();
        address1.put("houseNumber",address.getHouseNumber());
        address1.put("landmark",address.getLandmark());
        address1.put("city",address.getCity());
        address1.put("state",address.getState());
        address1.put("country",address.getCountry());
        address1.put("zipCode",address.getZipCode());
        address1.put("name",address.getFirstName()==null?ticketEntity.getSenderName():address.getFirstName());
        address1.put("mobile",address.getPhoneNumber()==null?ticketEntity.getSenderMobile():address.getPhoneNumber());
        templateModel.put("address",address1);

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
            Product product1 =productRepo.findByProductId(product.getProductId());
            products.add(Map.of(
                    "name", product.getName(),
                    "image", product1.getImageListInByte().get(0),
                    "description", product.getTreatment(),
                    "quantity", orderDto.getProductOrders().get(i).getQuantity(),
                    "price", orderDto.getProductOrders().get(i).getTotalAmount()
            ));
        }
        templateModel.put("products", products);
        templateModel.put("total_price", orderDto.getTotalPayableAmount());
        templateModel.put("currency",orderDto.getProductOrders().get(0).getCurrency());
        emailService.sendOrderConfirmation(ticketEntity.getSenderEmail(),"Order Confirmation",templateModel);

    }

public void handleChargeSucceeded(Event event) {
    // Parse the charge object from the event
    Charge charge = (Charge) event.getData().getObject();
    System.out.println(charge.getMetadata().get("ticketId"));
    System.out.println(charge.getMetadata().get("productCode"));
}




    public void orderConfirmationOfInquiry(String productCode,TicketEntity ticket) throws MessagingException {
        ProductFixedPriceList priceList=productFixedPriceListRepo.findByProductCode(productCode);
        Product product=priceList.getProduct();
        Map<String, Object> templateModel = new HashMap<>();
        // Customer information
        Map<String, String> customer = new HashMap<>();
        customer.put("name", ticket.getSenderName());
        customer.put("email", ticket.getSenderEmail());
        customer.put("mobile", ticket.getSenderMobile());
        customer.put("address", ticket.getSenderAddress());
        templateModel.put("customer", customer);
        Address address =addressRepo.findByTicketId(ticket.getUniqueQueryId()).get();
        Map<String,String> address1=new HashMap<>();
        address1.put("houseNumber",address.getHouseNumber());
        address1.put("landmark",address.getLandmark());
        address1.put("city",address.getCity());
        address1.put("state",address.getState());
        address1.put("country",address.getCountry());
        address1.put("zipCode",address.getZipCode());
        address1.put("name",address.getFirstName()==null?ticket.getSenderName():address.getFirstName());
        address1.put("mobile",address.getPhoneNumber()==null?ticket.getSenderMobile():address.getPhoneNumber());
        templateModel.put("address",address1);

        // Company information
        Map<String, String> company = new HashMap<>();
        company.put("name", "Buymed24");
        company.put("email", "help@buymed24.com");
        company.put("country", "India");
        templateModel.put("company", company);

        // Products
        List<Map<String, Object>> products = new ArrayList<>();
        products.add(Map.of(
                "name", product.getName(),
                "image", product.getImageListInByte().get(0),
                "description", product.getTreatment(),
                "quantity", priceList.getQuantity(),
                "price", priceList.getPrice()
        ));
        templateModel.put("products", products);
        templateModel.put("total_price", priceList.getPrice());
        templateModel.put("currency",priceList.getCurrency());
        emailService.sendOrderConfirmation(ticket.getSenderEmail(),"Order Confirmation",templateModel);


    }

    private void handleAsyncPaymentSucceeded(Event event) {
        System.out.println("Async payment succeeded for session: " + event.getData().getObject().toString());
    }

    private void handleAsyncPaymentFailed(Event event) {
        // Retrieve the PaymentIntent object from the event data
        PaymentIntent paymentIntent = (PaymentIntent) event.getData().getObject();

        // Create a new Payment entity for the failed payment
        Payment payment = new Payment();
        payment.setPaymentIntentId(paymentIntent.getId());              // Set the payment intent ID
        payment.setAmount(paymentIntent.getAmountReceived());           // Amount attempted (may be zero)
        payment.setCurrency(paymentIntent.getCurrency());               // Currency of the payment
        payment.setCustomerEmail(paymentIntent.getReceiptEmail());      // Email of the customer
        payment.setPaymentStatus("failed");                             // Set the payment status to "failed"
        payment.setPaymentWindow("Stripe");
        payment.setPaymentDate(LocalDateTime.now());

        // Retrieve and set the invoice ID if available
        if (paymentIntent.getInvoice() != null) {
            payment.setInvoiceId(paymentIntent.getInvoice());           // Set invoice ID if present
        } else {
            payment.setInvoiceId("N/A");                                // Default value if no invoice ID
        }

        // Collect metadata from the PaymentIntent
        if (paymentIntent.getMetadata() != null && !paymentIntent.getMetadata().isEmpty()) {
            Map<String, String> metadata = paymentIntent.getMetadata();

            // Retrieve specific metadata values
            String ticketId = metadata.getOrDefault("ticketId", "N/A");
            String productCode = metadata.getOrDefault("productCode", "N/A");
            String invoiceId = metadata.getOrDefault("invoiceId", "N/A");
            // Set the retrieved values in the payment entity
            payment.setTicketId(ticketId);     // Assuming Payment has a ticketId field
            payment.setProductCode(productCode); // Assuming Payment has a productCode field

            // Log metadata details
            System.out.println("Metadata - ticketId: " + ticketId);
            System.out.println("Metadata - productCode: " + productCode);

            // Optionally, store the entire metadata as JSON
            payment.setProductCode(productCode);
            payment.setTicketId(ticketId);
            payment.setInvoiceId(invoiceId);
        } else {
            payment.setInvoiceId("No metadata provided");
            payment.setTicketId("N/A");
            payment.setProductCode("N/A");
        }

        // Log payment failure details
        System.out.println("Payment failed for Payment Intent ID: " + paymentIntent.getId());
        System.out.println("Amount Attempted: " + paymentIntent.getAmountReceived());
        System.out.println("Currency: " + paymentIntent.getCurrency());
        System.out.println("Customer Email: " + paymentIntent.getReceiptEmail());
        System.out.println("Payment Status: failed");

        // Save the payment entity
        paymentRepository.save(payment);
    }
    private void handlePaymentSucceeded(Event event) {
        PaymentIntent paymentIntent = (PaymentIntent) event.getData().getObject();
        logPaymentIntentDetails(paymentIntent);
    }
    private void handlePaymentProcessing(Event event) {
        PaymentIntent paymentIntent = (PaymentIntent) event.getData().getObject();
        System.out.println("Payment is processing: " + paymentIntent.getId());
    }
    private void logPaymentIntentDetails(PaymentIntent paymentIntent) {
        String transactionId = paymentIntent.getId();
        String latestChargeId = paymentIntent.getLatestCharge();
        String paymentMethod = paymentIntent.getPaymentMethod();
        Long amountReceived = paymentIntent.getAmountReceived();
        String currency = paymentIntent.getCurrency();
        String status = paymentIntent.getStatus();

    }
}
