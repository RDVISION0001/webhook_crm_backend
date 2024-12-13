package com.crm.rdvision.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class ReceivedData {
    @JsonProperty("CODE")
    private String code;
    @JsonProperty("STATUS")
    private String status;
    @JsonProperty("RESPONSE")
    private ResponseData response;
}
