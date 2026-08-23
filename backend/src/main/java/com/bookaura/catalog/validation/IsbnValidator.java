package com.bookaura.catalog.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class IsbnValidator implements ConstraintValidator<ValidIsbn, String> {
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value == null || IsbnUtils.isValid(value); // @NotBlank owns null/blank validation
    }
}
