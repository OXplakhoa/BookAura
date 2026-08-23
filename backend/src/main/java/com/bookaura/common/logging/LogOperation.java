package com.bookaura.common.logging;

import java.lang.annotation.*;

/** Marks meaningful service use-cases for AOP outcome/duration logging. */
@Documented
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface LogOperation {
}
