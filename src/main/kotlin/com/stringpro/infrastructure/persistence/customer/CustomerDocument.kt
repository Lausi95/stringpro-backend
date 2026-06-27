package com.stringpro.infrastructure.persistence.customer

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document(collection = "customers")
data class CustomerDocument(
    @Id val id: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val phoneNumber: String,
    val notes: String?,
    val createdAt: Instant,
    val deletedAt: Instant?,
)
