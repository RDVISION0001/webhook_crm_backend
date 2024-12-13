package com.crm.rdvision.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.cglib.core.Local;

import java.time.LocalDate;
import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
public class TicketEntity {
    @Id
    @GeneratedValue(generator = "custom-id")
    @GenericGenerator(name = "custom-id", strategy = "com.crm.rdvision.dto.CustomIdGenerator")
    private Integer id;
    private String uniqueQueryId;
    private String queryType;
    private String queryTime;
    private String senderName;
    private String senderMobile;
    private String senderEmail;
    private String subject;
    private String senderCompany;
    private String senderAddress;
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
    private LocalDate lastActionDate;
    private LocalDateTime followUpDateTime;
    private String trackingNumber;
    private String deliveryStatus;
    private String recordingFile;
    // Add other fields similar to the JSON structure

}
