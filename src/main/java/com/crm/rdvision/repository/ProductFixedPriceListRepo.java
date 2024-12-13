package com.crm.rdvision.repository;

import com.crm.rdvision.entity.ProductFixedPriceList;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductFixedPriceListRepo extends JpaRepository<ProductFixedPriceList,String> {
    ProductFixedPriceList findByProductCode(String productCode);
}
