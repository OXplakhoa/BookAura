package com.bookaura.catalog.repository;

import com.bookaura.catalog.entity.Author;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface AuthorRepository extends JpaRepository<Author, UUID> {
    List<Author> findAllByNormalizedNameIn(Collection<String> normalizedNames);
}
