package com.example.ecommerce.category.dto;

import jakarta.validation.constraints.Size;

public record CategoryUpdateRequest(
        @Size(max = 200, message = "name must be at most 200 characters")
        String name,

        @Size(max = 1000, message = "description must be at most 1000 characters")
        String description) {
}
