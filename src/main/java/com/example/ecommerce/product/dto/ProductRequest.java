package com.example.ecommerce.product.dto;

import com.example.ecommerce.product.ProductStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public record ProductRequest(
        @NotBlank(message = "name is required")
        @Size(max = 200, message = "name must be at most 200 characters")
        String name,

        @NotBlank(message = "sku is required")
        @Size(max = 64, message = "sku must be at most 64 characters")
        String sku,

        @NotNull(message = "price is required")
        @DecimalMin(value = "0.00", message = "price must not be negative")
        @Digits(integer = 17, fraction = 2, message = "price must have at most 2 decimal places")
        BigDecimal price,

        @NotNull(message = "stock is required")
        @Min(value = 0, message = "stock must not be negative")
        Integer stock,

        @NotNull(message = "status is required")
        ProductStatus status,

        UUID categoryId) {

    public ProductRequest(String name, String sku, BigDecimal price, Integer stock, ProductStatus status) {
        this(name, sku, price, stock, status, null);
    }
}
