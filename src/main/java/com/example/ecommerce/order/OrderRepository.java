package com.example.ecommerce.order;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    boolean existsByProductId(UUID productId);
}
