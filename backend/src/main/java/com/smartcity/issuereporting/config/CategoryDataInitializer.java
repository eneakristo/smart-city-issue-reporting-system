package com.smartcity.issuereporting.config;

import com.smartcity.issuereporting.model.entity.Category;
import com.smartcity.issuereporting.repository.CategoryRepository;
import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class CategoryDataInitializer implements ApplicationRunner {

    private static final List<CategorySeed> DEFAULT_CATEGORIES = List.of(
            new CategorySeed("Roads", "Road damage, potholes, sidewalks, and traffic surface problems."),
            new CategorySeed("Street Lighting", "Broken or unsafe streetlights and public lighting issues."),
            new CategorySeed("Waste Management", "Overflowing bins, illegal dumping, and waste collection issues."),
            new CategorySeed("Water Supply", "Water leaks, supply interruptions, and drainage concerns."),
            new CategorySeed("Public Safety", "Hazards or concerns that may affect public safety."),
            new CategorySeed("Parks and Green Areas", "Problems in parks, public gardens, and green spaces."),
            new CategorySeed("Other", "City issues that do not match the standard categories.")
    );

    private final CategoryRepository categoryRepository;

    public CategoryDataInitializer(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        DEFAULT_CATEGORIES.forEach(this::createIfMissing);
    }

    private void createIfMissing(CategorySeed categorySeed) {
        if (categoryRepository.existsByNameIgnoreCase(categorySeed.name())) {
            return;
        }

        Category category = new Category();
        category.setName(categorySeed.name());
        category.setDescription(categorySeed.description());
        categoryRepository.save(category);
    }

    private record CategorySeed(String name, String description) {
    }
}
