package com.crm.rdvision.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ProductDtoForInvoice {
    private Integer productId;
    private String name;
    private String brand;
    // private byte[] imageData; // Matches the type in your query
}

