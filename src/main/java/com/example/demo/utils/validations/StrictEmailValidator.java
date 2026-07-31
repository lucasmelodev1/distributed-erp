package com.example.demo.utils.validations;

import java.util.regex.Pattern;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class StrictEmailValidator implements ConstraintValidator<StrictEmail, String> {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            // let @NotBlank handle null/blank; don't double-report here
            return true;
        }
        return EMAIL_PATTERN.matcher(value).matches();
    }
}