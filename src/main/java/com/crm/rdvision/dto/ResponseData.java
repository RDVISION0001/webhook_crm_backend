package com.crm.rdvision.dto;

import com.crm.rdvision.utility.CustomStringDeserializer;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class ResponseData {
    @JsonProperty("UNIQUE_QUERY_ID")
    @JsonDeserialize(using = CustomStringDeserializer.class)
    private String uniqueQueryId;
    @JsonProperty("QUERY_TYPE")
    private String queryType;
    @JsonProperty("QUERY_TIME")
    private String queryTime;
    @JsonProperty("SENDER_NAME")
    private String senderName;
    @JsonProperty("SENDER_MOBILE")
    private String senderMobile;
    @JsonProperty("SENDER_EMAIL")
    private String senderEmail;
    @JsonProperty("SUBJECT")
    private String subject;
    @JsonProperty("RECEIVER_CATALOG")
    private String response_catalog;
    @JsonProperty("SENDER_COMPANY")
    private String senderCompany;
    @JsonProperty("SENDER_ADDRESS")
    private String senderAddress;
    @JsonProperty("SENDER_CITY")
    private String senderCity;
    @JsonProperty("SENDER_STATE")
    private String senderState;
    @JsonProperty("SENDER_PINCODE")
    private String senderPincode;
    @JsonProperty("SENDER_COUNTRY_ISO")
    private String senderCountryIso;
    @JsonProperty("SENDER_MOBILE_ALT")
    private String senderMobileAlt;
    @JsonProperty("SENDER_PHONE")
    private String senderPhone;
    @JsonProperty("SENDER_PHONE_ALT")
    private String senderPhoneAlt;
    @JsonProperty("SENDER_EMAIL_ALT")
    private String senderEmailAlt;
    @JsonProperty("QUERY_PRODUCT_NAME")
    private String queryProductName;
    @JsonProperty("QUERY_MESSAGE")
    private String queryMessage;
    @JsonProperty("QUERY_MCAT_NAME")
    private String queryMcatName;
    @JsonProperty("CALL_DURATION")
    private String callDuration;
    @JsonProperty("RECEIVER_MOBILE")
    private String receiverMobile;
}
