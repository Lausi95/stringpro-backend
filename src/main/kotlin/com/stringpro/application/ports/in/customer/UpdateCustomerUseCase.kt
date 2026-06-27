package com.stringpro.application.ports.`in`.customer

import com.stringpro.application.domain.model.customer.Customer

interface UpdateCustomerUseCase {
    fun update(id: String, command: UpdateCustomerCommand): Customer
}

data class UpdateCustomerCommand(
    val firstName: String,
    val lastName: String,
    val email: String,
    val phoneNumber: String,
    val notes: String?,
)
