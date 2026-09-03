package com.example.ecommerce.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.ecommerce.common.ResourceNotFoundException;
import com.example.ecommerce.order.dto.OrderRequest;
import com.example.ecommerce.order.dto.OrderResponse;
import com.example.ecommerce.product.Product;
import com.example.ecommerce.product.ProductService;
import com.example.ecommerce.product.ProductStatus;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductService productService;

    @InjectMocks
    private OrderService orderService;

    private UUID productId;
    private Product product;
    private OrderRequest request;

    @BeforeEach
    void setUp() {
        productId = UUID.randomUUID();
        product = new Product("Wireless Mouse", "SKU-0001", new BigDecimal("29.99"), 100, ProductStatus.ACTIVE);
        request = new OrderRequest(
                "Ada Lovelace",
                productId,
                2,
                new BigDecimal("29.99"),
                OrderStatus.PENDING);
    }

    @Test
    void createResolvesForeignKeyAndPersists() {
        when(productService.findOrThrow(productId)).thenReturn(product);
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderResponse response = orderService.create(request);

        assertThat(response.customerName()).isEqualTo("Ada Lovelace");
        assertThat(response.quantity()).isEqualTo(2);
        assertThat(response.unitPrice()).isEqualByComparingTo("29.99");
        assertThat(response.status()).isEqualTo(OrderStatus.PENDING);
        verify(productService).findOrThrow(productId);
    }

    @Test
    void createFailsWhenProductMissing() {
        when(productService.findOrThrow(productId))
                .thenThrow(new ResourceNotFoundException("Product", productId));

        assertThatThrownBy(() -> orderService.create(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Product");

        verify(orderRepository, never()).save(any());
    }

    @Test
    void getThrowsWhenMissing() {
        UUID id = UUID.randomUUID();
        when(orderRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.get(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Order");
    }

    @Test
    void updateReassignsProductAndFields() {
        UUID id = UUID.randomUUID();
        UUID newProductId = UUID.randomUUID();
        Product newProduct = new Product("Mechanical Keyboard", "SKU-0002", new BigDecimal("89.00"), 40, ProductStatus.ACTIVE);

        Order existing = new Order("Ada Lovelace", product, 2, new BigDecimal("29.99"), OrderStatus.PENDING);

        when(orderRepository.findById(id)).thenReturn(Optional.of(existing));
        when(productService.findOrThrow(newProductId)).thenReturn(newProduct);
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderResponse response = orderService.update(
                id,
                new OrderRequest(
                        "Grace Hopper",
                        newProductId,
                        5,
                        new BigDecimal("89.00"),
                        OrderStatus.COMPLETED));

        assertThat(response.customerName()).isEqualTo("Grace Hopper");
        assertThat(response.quantity()).isEqualTo(5);
        assertThat(response.unitPrice()).isEqualByComparingTo("89.00");
        assertThat(response.status()).isEqualTo(OrderStatus.COMPLETED);
        assertThat(existing.getProduct()).isSameAs(newProduct);
    }

    @Test
    void deleteRemovesOrder() {
        UUID id = UUID.randomUUID();
        Order existing = new Order("Ada Lovelace", product, 2, new BigDecimal("29.99"), OrderStatus.PENDING);
        when(orderRepository.findById(id)).thenReturn(Optional.of(existing));

        orderService.delete(id);

        verify(orderRepository).delete(existing);
    }
}
