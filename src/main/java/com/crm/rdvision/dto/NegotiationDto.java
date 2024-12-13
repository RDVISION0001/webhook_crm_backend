package com.crm.rdvision.dto;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Data
public class NegotiationDto {
    private String queryTime;
    private LocalDate uploadDate;
    private String uniqueQueryId;
    private String mobileNumber;
    private String senderMobile;
    private String email;
    private String senderEmail;
    private String ticketstatus;
    private LocalDateTime followUpDateTime;
    private String senderName;
    private String firstName;
    private String lastName;
    private String senderCountryIso;
    private String queryProductName;
    private String productEnquiry;
    private String comment;
    private LocalDate lastActionDate;
    private String deliveryStatus;
    private String recordingFile;
    private String trackingNumber;

}
