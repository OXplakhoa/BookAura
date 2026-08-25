package com.bookaura.catalog.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * A catalog title/type, not an individual physical copy. Quantities model its inventory.
 * Book owns the two many-to-many join tables; Author/Category deliberately have no reverse
 * collection to avoid unnecessary cycles. No cascade: relations are resolved/persisted explicitly.
 */
@Entity
@Table(name = "books")
@Getter
@Setter
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 255)
    private String title;

    /** Normalized ISBN (digits and optional ISBN-10 X only; no spaces/hyphens). */
    @Column(nullable = false, unique = true, length = 13)
    private String isbn;

    @Column(length = 4000)
    private String description;

    /** Nullable: unknown page count stays neutral in Shelf Aura scoring (D30). */
    private Integer pageCount;

    /** Free-form vibe labels (e.g. cozy, slow-burn) used by the rule-based recommender. */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "book_tags", joinColumns = @JoinColumn(name = "book_id"))
    @Column(name = "tag", length = 40, nullable = false)
    @BatchSize(size = 50)
    private Set<String> tags = new LinkedHashSet<>();

    @Column(nullable = false)
    private Integer publicationYear;

    @Column(nullable = false)
    private int totalQuantity;

    @Column(nullable = false)
    private int availableQuantity;

    @Column(nullable = false)
    private boolean active = true;

    private Instant deletedAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "book_authors",
            joinColumns = @JoinColumn(name = "book_id"),
            inverseJoinColumns = @JoinColumn(name = "author_id")
    )
    @BatchSize(size = 50) // bounded page -> one batched relation query, not N+1
    private Set<Author> authors = new LinkedHashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "book_categories",
            joinColumns = @JoinColumn(name = "book_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    @BatchSize(size = 50)
    private Set<Category> categories = new LinkedHashSet<>();

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Book other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
