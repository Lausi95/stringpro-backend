package com.stringpro.application.ports.`in`.customer

import com.stringpro.application.domain.model.customer.Customer

interface GetCustomerUseCase {
    fun get(id: String): Customer
}
