package com.example.ecommerce.product;

import com.example.ecommerce.category.Category;
import com.example.ecommerce.category.CategoryRepository;
import com.example.ecommerce.common.DuplicateResourceException;
import com.example.ecommerce.common.ResourceInUseException;
import com.example.ecommerce.common.ResourceNotFoundException;
import com.example.ecommerce.order.OrderRepository;
import com.example.ecommerce.product.dto.ProductRequest;
import com.example.ecommerce.product.dto.ProductResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final CategoryRepository categoryRepository;

    public ProductService(
            ProductRepository productRepository,
            OrderRepository orderRepository,
            CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.categoryRepository = categoryRepository;
    }

    @Transactional
    public ProductResponse create(ProductRequest request) {
        if (productRepository.existsBySkuIgnoreCase(request.sku())) {
            throw new DuplicateResourceException("Product already exists with sku: " + request.sku());
        }

        Product product = new Product(
                request.name(),
                request.sku(),
                request.price(),
                request.stock(),
                request.status());
        product.setCategory(resolveCategory(request.categoryId()));

        return ProductResponse.from(productRepository.save(product));
    }

    public ProductResponse get(UUID id) {
        return ProductResponse.from(findOrThrow(id));
    }

    public List<ProductResponse> list() {
        return productRepository.findAll().stream()
                .map(ProductResponse::from)
                .toList();
    }

    @Transactional
    public ProductResponse update(UUID id, ProductRequest request) {
        Product product = findOrThrow(id);

        if (productRepository.existsBySkuIgnoreCaseAndIdNot(request.sku(), id)) {
            throw new DuplicateResourceException("Product already exists with sku: " + request.sku());
        }

        product.setName(request.name());
        product.setSku(request.sku());
        product.setPrice(request.price());
        product.setStock(request.stock());
        product.setStatus(request.status());
        product.setCategory(resolveCategory(request.categoryId()));

        return ProductResponse.from(productRepository.save(product));
    }

    @Transactional
    public void delete(UUID id) {
        Product product = findOrThrow(id);

        // Reject rather than let the orders.product_id constraint surface as a 500.
        if (orderRepository.existsByProductId(id)) {
            throw new ResourceInUseException("Product cannot be deleted while orders still reference it: " + id);
        }

        productRepository.delete(product);
    }

    /** Shared lookup so the Order slice can resolve the FK without duplicating the 404. */
    public Product findOrThrow(UUID id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
    }

    private Category resolveCategory(UUID categoryId) {
        if (categoryId == null) {
            return null;
        }
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", categoryId));
    }
}
