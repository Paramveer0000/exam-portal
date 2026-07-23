package com.project.examportalbackend.services.implementation;

import com.project.examportalbackend.models.Category;
import com.project.examportalbackend.repository.CategoryRepository;
import com.project.examportalbackend.security.AuthFacade;
import com.project.examportalbackend.services.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;

@Service
public class CategoryServiceImpl implements CategoryService {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private AuthFacade authFacade;

    @Override
    public Category addCategory(Category category) {
        if (!StringUtils.hasText(category.getTitle())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Class title is required");
        }
        Long me = authFacade.getCurrentUserId();
        if (categoryRepository.existsByTitleIgnoreCaseAndCreatedBy(category.getTitle(), me)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A class named '" + category.getTitle() + "' already exists");
        }
        // Stamp ownership from the authenticated admin, never from the request body.
        category.setCreatedBy(me);
        return categoryRepository.save(category);
    }

    @Override
    public List<Category> getCategories() {
        // Content is platform-wide (super-admin owned). A student sees only their
        // own class; admins and super admins see all classes.
        if (authFacade.isStudent()) {
            Long classId = authFacade.getCurrentUser().getClassId();
            if (classId == null) {
                return Collections.emptyList();
            }
            return categoryRepository.findById(classId)
                    .map(Collections::singletonList)
                    .orElse(Collections.emptyList());
        }
        return categoryRepository.findAll();
    }

    @Override
    public Category getCategory(Long catId) {
        // Read building-block used by other services; no scoping here.
        return categoryRepository.findById(catId).orElse(null);
    }

    @Override
    public Category updateCategory(Category category) {
        if (!StringUtils.hasText(category.getTitle())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Class title is required");
        }
        Category existing = categoryRepository.findById(category.getCatId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));
        authFacade.assertCanManage(existing.getCreatedBy());
        if (categoryRepository.existsByTitleIgnoreCaseAndCreatedByAndCatIdNot(
                category.getTitle(), existing.getCreatedBy(), existing.getCatId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A class named '" + category.getTitle() + "' already exists");
        }
        // Preserve ownership; the request body must not be able to reassign it.
        category.setCreatedBy(existing.getCreatedBy());
        return categoryRepository.save(category);
    }

    @Override
    public void deleteCategory(Long categoryId) {
        Category existing = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));
        authFacade.assertCanManage(existing.getCreatedBy());
        categoryRepository.delete(existing);
    }
}
