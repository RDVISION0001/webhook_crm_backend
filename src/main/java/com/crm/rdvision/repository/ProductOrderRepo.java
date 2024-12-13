package com.crm.rdvision.repository;

import com.crm.rdvision.entity.ProductOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductOrderRepo extends JpaRepository<ProductOrder,Integer> {
    List<ProductOrder> findAllByOrderId(Integer orderId);
    ProductOrder findByProductIdAndOrderId(Integer productId,Integer orderId);
    @Query("SELECT SUM(po.totalAmount) FROM ProductOrder po WHERE po.orderId = ?1")
    Double getTotalAmountSumByOrderId(Integer orderId);
}
