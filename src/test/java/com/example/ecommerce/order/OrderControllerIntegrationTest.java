package com.example.ecommerce.order;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.ecommerce.category.Category;
import com.example.ecommerce.category.CategoryRepository;
import com.example.ecommerce.product.Product;
import com.example.ecommerce.product.ProductRepository;
import com.example.ecommerce.product.ProductStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrderControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private UUID productId;

    @BeforeEach
    void seedProduct() {
        orderRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();

        Category category = categoryRepository.save(new Category("Peripherals", "Computer peripherals"));

        Product product = new Product("Wireless Mouse", "SKU-0001", new BigDecimal("29.99"), 100, ProductStatus.ACTIVE);
        product.setCategory(category);
        product = productRepository.save(product);
        productId = product.getId();
    }

    private static String orderJson(
            UUID productId, String customerName, String quantity, String unitPrice, String status) {
        return """
                {
                  "customerName": "%s",
                  "productId": "%s",
                  "quantity": %s,
                  "unitPrice": %s,
                  "status": "%s"
                }
                """.formatted(customerName, productId, quantity, unitPrice, status);
    }

    @Test
    void fullCrudLifecycleAcrossTheForeignKey() throws Exception {
        MvcResult created = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderJson(productId, "Ada Lovelace", "2", "29.99", "PENDING")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.customerName").value("Ada Lovelace"))
                .andExpect(jsonPath("$.productId").value(productId.toString()))
                .andExpect(jsonPath("$.quantity").value(2))
                .andExpect(jsonPath("$.unitPrice").value(29.99))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn();

        String id = objectMapper
                .readTree(created.getResponse().getContentAsString())
                .get("id")
                .asText();

        mockMvc.perform(get("/api/orders/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(productId.toString()))
                .andExpect(jsonPath("$.customerName").value("Ada Lovelace"));

        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(hasSize(1)));

        mockMvc.perform(put("/api/orders/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderJson(productId, "Grace Hopper", "5", "89.00", "COMPLETED")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerName").value("Grace Hopper"))
                .andExpect(jsonPath("$.quantity").value(5))
                .andExpect(jsonPath("$.unitPrice").value(89.00))
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        mockMvc.perform(delete("/api/orders/{id}", id)).andExpect(status().isNoContent());

        mockMvc.perform(get("/api/orders/{id}", id)).andExpect(status().isNotFound());
    }

    @Test
    void createWithUnknownProductReturns404() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderJson(
                                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                                "Ada Lovelace",
                                "1",
                                "0.00",
                                "PENDING")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Product not found")));
    }

    @Test
    void nonPositiveQuantityReturns400() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderJson(productId, "Ada Lovelace", "0", "10.00", "PENDING")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.quantity").exists());
    }

    @Test
    void negativeUnitPriceReturns400() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderJson(productId, "Ada Lovelace", "1", "-5.00", "PENDING")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.unitPrice").exists());
    }

    @Test
    void unknownEnumValueReturns400() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderJson(productId, "Ada Lovelace", "1", "10.00", "SHIPPED")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateWithUnknownProductReturns404() throws Exception {
        MvcResult created = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderJson(productId, "Ada Lovelace", "1", "29.99", "PENDING")))
                .andExpect(status().isCreated())
                .andReturn();

        String id = objectMapper
                .readTree(created.getResponse().getContentAsString())
                .get("id")
                .asText();

        mockMvc.perform(put("/api/orders/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderJson(
                                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                                "Ada Lovelace",
                                "1",
                                "29.99",
                                "PENDING")))
                .andExpect(status().isNotFound());
    }
}
