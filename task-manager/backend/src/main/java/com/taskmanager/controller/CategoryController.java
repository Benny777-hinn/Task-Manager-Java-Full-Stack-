package com.taskmanager.controller;

import com.taskmanager.dto.CategoryRequest;
import com.taskmanager.dto.CategoryResponse;
import com.taskmanager.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoints for the current user's categories.
 */
@RestController
@RequestMapping("/api/categories")
@Tag(name = "Categories", description = "Create and manage your own categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    /** GET /api/categories -> all your categories. */
    @GetMapping
    @Operation(summary = "List your categories")
    public ResponseEntity<List<CategoryResponse>> getCategories(Authentication authentication) {
        return ResponseEntity.ok(categoryService.getCategories(authentication.getName()));
    }

    /** POST /api/categories -> create a category. */
    @PostMapping
    @Operation(summary = "Create a new category")
    public ResponseEntity<CategoryResponse> createCategory(@Valid @RequestBody CategoryRequest request,
                                                           Authentication authentication) {
        CategoryResponse created = categoryService.createCategory(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /** DELETE /api/categories/{id} -> delete a category you own. */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete one of your categories")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id, Authentication authentication) {
        categoryService.deleteCategory(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
