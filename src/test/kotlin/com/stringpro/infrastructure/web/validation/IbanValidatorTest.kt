package com.stringpro.infrastructure.web.validation

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class IbanValidatorTest {
    private val validator = IbanValidator()

    @Test
    fun `should accept a valid iban`() {
        assertTrue(validator.isValid("DE89370400440532013000", null))
    }

    @Test
    fun `should treat blank and null as valid (optional field)`() {
        assertTrue(validator.isValid(null, null))
        assertTrue(validator.isValid("", null))
        assertTrue(validator.isValid("   ", null))
    }

    @Test
    fun `should normalise spaces and lower case before checking`() {
        assertTrue(validator.isValid("de89 3704 0044 0532 0130 00", null))
    }

    @Test
    fun `should reject an iban with a wrong check digit`() {
        assertFalse(validator.isValid("DE89370400440532013001", null))
    }

    @Test
    fun `should reject a structurally malformed iban`() {
        assertFalse(validator.isValid("GARBAGE", null))
        assertFalse(validator.isValid("D8937040044", null))
    }
}
