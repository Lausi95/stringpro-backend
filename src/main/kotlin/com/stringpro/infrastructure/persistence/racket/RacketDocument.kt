package com.stringpro.infrastructure.persistence.racket

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document(collection = "rackets")
data class RacketDocument(
    @Id val id: String,
    val customerId: String,
    val brand: String,
    val model: String,
    val headSize: Int,
    val stringMains: Int,
    val stringCrosses: Int,
    val notes: String?,
    val createdAt: Instant,
    val deletedAt: Instant?,
)
