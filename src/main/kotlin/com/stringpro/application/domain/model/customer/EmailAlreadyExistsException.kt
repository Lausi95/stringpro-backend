package com.stringpro.application.domain.model.customer

class EmailAlreadyExistsException(email: String) : RuntimeException("Email already in use: $email")
