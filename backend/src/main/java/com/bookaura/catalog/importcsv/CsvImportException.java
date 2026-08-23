package com.bookaura.catalog.importcsv;

import com.bookaura.common.error.BusinessException;
import com.bookaura.common.error.ErrorCode;

import java.util.LinkedHashMap;
import java.util.Map;

/** Row-level CSV errors keyed like row[3].isbn. */
public class CsvImportException extends BusinessException {

    private final Map<String, String> rowErrors;

    public CsvImportException(String message, Map<String, String> rowErrors) {
        super(ErrorCode.CSV_VALIDATION_ERROR, message);
        this.rowErrors = Map.copyOf(new LinkedHashMap<>(rowErrors));
    }

    public Map<String, String> rowErrors() {
        return rowErrors;
    }
}
