package com.example.ecommerce.order.dto;

import com.example.ecommerce.order.Order;
import com.example.ecommerce.order.OrderStatus;
import java.math.BigDecimal;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        String customerName,
        UUID productId,
        Integer quantity,
        BigDecimal unitPrice,
        OrderStatus status) {

    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getCustomerName(),
                order.getProduct().getId(),
                order.getQuantity(),
                order.getUnitPrice(),
                order.getStatus());
    }
}
