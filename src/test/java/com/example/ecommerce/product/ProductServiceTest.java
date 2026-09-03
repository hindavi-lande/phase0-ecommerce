package com.example.ecommerce.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.ecommerce.common.DuplicateResourceException;
import com.example.ecommerce.common.ResourceInUseException;
import com.example.ecommerce.common.ResourceNotFoundException;
import com.example.ecommerce.order.OrderRepository;
import com.example.ecommerce.product.dto.ProductRequest;
import com.example.ecommerce.product.dto.ProductResponse;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private ProductService productService;

    private ProductRequest request;

    @BeforeEach
    void setUp() {
        request = new ProductRequest("Wireless Mouse", "SKU-0001", new BigDecimal("29.99"), 100, ProductStatus.ACTIVE);
    }

    @Test
    void createPersistsProduct() {
        when(productRepository.existsBySkuIgnoreCase("SKU-0001")).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductResponse response = productService.create(request);

        assertThat(response.name()).isEqualTo("Wireless Mouse");
        assertThat(response.sku()).isEqualTo("SKU-0001");
        assertThat(response.price()).isEqualByComparingTo("29.99");
        assertThat(response.stock()).isEqualTo(100);
        assertThat(response.status()).isEqualTo(ProductStatus.ACTIVE);
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void createRejectsDuplicateSku() {
        when(productRepository.existsBySkuIgnoreCase("SKU-0001")).thenReturn(true);

        assertThatThrownBy(() -> productService.create(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("SKU-0001");

        verify(productRepository, never()).save(any());
    }

    @Test
    void getThrowsWhenMissing() {
        UUID id = UUID.randomUUID();
        when(productRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.get(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(id.toString());
    }

    @Test
    void listMapsEveryProduct() {
        when(productRepository.findAll()).thenReturn(List.of(
                new Product("Wireless Mouse", "SKU-0001", new BigDecimal("29.99"), 100, ProductStatus.ACTIVE),
                new Product("Mechanical Keyboard", "SKU-0002", new BigDecimal("89.00"), 40, ProductStatus.INACTIVE)));

        List<ProductResponse> products = productService.list();

        assertThat(products).hasSize(2);
        assertThat(products).extracting(ProductResponse::sku).containsExactly("SKU-0001", "SKU-0002");
    }

    @Test
    void updateAppliesEveryField() {
        UUID id = UUID.randomUUID();
        Product existing = new Product("Wireless Mouse", "SKU-0001", new BigDecimal("29.99"), 100, ProductStatus.ACTIVE);
        when(productRepository.findById(id)).thenReturn(Optional.of(existing));
        when(productRepository.existsBySkuIgnoreCaseAndIdNot("SKU-0099", id)).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductResponse response = productService.update(
                id,
                new ProductRequest("Ergonomic Mouse", "SKU-0099", new BigDecimal("39.99"), 50, ProductStatus.INACTIVE));

        assertThat(response.name()).isEqualTo("Ergonomic Mouse");
        assertThat(response.sku()).isEqualTo("SKU-0099");
        assertThat(response.price()).isEqualByComparingTo("39.99");
        assertThat(response.stock()).isEqualTo(50);
        assertThat(response.status()).isEqualTo(ProductStatus.INACTIVE);
    }

    @Test
    void deleteRemovesProductWithoutOrders() {
        UUID id = UUID.randomUUID();
        Product existing = new Product("Wireless Mouse", "SKU-0001", new BigDecimal("29.99"), 100, ProductStatus.ACTIVE);
        when(productRepository.findById(id)).thenReturn(Optional.of(existing));
        when(orderRepository.existsByProductId(id)).thenReturn(false);

        productService.delete(id);

        verify(productRepository).delete(existing);
    }

    @Test
    void deleteRejectsProductWithOrders() {
        UUID id = UUID.randomUUID();
        when(productRepository.findById(id))
                .thenReturn(Optional.of(
                        new Product("Wireless Mouse", "SKU-0001", new BigDecimal("29.99"), 100, ProductStatus.ACTIVE)));
        when(orderRepository.existsByProductId(id)).thenReturn(true);

        assertThatThrownBy(() -> productService.delete(id))
                .isInstanceOf(ResourceInUseException.class);

        verify(productRepository, never()).delete(any());
    }
}
