package com.example.ecommerce.order.dto;

import com.example.ecommerce.order.OrderStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public record OrderRequest(
        @NotBlank(message = "customerName is required")
        @Size(max = 200, message = "customerName must be at most 200 characters")
        String customerName,

        @NotNull(message = "productId is required")
        UUID productId,

        @NotNull(message = "quantity is required")
        @Min(value = 1, message = "quantity must be at least 1")
        Integer quantity,

        @NotNull(message = "unitPrice is required")
        @DecimalMin(value = "0.00", message = "unitPrice must not be negative")
        @Digits(integer = 17, fraction = 2, message = "unitPrice must have at most 2 decimal places")
        BigDecimal unitPrice,

        @NotNull(message = "status is required")
        OrderStatus status) {
}
