package com.stringpro.application.ports.`in`.customer

import com.stringpro.application.domain.model.PageResult
import com.stringpro.application.domain.model.customer.Customer

interface ListCustomersUseCase {
    fun list(query: ListCustomersQuery): PageResult<Customer>
}

data class ListCustomersQuery(
    val page: Int,
    val size: Int,
    val name: String?,
)
