package com.crm.rdvision.dto;

import com.crm.rdvision.common.PaymentStatus;
import com.crm.rdvision.entity.ProductOrder;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Transient;

import java.util.Date;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class OrderDto {
    private Integer orderId;
    private String ticketId;
    private Integer userId;
    private Integer productId;
    private Integer quantity;
    private String currency;
    private double price;
    private Date date;
    private String trackingNumber;
    @Transient
    private List<ProductOrderDto> productOrders;
    private double TotalPayableAmount;
    private PaymentStatus paymentStatus;




}
