package com.example.ecommerce;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.ecommerce.order.OrderController;
import com.example.ecommerce.product.ProductController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class EcommerceApplicationTests {

    @Autowired
    private ProductController productController;

    @Autowired
    private OrderController orderController;

    @Test
    void contextLoadsWithBothSlices() {
        assertThat(productController).isNotNull();
        assertThat(orderController).isNotNull();
    }
}
