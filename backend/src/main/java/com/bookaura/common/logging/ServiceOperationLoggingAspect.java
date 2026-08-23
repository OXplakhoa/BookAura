package com.bookaura.common.logging;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Cross-cutting timing/outcome concern only. Never logs arguments, return values, entities,
 * passwords, tokens or OTPs. Business logic remains in services.
 */
@Aspect
@Component
public class ServiceOperationLoggingAspect {

    private static final Logger log = LoggerFactory.getLogger(ServiceOperationLoggingAspect.class);

    @Around("@within(com.bookaura.common.logging.LogOperation) || " +
            "@annotation(com.bookaura.common.logging.LogOperation)")
    public Object logOperation(ProceedingJoinPoint joinPoint) throws Throwable {
        long started = System.nanoTime();
        String operation = operationName(joinPoint);
        try {
            Object result = joinPoint.proceed();
            log.info("service_operation operation={} outcome=SUCCESS durationMs={}",
                    operation, elapsedMillis(started));
            return result;
        } catch (Throwable error) {
            log.warn("service_operation operation={} outcome=FAILURE durationMs={} exception={}",
                    operation, elapsedMillis(started), error.getClass().getSimpleName());
            throw error;
        }
    }

    private String operationName(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        return signature.getDeclaringType().getSimpleName() + "." + signature.getMethod().getName();
    }

    private long elapsedMillis(long started) {
        return (System.nanoTime() - started) / 1_000_000;
    }
}
