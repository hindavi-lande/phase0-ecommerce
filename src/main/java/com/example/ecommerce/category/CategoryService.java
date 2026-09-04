package com.example.ecommerce.category;

import com.example.ecommerce.category.dto.CategoryCreateRequest;
import com.example.ecommerce.category.dto.CategoryResponse;
import com.example.ecommerce.category.dto.CategoryUpdateRequest;
import com.example.ecommerce.common.DuplicateResourceException;
import com.example.ecommerce.common.ResourceInUseException;
import com.example.ecommerce.common.ResourceNotFoundException;
import com.example.ecommerce.product.ProductRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    public CategoryService(CategoryRepository categoryRepository, ProductRepository productRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public CategoryResponse create(CategoryCreateRequest request) {
        if (categoryRepository.existsByNameIgnoreCase(request.name())) {
            throw new DuplicateResourceException("Category already exists with name: " + request.name());
        }

        Category category = new Category(request.name(), request.description());

        return CategoryResponse.from(categoryRepository.save(category));
    }

    public CategoryResponse get(UUID id) {
        return CategoryResponse.from(findOrThrow(id));
    }

    public List<CategoryResponse> list() {
        return categoryRepository.findAll().stream()
                .map(CategoryResponse::from)
                .toList();
    }

    @Transactional
    public CategoryResponse update(UUID id, CategoryUpdateRequest request) {
        Category category = findOrThrow(id);

        if (request.name() != null) {
            if (categoryRepository.existsByNameIgnoreCaseAndIdNot(request.name(), id)) {
                throw new DuplicateResourceException("Category already exists with name: " + request.name());
            }
            category.setName(request.name());
        }

        if (request.description() != null) {
            category.setDescription(request.description());
        }

        return CategoryResponse.from(categoryRepository.save(category));
    }

    @Transactional
    public void delete(UUID id) {
        Category category = findOrThrow(id);

        // Reject rather than let the products.category_id constraint surface as a 500.
        if (productRepository.existsByCategoryId(id)) {
            throw new ResourceInUseException("Category cannot be deleted while products still reference it: " + id);
        }

        categoryRepository.delete(category);
    }

    private Category findOrThrow(UUID id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));
    }
}
