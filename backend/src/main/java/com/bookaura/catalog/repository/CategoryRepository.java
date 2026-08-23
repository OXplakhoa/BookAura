package com.bookaura.catalog.repository;

import com.bookaura.catalog.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {
    List<Category> findAllByNormalizedNameIn(Collection<String> normalizedNames);
}
