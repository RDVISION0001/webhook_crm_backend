package com.crm.rdvision.dto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class TicketResponseDto {
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
    private String ticketstatus;
    private String followUpDateTime;
    private String comment;
    // Add other fields similar to the JSON structure

}
