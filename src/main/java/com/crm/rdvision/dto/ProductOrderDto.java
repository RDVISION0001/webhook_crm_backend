package com.crm.rdvision.dto;

import com.crm.rdvision.entity.Product;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Transient;

import java.util.List;


@AllArgsConstructor
@NoArgsConstructor
@Data
public class ProductOrderDto {
    private Integer productorderId;
    private Integer productId;
    private Integer orderId;
    private Integer quantity;
    private Double totalAmount;
    private String currency;
    private List<ProductDto> product;



    // Constructor, getters, and setters
}