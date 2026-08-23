package com.bookaura.catalog.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "authors")
@Getter
@Setter
public class Author {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 120)
    private String name;

    /** Lowercase + collapsed whitespace; unique lookup key, while name preserves display casing. */
    @Column(nullable = false, unique = true, length = 120)
    private String normalizedName;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Author other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
