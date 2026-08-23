package com.bookaura.catalog.repository;

import com.bookaura.catalog.entity.Book;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BookRepository extends JpaRepository<Book, UUID>, JpaSpecificationExecutor<Book> {

    boolean existsByIsbn(String isbn);

    boolean existsByIsbnAndIdNot(String isbn, UUID id);

    List<Book> findAllByIsbnIn(Collection<String> isbns);

    @EntityGraph(attributePaths = {"authors", "categories"})
    @Query("SELECT b FROM Book b WHERE b.id = :id")
    Optional<Book> findDetailedById(@Param("id") UUID id);

    @EntityGraph(attributePaths = {"authors", "categories"})
    @Query("SELECT b FROM Book b WHERE b.id = :id AND b.active = true")
    Optional<Book> findActiveDetailedById(@Param("id") UUID id);
}
