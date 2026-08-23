package com.bookaura.systemconfig.service;

import com.bookaura.systemconfig.dto.SystemConfigurationResponse;
import com.bookaura.systemconfig.entity.SystemConfiguration;
import com.bookaura.systemconfig.repository.SystemConfigurationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * DB is source of truth; AtomicBoolean is the request-path cache.
 * Only startup/toggle/read-admin endpoints query DB. Normal requests do zero config queries.
 */
@Service
public class SystemConfigurationService {

    private static final Logger AUDIT = LoggerFactory.getLogger("com.bookaura.audit");

    private final SystemConfigurationRepository repository;
    private final AtomicBoolean maintenanceMode = new AtomicBoolean(false);

    public SystemConfigurationService(SystemConfigurationRepository repository) {
        this.repository = repository;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional(readOnly = true)
    public void initializeCache() {
        SystemConfiguration config = findSingleton();
        maintenanceMode.set(config.isMaintenanceMode());
        AUDIT.info("event=MAINTENANCE_CACHE_INITIALIZED enabled={}", config.isMaintenanceMode());
    }

    public boolean isMaintenanceMode() {
        return maintenanceMode.get();
    }

    @Transactional(readOnly = true)
    public SystemConfigurationResponse get() {
        return toResponse(findSingleton());
    }

    @Transactional
    public SystemConfigurationResponse setMaintenanceMode(boolean enabled, UUID adminUserId) {
        SystemConfiguration config = findSingleton();
        config.setMaintenanceMode(enabled);
        config.setUpdatedAt(Instant.now());
        config.setUpdatedBy(adminUserId);

        // Cache only after DB commit. A failed transaction must never expose a state not in DB.
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                maintenanceMode.set(enabled);
                AUDIT.warn("event=MAINTENANCE_MODE_CHANGED enabled={} adminUserId={}", enabled, adminUserId);
            }
        });
        return toResponse(config);
    }

    private SystemConfiguration findSingleton() {
        return repository.findById(SystemConfiguration.SINGLETON_ID)
                .orElseThrow(() -> new IllegalStateException("System configuration singleton row is missing"));
    }

    private SystemConfigurationResponse toResponse(SystemConfiguration config) {
        return new SystemConfigurationResponse(
                config.isMaintenanceMode(), config.getUpdatedAt(), config.getUpdatedBy());
    }
}
