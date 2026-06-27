package com.stringpro.application.domain.model.customer

class CustomerNotFoundException(id: String) : RuntimeException("Customer not found: $id")
