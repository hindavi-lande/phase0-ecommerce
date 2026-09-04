package com.example.ecommerce.product.dto;

import com.example.ecommerce.product.Product;
import com.example.ecommerce.product.ProductStatus;
import java.math.BigDecimal;
import java.util.UUID;

public record ProductResponse(
        UUID id,
        String name,
        String sku,
        BigDecimal price,
        Integer stock,
        ProductStatus status,
        UUID categoryId,
        String description) {

    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getSku(),
                product.getPrice(),
                product.getStock(),
                product.getStatus(),
                product.getCategory() != null ? product.getCategory().getId() : null,
                product.getDescription());
    }
}
