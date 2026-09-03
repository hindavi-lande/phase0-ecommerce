package com.example.ecommerce.product;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.ecommerce.order.OrderRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
class ProductControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @BeforeEach
    void clean() {
        orderRepository.deleteAll();
        productRepository.deleteAll();
    }

    private static String productJson(String sku) {
        return """
                {
                  "name": "Wireless Mouse",
                  "sku": "%s",
                  "price": 29.99,
                  "stock": 100,
                  "status": "ACTIVE"
                }
                """.formatted(sku);
    }

    @Test
    void fullCrudLifecycle() throws Exception {
        MvcResult created = mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productJson("SKU-0001")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Wireless Mouse"))
                .andExpect(jsonPath("$.sku").value("SKU-0001"))
                .andExpect(jsonPath("$.price").value(29.99))
                .andExpect(jsonPath("$.stock").value(100))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andReturn();

        JsonNode body = objectMapper.readTree(created.getResponse().getContentAsString());
        String id = body.get("id").asText();

        mockMvc.perform(get("/api/products/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.sku").value("SKU-0001"));

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(hasSize(1)));

        mockMvc.perform(put("/api/products/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Ergonomic Mouse",
                                  "sku": "SKU-0002",
                                  "price": 39.99,
                                  "stock": 50,
                                  "status": "INACTIVE"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Ergonomic Mouse"))
                .andExpect(jsonPath("$.sku").value("SKU-0002"))
                .andExpect(jsonPath("$.status").value("INACTIVE"));

        mockMvc.perform(delete("/api/products/{id}", id)).andExpect(status().isNoContent());

        mockMvc.perform(get("/api/products/{id}", id)).andExpect(status().isNotFound());
    }

    @Test
    void getUnknownIdReturns404() throws Exception {
        mockMvc.perform(get("/api/products/{id}", "11111111-1111-1111-1111-111111111111"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Product not found")));
    }

    @Test
    void invalidPayloadReturns400WithFieldErrors() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "",
                                  "sku": "SKU-0001",
                                  "price": -1.00,
                                  "stock": -5,
                                  "status": "ACTIVE"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.name").exists())
                .andExpect(jsonPath("$.fieldErrors.price").exists())
                .andExpect(jsonPath("$.fieldErrors.stock").exists());
    }

    @Test
    void unknownEnumValueReturns400() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Wireless Mouse",
                                  "sku": "SKU-0003",
                                  "price": 10.00,
                                  "stock": 5,
                                  "status": "DISCONTINUED"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void duplicateSkuReturns409() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productJson("SKU-DUP")))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productJson("SKU-DUP")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void deletingProductWithOrdersReturns409() throws Exception {
        MvcResult created = mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productJson("SKU-LINKED")))
                .andExpect(status().isCreated())
                .andReturn();

        String productId = objectMapper
                .readTree(created.getResponse().getContentAsString())
                .get("id")
                .asText();

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerName": "Ada Lovelace",
                                  "productId": "%s",
                                  "quantity": 2,
                                  "unitPrice": 29.99,
                                  "status": "PENDING"
                                }
                                """.formatted(productId)))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/products/{id}", productId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }
}
