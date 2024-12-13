package com.crm.rdvision.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
public class Invoice {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer inviceId;
    private String ticketId;
    private String inviceStatus;
    private Boolean isViewByCustomer;
    private String agreeStatus;
    private double quotedPrice;
    private LocalDate createDate;
    private LocalDate lastupdateDate;
    private int createdByUserId;
    private double totalAmount;
    private String currency;
    private String trackingNumber;
    private String deliveryStatus;
    private String callRecording;
    private String assCallStatus;
    private String paymentStatus;
    private boolean isVerified;
    private Boolean isVerifiedByAdmin;
    private String customerIp;
    private  int numberOfAttemptForPayment;
    private LocalDate verificationDate;
    private LocalTime verificationTime;
    private String shippingCareer;

}
