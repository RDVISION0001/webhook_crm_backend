package com.crm.rdvision.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
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
public class ProductFixedPriceList {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long priceId;

    @ManyToOne
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) // Ignore during serialization but allow during deserialization
    private Product product;
    private int quantity;
    private int price;
    private double pricePerPill;
    private String productCode;
    private String currency;
    private String unit;
}
