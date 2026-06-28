package com.stringpro.application.ports.out.customer

import com.stringpro.application.domain.model.PageResult
import com.stringpro.application.domain.model.customer.Customer

interface CustomerRepository {
    fun save(customer: Customer): Customer

    fun findById(id: String): Customer?

    fun findByEmail(email: String): Customer?

    fun findAll(
        page: Int,
        size: Int,
        name: String?,
    ): PageResult<Customer>
}
