package com.stringpro.infrastructure.web.customer

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

data class CreateCustomerRequest(
    @field:NotBlank val firstName: String,
    @field:NotBlank val lastName: String,
    @field:NotBlank @field:Email val email: String,
    @field:NotBlank val phoneNumber: String,
    val notes: String?,
)

data class UpdateCustomerRequest(
    @field:NotBlank val firstName: String,
    @field:NotBlank val lastName: String,
    @field:NotBlank @field:Email val email: String,
    @field:NotBlank val phoneNumber: String,
    val notes: String?,
)
