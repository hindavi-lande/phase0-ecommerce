package com.example.ecommerce.order;

import com.example.ecommerce.common.ResourceNotFoundException;
import com.example.ecommerce.order.dto.OrderRequest;
import com.example.ecommerce.order.dto.OrderResponse;
import com.example.ecommerce.product.Product;
import com.example.ecommerce.product.ProductService;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductService productService;

    public OrderService(OrderRepository orderRepository, ProductService productService) {
        this.orderRepository = orderRepository;
        this.productService = productService;
    }

    @Transactional
    public OrderResponse create(OrderRequest request) {
        Product product = productService.findOrThrow(request.productId());

        Order order = new Order(
                request.customerName(),
                product,
                request.quantity(),
                request.unitPrice(),
                request.status());

        return OrderResponse.from(orderRepository.save(order));
    }

    public OrderResponse get(UUID id) {
        return OrderResponse.from(findOrThrow(id));
    }

    public List<OrderResponse> list() {
        return orderRepository.findAll().stream()
                .map(OrderResponse::from)
                .toList();
    }

    @Transactional
    public OrderResponse update(UUID id, OrderRequest request) {
        Order order = findOrThrow(id);

        // Re-resolve the FK so an order can be moved to a different product.
        order.setProduct(productService.findOrThrow(request.productId()));
        order.setCustomerName(request.customerName());
        order.setQuantity(request.quantity());
        order.setUnitPrice(request.unitPrice());
        order.setStatus(request.status());

        return OrderResponse.from(orderRepository.save(order));
    }

    @Transactional
    public void delete(UUID id) {
        orderRepository.delete(findOrThrow(id));
    }

    private Order findOrThrow(UUID id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", id));
    }
}
