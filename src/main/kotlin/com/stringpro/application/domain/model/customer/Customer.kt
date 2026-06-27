package com.stringpro.application.domain.model.customer

import java.time.Instant

data class Customer(
    val id: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val phoneNumber: String,
    val notes: String?,
    val createdAt: Instant,
    val deletedAt: Instant? = null,
)
