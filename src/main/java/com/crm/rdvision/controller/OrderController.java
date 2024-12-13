package com.crm.rdvision.controller;

import com.crm.rdvision.Exception.BussinessException;
import com.crm.rdvision.common.EndPointReference;
import com.crm.rdvision.common.PaymentStatus;
import com.crm.rdvision.dto.OrderDto;
import com.crm.rdvision.dto.ProductDto;
import com.crm.rdvision.dto.ProductOrderDto;
import com.crm.rdvision.dto.SuccessResponse;
import com.crm.rdvision.entity.*;
import com.crm.rdvision.repository.*;
import com.crm.rdvision.service.OrderService;
import com.crm.rdvision.utility.Constants;
import jakarta.transaction.Transactional;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@CrossOrigin
@RequestMapping("/order/")
public class OrderController {
    @Autowired
    OrderRepo orderRepo;
    @Autowired
    ProductOrderRepo productOrderRepo;
    @Autowired
    ProductRepo productRepo;
    @Autowired
    ModelMapper modelMapper;
    @Autowired
    OrderService orderService;
    @Autowired
    AddressRepo addressRepo;
    @Autowired
    TicketRepo ticketRepo;
    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);
    @Transactional
    @PostMapping(EndPointReference.ADD_TO_ORDER)
    public Map<String, Object> addToOrder(@RequestBody OrderDto orderDto) throws com.avanse.core.exception.TechnicalException, BussinessException {
        logger.info("Add to Order Called");
        Map<String, Object> map = new HashMap<>();
        OrderItem order=null;
        Double totalAmount;
        ProductOrder productOrder=null;
        try {
            order=orderRepo.findByTicketId(orderDto.getTicketId());
            if(null==order){
                logger.info("Order is null of given ticket id {}",orderDto.getTicketId());
                order=orderRepo.save(mapToOrderItem(orderDto));
                order.setPaymentStatus(PaymentStatus.PENDING);

            }
            Integer orderId=order.getOrderId();
            productOrder=productOrderRepo.findByProductIdAndOrderId(orderDto.getProductId(),orderId);
            Product product=productRepo.getById(orderDto.getProductId());
            if (null==productOrder){
                logger.info("product Order is null of given ticket id {}",orderDto.getTicketId());
                ProductOrder productOrder1=new ProductOrder();
                productOrder1.setOrderId(orderId);
                productOrder1.setProductId(orderDto.getProductId());
                productOrder1.setQuantity(orderDto.getQuantity());
                productOrder1.setTotalAmount(orderDto.getPrice());
                productOrder1.setCurrency(orderDto.getCurrency());
                String codeName =generateProductCode(product.getName());
                productOrder1.setProductCode(codeName+"."+orderDto.getQuantity()+product.getPackagingType().charAt(0)+".pack" + "."+product.getBrand());
                productOrderRepo.save(productOrder1);
            }
            else {
                productOrder.setOrderId(orderId);
                productOrder.setProductId(orderDto.getProductId());
                productOrder.setQuantity(orderDto.getQuantity());
                productOrder.setTotalAmount(orderDto.getPrice());
                productOrderRepo.save(productOrder);
            }

        } catch (Exception e) {
            throw new com.avanse.core.exception.TechnicalException(Constants.TECHNICAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR, Constants.SPACE);
        }
        map.put(Constants.SUCCESS, new SuccessResponse(Constants.SUCCESS));
        map.put(Constants.ERROR, null);

        return map;
    }
    public static String generateProductCode(String input) {
        String[] parts = input.split(" ");  // Split by spaces
        StringBuilder result = new StringBuilder();

        // Loop through the parts and take the first letter of each word (except the last one)
        for (int i = 0; i < parts.length - 1; i++) {
            result.append(parts[i].substring(0, 1).toLowerCase());  // Add first letter of each part
        }

        // Extract the numeric part (strength) from the last element
        String strength = parts[parts.length - 1].replaceAll("[^0-9]", "");  // Remove any non-numeric characters

        // Append the numeric strength to the result
        result.append(strength);

        return result.toString();
    }
    @GetMapping(EndPointReference.GET_ORDER)
    public Map<String, Object> getOrder(@PathVariable String ticketId)
            throws com.avanse.core.exception.TechnicalException, BussinessException {
        logger.info("Get Order by Ticket Id Called, ticketId: {}", ticketId);
        Map<String, Object> map = new HashMap<>();
        OrderDto orderDto = new OrderDto();

        try {
            // Fetch the order by ticket ID
            OrderItem order = orderRepo.findByTicketId(ticketId);
            if (order == null) {
                logger.warn("No order found for ticketId: {}", ticketId);
                map.put(Constants.ERROR, "No order found for the given ticket ID");
                return map;
            }

            logger.info("Order found, orderId: {}", order.getOrderId());

            // Fetch the product orders by order ID
            List<ProductOrder> productOrders = productOrderRepo.findAllByOrderId(order.getOrderId());
            orderDto = modelMapper.map(order, OrderDto.class);

            if (orderDto.getProductOrders() == null) {
                orderDto.setProductOrders(new ArrayList<>());
                logger.info("Initialized empty productOrders list in orderDto");
            }

            // Iterate over product orders and map them to DTOs
            for (ProductOrder productOrder : productOrders) {
                ProductOrderDto productOrderDto = modelMapper.map(productOrder, ProductOrderDto.class);
                Product product=productRepo.getById(productOrder.getProductId());
                ProductDto productDto = modelMapper.map(product, ProductDto.class);

                if (productOrderDto.getProduct() == null) {
                    productOrderDto.setProduct(new ArrayList<>());
                    logger.info("Initialized empty product list in productOrderDto");
                }
                productOrderDto.getProduct().add(productDto);
                logger.info("Added productDto to productOrderDto, productId: {}", productDto.getProductId());

                orderDto.getProductOrders().add(productOrderDto);
            }

            // Calculate the total payable amount
            Double totalAmount = productOrderRepo.getTotalAmountSumByOrderId(order.getOrderId());
            if (totalAmount != null) {
                orderDto.setTotalPayableAmount(totalAmount);
                logger.info("Total payable amount calculated: {}", totalAmount);
            } else {
                orderDto.setTotalPayableAmount(0);
                logger.warn("No total amount found, setting totalPayableAmount to 0");
            }

        } catch (Exception e) {
            logger.error("Error occurred while processing order for ticketId: {}", ticketId, e);
            map.put(Constants.ERROR, "An error occurred while fetching the order.");
            return map;
        }

        // Prepare the response
        logger.info("Successfully provided  order for ticketId: {}", ticketId);
        map.put(Constants.DTO_LIST, orderDto);
        map.put(Constants.ERROR, null);
        map.put(Constants.SUCCESS, new SuccessResponse(Constants.SUCCESS));

        return map;
    }



    @GetMapping(EndPointReference.GET_ALL_ORDER)
    public Map<String, Object> getAllOrder() throws com.avanse.core.exception.TechnicalException, BussinessException {
        logger.info("Get All Address Called");
        Map<String, Object> map = new HashMap<>();

        try {
            List<OrderItem> orders = orderRepo.findAll();
            List<OrderDto> orderDtoList = new ArrayList<>();

            for (OrderItem order : orders) {
                List<ProductOrder> productOrders = productOrderRepo.findAllByOrderId(order.getOrderId());
                OrderDto orderDto = modelMapper.map(order, OrderDto.class);

                if (orderDto.getProductOrders() == null) {
                    orderDto.setProductOrders(new ArrayList<>());
                }

                for (ProductOrder productOrder : productOrders) {
                    ProductOrderDto productOrderDto = modelMapper.map(productOrder, ProductOrderDto.class);
                    ProductDto productDto = modelMapper.map(productRepo.getById(productOrder.getProductId()), ProductDto.class);

                    if (productOrderDto.getProduct() == null) {
                        productOrderDto.setProduct(new ArrayList<>());
                    }
                    productOrderDto.getProduct().add(productDto);

                    orderDto.getProductOrders().add(productOrderDto);
                }

                double payAmount = productOrderRepo.getTotalAmountSumByOrderId(order.getOrderId());
                orderDto.setTotalPayableAmount(payAmount);
                orderDtoList.add(orderDto);
            }

            map.put(Constants.DTO_LIST, orderDtoList);
            map.put(Constants.ERROR, null);
            map.put(Constants.SUCCESS, new SuccessResponse(Constants.SUCCESS));

        } catch (Exception e) {
            // Handle exceptions and add proper error responses
            map.put(Constants.ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
            map.put(Constants.SUCCESS, null);
        }

        return map;
    }

    @GetMapping("/getOrderDetails/{ticketId}")
    public Map<String,Object> getOrderDetailsByStatus(@PathVariable String ticketId) throws com.avanse.core.exception.TechnicalException, BussinessException {
        OrderDto orderDto =orderService.getOrder(ticketId);
        Optional<Address> address =addressRepo.findByTicketId(ticketId);
        TicketEntity ticketEntity =ticketRepo.findByUniqueQueryId(ticketId);
        Map<String,Object> map =new HashMap<>();
        map.put("orderDetails",orderDto);
        map.put("addresss",address);
        map.put("ticketDetail",ticketEntity);
        return map;
    }

    @DeleteMapping("/deleteProductOrder/{productOrderId}")
    public String deleteProductOrder(@PathVariable int productOrderId){
         productOrderRepo.deleteById(productOrderId);
        return "deleted";
    }

    OrderItem mapToOrderItem(OrderDto orderDto){
        OrderItem orderItem=new OrderItem();
        orderItem.setTicketId(orderDto.getTicketId());
        orderItem.setDate(new Date());
        orderItem.setUserId(orderDto.getUserId());
        return orderItem;
    }
    ProductOrder mapToProductOrder(OrderDto orderDto, Integer orderId, Double price){
        ProductOrder productOrder=new ProductOrder();
        productOrder.setOrderId(orderId);
        productOrder.setProductId(orderDto.getProductId());
        productOrder.setQuantity(orderDto.getQuantity());
        productOrder.setTotalAmount(price);
        return productOrder;
    }

    @PostMapping("/addtrackingnumber")
    public ResponseEntity<?> addTrackingNumberToOrder(@RequestBody Map<String,String> object){
        OrderItem orderItem =orderRepo.findByTicketId(object.get("ticketId"));
        orderItem.setTrackingNumber(object.get("trackingNumber"));
        return ResponseEntity.ok("Tracking Number Updated");
    }

}
