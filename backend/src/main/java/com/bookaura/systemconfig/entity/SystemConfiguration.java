package com.bookaura.systemconfig.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/** Singleton row (id=1) controlling operational application state. */
@Entity
@Table(name = "system_configuration")
@Getter
@Setter
public class SystemConfiguration {

    public static final long SINGLETON_ID = 1L;

    @Id
    private Long id;

    @Column(nullable = false)
    private boolean maintenanceMode;

    @Column(nullable = false)
    private Instant updatedAt;

    private UUID updatedBy;
}
