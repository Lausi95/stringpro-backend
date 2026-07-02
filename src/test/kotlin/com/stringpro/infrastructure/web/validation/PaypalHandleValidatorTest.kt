package com.stringpro.infrastructure.web.validation

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PaypalHandleValidatorTest {
    private val validator = PaypalHandleValidator()

    @Test
    fun `should treat null as valid because the field is optional`() {
        assertTrue(validator.isValid(null, null))
    }

    @Test
    fun `should treat blank as valid because the field is optional`() {
        assertTrue(validator.isValid("", null))
        assertTrue(validator.isValid("   ", null))
    }

    @Test
    fun `should accept an alphanumeric handle`() {
        assertTrue(validator.isValid("JaneStringer", null))
        assertTrue(validator.isValid("jane123", null))
        assertTrue(validator.isValid("A", null))
    }

    @Test
    fun `should accept a handle with surrounding whitespace since it is trimmed before storing`() {
        assertTrue(validator.isValid("  JaneStringer  ", null))
    }

    @Test
    fun `should accept a handle of the maximum length`() {
        assertTrue(validator.isValid("a".repeat(20), null))
    }

    @Test
    fun `should reject a handle longer than 20 characters`() {
        assertFalse(validator.isValid("a".repeat(21), null))
    }

    @Test
    fun `should reject a handle with spaces`() {
        assertFalse(validator.isValid("jane doe", null))
    }

    @Test
    fun `should reject a handle with an at sign`() {
        assertFalse(validator.isValid("@jane", null))
    }

    @Test
    fun `should reject a handle with a slash`() {
        assertFalse(validator.isValid("paypal.me/jane", null))
    }

    @Test
    fun `should reject a handle with punctuation`() {
        assertFalse(validator.isValid("jane_doe", null))
        assertFalse(validator.isValid("jane-doe", null))
        assertFalse(validator.isValid("jane.doe", null))
    }
}
