package com.crm.rdvision.repository;

import com.crm.rdvision.dto.ProductDtoForInvoice;
import com.crm.rdvision.entity.Product;
import com.crm.rdvision.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepo extends JpaRepository<Product,Integer> {
    Product findByProductId(Integer productId);
    List<Product> findByNameContaining(String name);

    @Query("SELECT p FROM Product p WHERE LOWER(p.name) LIKE CONCAT('%', LOWER(:keyword), '%')")
    List<Product> findByNameContainsIgnoreCase(@Param("keyword") String keyword);

    @Query("SELECT new com.crm.rdvision.dto.ProductDtoForInvoice(p.productId, p.name, p.brand, " +
            "CASE WHEN pi.imageData IS NOT NULL THEN pi.imageData ELSE 'No image found' END) " +
            "FROM Product p " +
            "LEFT JOIN ProductImages pi ON pi.product = p AND pi.id = (SELECT MIN(subPi.id) FROM ProductImages subPi WHERE subPi.product = p)")
    List<ProductDtoForInvoice> findAllProductsWithSingleImage();







}
