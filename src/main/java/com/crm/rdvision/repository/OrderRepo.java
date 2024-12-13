package com.crm.rdvision.repository;

import com.crm.rdvision.entity.Invoice;
import com.crm.rdvision.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepo extends JpaRepository<OrderItem,Integer> {
    OrderItem findByTicketId(String ticketId);
    OrderItem findByTrackingNumber(String trackingNumber);
    @Query("SELECT COUNT(o) FROM OrderItem o WHERE o.orderStatus = 'Paid' AND o.trackingNumber IS NOT NULL")
    Long countOrderItemWhereOrderStatusIsPaidAndTrackingNumberIsNotNull();


}
