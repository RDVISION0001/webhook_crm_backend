package com.crm.rdvision.repository;

import com.crm.rdvision.entity.Product;
import com.crm.rdvision.entity.ProductImages;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductImageRepo extends JpaRepository<ProductImages,Long> {

    ProductImages findByImageId(int imageId);

    @Query("SELECT pi FROM ProductImages pi WHERE pi.product.productId = :productId ORDER BY pi.id ASC")
    List<ProductImages> findFirstImageByProductId(@Param("productId") int productId);

    @Query("SELECT i.product.id, i.imageId FROM ProductImages i")
    List<Object[]> findAllImageIdsGroupedByProduct();
}
