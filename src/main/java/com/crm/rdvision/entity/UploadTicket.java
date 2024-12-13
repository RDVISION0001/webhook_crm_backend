package com.crm.rdvision.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UploadTicket {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long ticketId;
    private String firstName;
    private String lastName;
    private String email;
    private String mobileNumber;
    private String senderAddress;
    @Column(columnDefinition = "MEDIUMTEXT")
    private String productEnquiry;
    private LocalDate uploadDate;
    @Column(columnDefinition = "MEDIUMTEXT")
    private String subject;
    private String senderCompany;
    private String senderCity;
    private String senderState;
    private String senderPincode;
    private String senderCountryIso;
    private String senderMobileAlt;
    private String senderPhone;
    private String senderPhoneAlt;
    private String senderEmailAlt;
    private String queryProductName;
    private String queryMessage;
    private String queryMcatName;
    private String callDuration;
    private String receiverMobile;
    private Integer assigntoteam;
    private Integer assigntouser;
    private LocalDate assignDate;
    private String ticketstatus;
    private String comment;
    private LocalDateTime followUpDateTime;
    private String uniqueQueryId;
    private String queryTime;
    private String trackingNumber;
    private String deliveryStatus;
    private String recordingFile;
    private LocalDate lastActionDate;


}
