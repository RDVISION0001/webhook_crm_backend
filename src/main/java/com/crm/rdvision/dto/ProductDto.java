package com.crm.rdvision.dto;

import com.crm.rdvision.entity.ProductImages;
import jakarta.persistence.Column;
import jakarta.persistence.Lob;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class ProductDto {
    private Integer productId;
    private String name;
    private String composition;
    private String brand;
    private String treatment;
    private String packagingSize;
    private String productCode;
    private String packagingType;
    private String strength;
    private String price;
    private String unit;
    private String description;
    private List<ProductImages> images;
    private String productVideo;
    private String bruchureLink;
}
