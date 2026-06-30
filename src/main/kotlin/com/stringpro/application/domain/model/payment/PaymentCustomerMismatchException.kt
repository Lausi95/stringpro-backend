package com.stringpro.application.domain.model.payment

class PaymentCustomerMismatchException(
    jobId: String,
    customerId: String,
) : RuntimeException("Customer $customerId does not own job $jobId")
