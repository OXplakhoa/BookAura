package com.bookaura.catalog.repository;

import com.bookaura.catalog.entity.Book;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
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

    /** Atomic final-copy guard: affected rows = 1 means inventory was obtained, 0 means unavailable. */
    @Modifying
    @Query("UPDATE Book b SET b.availableQuantity = b.availableQuantity - 1 " +
            "WHERE b.id = :id AND b.active = true AND b.availableQuantity > 0")
    int decrementAvailableIfPossible(@Param("id") UUID id);

    /** Return-side invariant guard: availability can never exceed total inventory. */
    @Modifying
    @Query("UPDATE Book b SET b.availableQuantity = b.availableQuantity + 1 " +
            "WHERE b.id = :id AND b.availableQuantity < b.totalQuantity")
    int incrementAvailableIfBelowTotal(@Param("id") UUID id);

    @EntityGraph(attributePaths = {"authors", "categories"})
    @Query("SELECT b FROM Book b WHERE b.id = :id")
    Optional<Book> findDetailedById(@Param("id") UUID id);

    @EntityGraph(attributePaths = {"authors", "categories"})
    @Query("SELECT b FROM Book b WHERE b.id = :id AND b.active = true")
    Optional<Book> findActiveDetailedById(@Param("id") UUID id);

    /** Shelf Aura: all live titles in one bounded query; tags load via @BatchSize. */
    @EntityGraph(attributePaths = {"authors", "categories"})
    List<Book> findByActiveTrue();
}
