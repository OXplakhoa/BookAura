package com.bookaura.loan.entity;

import com.bookaura.account.entity.MemberProfile;
import com.bookaura.catalog.entity.Book;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "loans")
@Getter
@Setter
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_profile_id", nullable = false)
    private MemberProfile memberProfile;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @Column(nullable = false, updatable = false)
    private Instant borrowedAt;

    @Column(nullable = false)
    private Instant dueAt;

    /** null = active loan; non-null = returned and retained forever for history. */
    private Instant returnedAt;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Loan other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
