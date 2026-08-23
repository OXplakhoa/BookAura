package com.bookaura.loan.service;

/**
 * No production implementation. A test-context bean may throw after an internal mutation
 * to prove the enclosing service transaction rolls the complete use case back.
 */
@FunctionalInterface
public interface LoanFailureHook {
    void afterMutation(LoanMutation mutation);
}
