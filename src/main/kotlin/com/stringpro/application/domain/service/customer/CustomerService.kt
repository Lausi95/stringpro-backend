package com.stringpro.application.domain.service.customer

import com.stringpro.application.domain.model.PageResult
import com.stringpro.application.domain.model.customer.Customer
import com.stringpro.application.domain.model.customer.CustomerNotFoundException
import com.stringpro.application.domain.model.customer.EmailAlreadyExistsException
import com.stringpro.application.ports.`in`.customer.CreateCustomerCommand
import com.stringpro.application.ports.`in`.customer.CreateCustomerUseCase
import com.stringpro.application.ports.`in`.customer.DeleteCustomerUseCase
import com.stringpro.application.ports.`in`.customer.GetCustomerUseCase
import com.stringpro.application.ports.`in`.customer.ListCustomersQuery
import com.stringpro.application.ports.`in`.customer.ListCustomersUseCase
import com.stringpro.application.ports.`in`.customer.UpdateCustomerCommand
import com.stringpro.application.ports.`in`.customer.UpdateCustomerUseCase
import com.stringpro.application.ports.out.customer.CustomerRepository
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

@Service
class CustomerService(
    private val customerRepository: CustomerRepository,
) : CreateCustomerUseCase, GetCustomerUseCase, ListCustomersUseCase, UpdateCustomerUseCase, DeleteCustomerUseCase {

    override fun create(command: CreateCustomerCommand): Customer {
        customerRepository.findByEmail(command.email)?.let {
            throw EmailAlreadyExistsException(command.email)
        }
        return customerRepository.save(
            Customer(
                id = UUID.randomUUID().toString(),
                firstName = command.firstName,
                lastName = command.lastName,
                email = command.email,
                phoneNumber = command.phoneNumber,
                notes = command.notes,
                createdAt = Instant.now(),
            ),
        )
    }

    override fun get(id: String): Customer =
        customerRepository.findById(id) ?: throw CustomerNotFoundException(id)

    override fun list(query: ListCustomersQuery): PageResult<Customer> =
        customerRepository.findAll(query.page, query.size, query.name)

    override fun update(id: String, command: UpdateCustomerCommand): Customer {
        val existing = customerRepository.findById(id) ?: throw CustomerNotFoundException(id)
        if (existing.email != command.email) {
            customerRepository.findByEmail(command.email)?.let {
                throw EmailAlreadyExistsException(command.email)
            }
        }
        return customerRepository.save(
            existing.copy(
                firstName = command.firstName,
                lastName = command.lastName,
                email = command.email,
                phoneNumber = command.phoneNumber,
                notes = command.notes,
            ),
        )
    }

    override fun delete(id: String) {
        val existing = customerRepository.findById(id) ?: throw CustomerNotFoundException(id)
        customerRepository.save(existing.copy(deletedAt = Instant.now()))
    }
}
