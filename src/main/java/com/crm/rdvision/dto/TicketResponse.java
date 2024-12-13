package com.crm.rdvision.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class TicketResponse {
    @JsonProperty("CODE")
    private int code;
    @JsonProperty("STATUS")
    private String status;
    @JsonProperty("MESSAGE")
    private String message;
    @JsonProperty("TOTAL_RECORDS")
    private int totalRecords;
    @JsonProperty("RESPONSE")
    private Response[] response;

    // Getters and setters
}

