package com.crm.rdvision.dto;

import com.crm.rdvision.entity.Address;
import com.crm.rdvision.entity.Payment;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class InvoiceVerificationDto {
    private String closerName;
    private LocalDate saleDate;
    private String customerName;
    private OrderDto orderDto;
    private double orderAmount;
    private String customerMobile;
    private String customerEmail;
    private String uniqueQueryId;
    private  long invoiceId;
    private Payment payment;
    private Address address;
    private String ipAddress;
    private boolean isOpened;
    private String countryIso;
    private String trackingNumber;
    private String deliveryStatus;
    private String assCallStatus;
    private String callRecording;
    private String paymentStatus;
    private int numberOfPaymentAttempt;
    private LocalDate verificationDate;
    private LocalTime verificationTime;
    private Boolean isVerifiedByAdmin;
    private String shippingCareer;
}
