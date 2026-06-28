package com.stringpro.infrastructure.web.validation

import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import java.math.BigDecimal

class HalfStepValidator : ConstraintValidator<HalfStep, BigDecimal> {
    override fun isValid(
        value: BigDecimal?,
        context: ConstraintValidatorContext?,
    ): Boolean {
        if (value == null) return true // null handled by @NotNull
        // value is a multiple of 0.5 iff (value * 2) has no fractional part.
        return value.multiply(BigDecimal(2)).stripTrailingZeros().scale() <= 0
    }
}
