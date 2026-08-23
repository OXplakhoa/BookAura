package com.bookaura.catalog.importcsv;

/**
 * No production bean implements this interface. Tests may register a test-context bean
 * that throws after saveAll+flush, proving @Transactional rolls DB mutations back.
 * This is not exposed by any endpoint and has zero production behavior.
 */
@FunctionalInterface
public interface CsvImportFailureHook {
    void afterPersist();
}
