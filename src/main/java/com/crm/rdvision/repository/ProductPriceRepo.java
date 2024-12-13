package com.crm.rdvision.repository;

import com.crm.rdvision.entity.Product;
import com.crm.rdvision.entity.ProductsPrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductPriceRepo extends JpaRepository<ProductsPrice,Long> {

    List<ProductsPrice> findByProduct(Product product);
    List<ProductsPrice> findByProductAndTicketId(Product product,String ticket);
}
