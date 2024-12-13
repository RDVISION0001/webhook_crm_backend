package com.crm.rdvision.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class InviceDto {
    private Integer inviceId;
    private String ticketId;
    private String inviceStatus;
    private String paymentStatus;
}
