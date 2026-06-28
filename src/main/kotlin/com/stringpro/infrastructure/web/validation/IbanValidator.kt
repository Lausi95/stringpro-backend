package com.stringpro.infrastructure.web.validation

import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import java.math.BigInteger

/**
 * Validates an IBAN by structure and the ISO 13616 mod-97 checksum.
 * A blank value is treated as valid (the field is optional); format is only
 * enforced when a value is present. Whitespace is stripped and letters
 * upper-cased before checking.
 */
class IbanValidator : ConstraintValidator<Iban, String> {
    override fun isValid(
        value: String?,
        context: ConstraintValidatorContext?,
    ): Boolean {
        if (value.isNullOrBlank()) return true
        val normalized = value.replace(" ", "").uppercase()
        if (!STRUCTURE.matches(normalized)) return false

        // Move the four leading characters (country code + check digits) to the end.
        val rearranged = normalized.substring(4) + normalized.substring(0, 4)
        val numeric =
            buildString {
                for (ch in rearranged) {
                    when (ch) {
                        in '0'..'9' -> append(ch)
                        in 'A'..'Z' -> append(ch - 'A' + 10)
                        else -> return false
                    }
                }
            }
        return BigInteger(numeric).mod(NINETY_SEVEN) == BigInteger.ONE
    }

    companion object {
        private val STRUCTURE = Regex("^[A-Z]{2}\\d{2}[A-Z0-9]{1,30}$")
        private val NINETY_SEVEN = BigInteger.valueOf(97)
    }
}
