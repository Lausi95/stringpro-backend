package com.stringpro.infrastructure.web.customer

import com.stringpro.application.domain.model.PageResult
import com.stringpro.application.domain.model.customer.Customer
import java.time.Instant

data class CustomerResponse(
    val id: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val phoneNumber: String,
    val notes: String?,
    val createdAt: Instant,
)

data class PagedCustomerResponse(
    val content: List<CustomerResponse>,
    val totalElements: Long,
    val totalPages: Int,
    val page: Int,
    val size: Int,
)

fun Customer.toResponse() =
    CustomerResponse(
        id = id,
        firstName = firstName,
        lastName = lastName,
        email = email,
        phoneNumber = phoneNumber,
        notes = notes,
        createdAt = createdAt,
    )

fun PageResult<Customer>.toResponse() =
    PagedCustomerResponse(
        content = content.map { it.toResponse() },
        totalElements = totalElements,
        totalPages = totalPages,
        page = page,
        size = size,
    )
