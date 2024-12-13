package com.crm.rdvision.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
public class Product {
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Id
    private Integer productId;

    private String name;
    private String composition;
    private String brand;
    private String treatment;
    private String packagingSize;
    private String productCode;
    private String packagingType;
    private String genericName;
    private String strength;
    private String price;
    private String unit;
    private String category;
    private String description;

    @Column(columnDefinition = "TEXT")
    private String productVideo;

    @Column(columnDefinition = "TEXT")
    private String bruchureLink;

    @OneToMany(mappedBy = "product")
    @JsonIgnore
    private List<ProductsPrice> prices;

    @OneToMany(mappedBy = "product")
    private List<ProductFixedPriceList> priceList;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private List<ProductImages> imageListInByte;

}
