package com.stringpro.infrastructure.persistence.customer

import com.stringpro.application.domain.model.PageResult
import com.stringpro.application.domain.model.customer.Customer
import com.stringpro.application.ports.out.customer.CustomerRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component

@Component
class CustomerRepositoryAdapter(
    private val mongoRepository: CustomerMongoRepository,
) : CustomerRepository {
    override fun save(customer: Customer): Customer = mongoRepository.save(customer.toDocument()).toDomain()

    override fun findById(id: String): Customer? = mongoRepository.findByIdAndDeletedAtIsNull(id)?.toDomain()

    override fun findByEmail(email: String): Customer? = mongoRepository.findByEmailAndDeletedAtIsNull(email)?.toDomain()

    override fun findAll(
        page: Int,
        size: Int,
        name: String?,
    ): PageResult<Customer> {
        val pageable = PageRequest.of(page, size)
        val result: Page<CustomerDocument> =
            if (name.isNullOrBlank()) {
                mongoRepository.findAllByDeletedAtIsNull(pageable)
            } else {
                mongoRepository.findAllByNameAndDeletedAtIsNull(name, pageable)
            }
        return PageResult(
            content = result.content.map { it.toDomain() },
            totalElements = result.totalElements,
            totalPages = result.totalPages,
            page = result.number,
            size = result.size,
        )
    }

    private fun Customer.toDocument() =
        CustomerDocument(
            id = id,
            firstName = firstName,
            lastName = lastName,
            email = email,
            phoneNumber = phoneNumber,
            notes = notes,
            createdAt = createdAt,
            deletedAt = deletedAt,
        )

    private fun CustomerDocument.toDomain() =
        Customer(
            id = id,
            firstName = firstName,
            lastName = lastName,
            email = email,
            phoneNumber = phoneNumber,
            notes = notes,
            createdAt = createdAt,
            deletedAt = deletedAt,
        )
}
