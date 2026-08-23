package com.bookaura.catalog.service;

import com.bookaura.catalog.entity.Author;
import com.bookaura.catalog.entity.Category;
import com.bookaura.catalog.repository.AuthorRepository;
import com.bookaura.catalog.repository.CategoryRepository;
import com.bookaura.common.error.BusinessException;
import com.bookaura.common.error.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Resolves author/category names in bulk.
 * Complexity: O(n + m), using LinkedHashMap lookup; no nested row x relation scans and
 * no database call inside a loop. normalizedName prevents case-only duplicates.
 */
@Component
public class CatalogRelationResolver {

    private final AuthorRepository authorRepository;
    private final CategoryRepository categoryRepository;

    public CatalogRelationResolver(AuthorRepository authorRepository, CategoryRepository categoryRepository) {
        this.authorRepository = authorRepository;
        this.categoryRepository = categoryRepository;
    }

    public Set<Author> resolveAuthors(Collection<String> rawNames) {
        LinkedHashMap<String, String> requested = normalizedDisplayNames(rawNames);
        Map<String, Author> byName = authorRepository.findAllByNormalizedNameIn(requested.keySet()).stream()
                .collect(Collectors.toMap(Author::getNormalizedName, Function.identity()));

        List<Author> missing = new ArrayList<>();
        requested.forEach((normalized, display) -> {
            if (!byName.containsKey(normalized)) {
                Author author = new Author();
                author.setName(display);
                author.setNormalizedName(normalized);
                missing.add(author);
            }
        });
        authorRepository.saveAll(missing).forEach(author -> byName.put(author.getNormalizedName(), author));

        LinkedHashSet<Author> result = new LinkedHashSet<>();
        requested.keySet().forEach(name -> result.add(byName.get(name)));
        return result;
    }

    public Set<Category> resolveCategories(Collection<String> rawNames) {
        LinkedHashMap<String, String> requested = normalizedDisplayNames(rawNames);
        Map<String, Category> byName = categoryRepository.findAllByNormalizedNameIn(requested.keySet()).stream()
                .collect(Collectors.toMap(Category::getNormalizedName, Function.identity()));

        List<Category> missing = new ArrayList<>();
        requested.forEach((normalized, display) -> {
            if (!byName.containsKey(normalized)) {
                Category category = new Category();
                category.setName(display);
                category.setNormalizedName(normalized);
                missing.add(category);
            }
        });
        categoryRepository.saveAll(missing).forEach(category -> byName.put(category.getNormalizedName(), category));

        LinkedHashSet<Category> result = new LinkedHashSet<>();
        requested.keySet().forEach(name -> result.add(byName.get(name)));
        return result;
    }

    public static String normalizeName(String raw) {
        return displayName(raw).toLowerCase(Locale.ROOT);
    }

    private LinkedHashMap<String, String> normalizedDisplayNames(Collection<String> rawNames) {
        LinkedHashMap<String, String> result = new LinkedHashMap<>();
        for (String raw : rawNames) {
            String display = displayName(raw);
            if (display.isBlank()) {
                throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Author/category name cannot be blank");
            }
            result.putIfAbsent(display.toLowerCase(Locale.ROOT), display);
        }
        return result;
    }

    private static String displayName(String raw) {
        if (raw == null) return "";
        return raw.trim().replaceAll("\\s+", " ");
    }
}
