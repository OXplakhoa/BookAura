package com.bookaura.member.service;

import com.bookaura.common.error.BusinessException;
import com.bookaura.common.error.ErrorCode;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class MemberPageRequestFactory {

    private static final Map<String, String> ALLOWED = Map.of(
            "fullName", "fullName",
            "email", "userAccount.email",
            "dateOfBirth", "dateOfBirth",
            "status", "userAccount.status",
            "createdAt", "createdAt"
    );

    public Pageable create(int page, int size, String rawSort) {
        if (page < 0) throw new BusinessException(ErrorCode.INVALID_PAGE, "Page index must be >= 0");
        if (size < 1 || size > 10) {
            throw new BusinessException(ErrorCode.PAGE_SIZE_EXCEEDED, "Page size must be between 1 and 10");
        }
        if (rawSort == null || rawSort.isBlank()) {
            return PageRequest.of(page, size, Sort.by(Sort.Order.asc("fullName")));
        }
        String[] entries = rawSort.split(",");
        if (entries.length > 5) throw invalid("At most 5 sort fields are allowed");
        List<Sort.Order> orders = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (String entry : entries) {
            String[] pair = entry.trim().split(":", -1);
            if (pair.length != 2 || !ALLOWED.containsKey(pair[0]) || !seen.add(pair[0])) {
                throw invalid("Sort fields must be unique and allowlisted: " + ALLOWED.keySet());
            }
            try {
                orders.add(new Sort.Order(Sort.Direction.fromString(pair[1]), ALLOWED.get(pair[0])));
            } catch (IllegalArgumentException ex) {
                throw invalid("Sort direction must be asc or desc");
            }
        }
        return PageRequest.of(page, size, Sort.by(orders));
    }

    private BusinessException invalid(String message) {
        return new BusinessException(ErrorCode.INVALID_SORT, message);
    }
}
