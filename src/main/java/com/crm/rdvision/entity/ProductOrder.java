package com.crm.rdvision.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Transient;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
public class ProductOrder {
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Id
    private Integer productorderId;
    private Integer productId;
    private Integer orderId;
    private Integer quantity;
    private String currency;
    private Double totalAmount;
    private String productCode;



    // Constructor, getters, and setters
}