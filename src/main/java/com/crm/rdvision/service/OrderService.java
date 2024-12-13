package com.crm.rdvision.service;

import com.crm.rdvision.Exception.BussinessException;
import com.crm.rdvision.common.EndPointReference;
import com.crm.rdvision.controller.OrderController;
import com.crm.rdvision.dto.OrderDto;
import com.crm.rdvision.dto.ProductDto;
import com.crm.rdvision.dto.ProductOrderDto;
import com.crm.rdvision.dto.SuccessResponse;
import com.crm.rdvision.entity.OrderItem;
import com.crm.rdvision.entity.ProductOrder;
import com.crm.rdvision.repository.OrderRepo;
import com.crm.rdvision.repository.ProductOrderRepo;
import com.crm.rdvision.repository.ProductRepo;
import com.crm.rdvision.utility.Constants;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class OrderService {
    @Autowired
    OrderRepo orderRepo;
    @Autowired
    ProductOrderRepo productOrderRepo;
    @Autowired
    ProductRepo productRepo;
    @Autowired
    ModelMapper modelMapper;
    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);

    public OrderDto getOrder(@PathVariable String ticketId) throws com.avanse.core.exception.TechnicalException, BussinessException {
        logger.info("Get Order by Ticket Id Called");
        Map<String, Object> map = new HashMap<>();
        OrderDto orderDto = new OrderDto();

        // Fetch the order by ticket ID
        OrderItem order = orderRepo.findByTicketId(ticketId);
        if (order != null) {
            // Fetch the product orders by order ID
            List<ProductOrder> productOrders = productOrderRepo.findAllByOrderId(order.getOrderId());

            orderDto = modelMapper.map(order, OrderDto.class);

            if (orderDto.getProductOrders() == null) {
                orderDto.setProductOrders(new ArrayList<>());
            }

            // Iterate over product orders and map them to DTOs
            for (ProductOrder productOrder : productOrders) {
                ProductOrderDto productOrderDto = modelMapper.map(productOrder, ProductOrderDto.class);
                ProductDto productDto = modelMapper.map(productRepo.getById(productOrder.getProductId()), ProductDto.class);

                if (productOrderDto.getProduct() == null) {
                    productOrderDto.setProduct(new ArrayList<>());
                }
                productOrderDto.getProduct().add(productDto);

                orderDto.getProductOrders().add(productOrderDto);
            }

            // Calculate the total payable amount
            System.out.println(order.getOrderId());
            double payAmount = productOrderRepo.getTotalAmountSumByOrderId(order.getOrderId());
            orderDto.setTotalPayableAmount(payAmount);
        }

        // Prepare the response
        map.put(Constants.DTO_LIST, orderDto);
        map.put(Constants.ERROR, null);
        map.put(Constants.SUCCESS, new SuccessResponse(Constants.SUCCESS));

        return orderDto;
    }

}
