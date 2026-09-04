package com.example.ecommerce.category;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.ecommerce.category.dto.CategoryCreateRequest;
import com.example.ecommerce.category.dto.CategoryResponse;
import com.example.ecommerce.category.dto.CategoryUpdateRequest;
import com.example.ecommerce.common.DuplicateResourceException;
import com.example.ecommerce.common.ResourceInUseException;
import com.example.ecommerce.common.ResourceNotFoundException;
import com.example.ecommerce.product.ProductRepository;
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
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private CategoryService categoryService;

    private CategoryCreateRequest createRequest;

    @BeforeEach
    void setUp() {
        createRequest = new CategoryCreateRequest("Electronics", "Gadgets and gizmos");
    }

    @Test
    void createPersistsCategory() {
        when(categoryRepository.existsByNameIgnoreCase("Electronics")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));

        CategoryResponse response = categoryService.create(createRequest);

        assertThat(response.name()).isEqualTo("Electronics");
        assertThat(response.description()).isEqualTo("Gadgets and gizmos");
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    void createRejectsDuplicateName() {
        when(categoryRepository.existsByNameIgnoreCase("Electronics")).thenReturn(true);

        assertThatThrownBy(() -> categoryService.create(createRequest))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Electronics");

        verify(categoryRepository, never()).save(any());
    }

    @Test
    void getThrowsWhenMissing() {
        UUID id = UUID.randomUUID();
        when(categoryRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.get(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(id.toString());
    }

    @Test
    void listMapsEveryCategory() {
        when(categoryRepository.findAll()).thenReturn(List.of(
                new Category("Electronics", "Gadgets and gizmos"),
                new Category("Books", "Printed and digital books")));

        List<CategoryResponse> categories = categoryService.list();

        assertThat(categories).hasSize(2);
        assertThat(categories).extracting(CategoryResponse::name).containsExactly("Electronics", "Books");
    }

    @Test
    void updateAppliesOnlyProvidedFields() {
        UUID id = UUID.randomUUID();
        Category existing = new Category("Electronics", "Gadgets and gizmos");
        when(categoryRepository.findById(id)).thenReturn(Optional.of(existing));
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));

        CategoryResponse response = categoryService.update(id, new CategoryUpdateRequest(null, "Updated description"));

        assertThat(response.name()).isEqualTo("Electronics");
        assertThat(response.description()).isEqualTo("Updated description");
        verify(categoryRepository, never()).existsByNameIgnoreCaseAndIdNot(any(), any());
    }

    @Test
    void updateRejectsDuplicateNameWhenNameChanges() {
        UUID id = UUID.randomUUID();
        Category existing = new Category("Electronics", "Gadgets and gizmos");
        when(categoryRepository.findById(id)).thenReturn(Optional.of(existing));
        when(categoryRepository.existsByNameIgnoreCaseAndIdNot("Books", id)).thenReturn(true);

        assertThatThrownBy(() -> categoryService.update(id, new CategoryUpdateRequest("Books", null)))
                .isInstanceOf(DuplicateResourceException.class);

        verify(categoryRepository, never()).save(any());
    }

    @Test
    void deleteRemovesCategoryWithoutProducts() {
        UUID id = UUID.randomUUID();
        Category existing = new Category("Electronics", "Gadgets and gizmos");
        when(categoryRepository.findById(id)).thenReturn(Optional.of(existing));
        when(productRepository.existsByCategoryId(id)).thenReturn(false);

        categoryService.delete(id);

        verify(categoryRepository).delete(existing);
    }

    @Test
    void deleteRejectsCategoryWithProducts() {
        UUID id = UUID.randomUUID();
        when(categoryRepository.findById(id))
                .thenReturn(Optional.of(new Category("Electronics", "Gadgets and gizmos")));
        when(productRepository.existsByCategoryId(id)).thenReturn(true);

        assertThatThrownBy(() -> categoryService.delete(id))
                .isInstanceOf(ResourceInUseException.class);

        verify(categoryRepository, never()).delete(any());
    }
}
