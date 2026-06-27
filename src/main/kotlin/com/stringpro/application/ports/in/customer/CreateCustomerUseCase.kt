package com.stringpro.application.ports.`in`.customer

import com.stringpro.application.domain.model.customer.Customer

interface CreateCustomerUseCase {
    fun create(command: CreateCustomerCommand): Customer
}

data class CreateCustomerCommand(
    val firstName: String,
    val lastName: String,
    val email: String,
    val phoneNumber: String,
    val notes: String?,
)
