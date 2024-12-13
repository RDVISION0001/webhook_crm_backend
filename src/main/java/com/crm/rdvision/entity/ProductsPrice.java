package com.crm.rdvision.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ProductsPrice {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long priceId;
    private String unit;
    private int quantity;
    private int price;
    private String paymentLink;
    @ManyToOne
    private Product product;
    private String ticketId;
    private String currency;
}
