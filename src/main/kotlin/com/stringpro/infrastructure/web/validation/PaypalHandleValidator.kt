package com.stringpro.infrastructure.web.validation

import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext

/**
 * Validates the Stringer's PayPal Handle — the `<username>` in a `paypal.me/<username>` link.
 * A blank value is treated as valid (the field is optional); format is only enforced when a
 * value is present. Surrounding whitespace is trimmed first (the controller trims before
 * storing), then the handle must be 1-20 characters, letters or digits only (no inner spaces,
 * '@', or slashes), so the `paypal.me/…` link built from it is always well-formed.
 */
class PaypalHandleValidator : ConstraintValidator<PaypalHandle, String> {
    override fun isValid(
        value: String?,
        context: ConstraintValidatorContext?,
    ): Boolean {
        if (value.isNullOrBlank()) return true
        return HANDLE.matches(value.trim())
    }

    companion object {
        private val HANDLE = Regex("^[A-Za-z0-9]{1,20}$")
    }
}
