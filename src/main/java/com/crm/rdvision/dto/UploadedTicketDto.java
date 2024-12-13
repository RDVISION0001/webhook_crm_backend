package com.crm.rdvision.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UploadedTicketDto {
    private String firstName;
    private String lastName;
    private String email;
    private String mobileNumber;
    private String senderAddress;
    private String productEnquiry;
    private LocalDate uploadDate;
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
    private String ticketstatus;
    private String comment;
    private LocalDateTime followUpDateTime;
    private String uniqueQueryId;
    private String queryTime;
}
