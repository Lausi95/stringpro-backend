package com.stringpro.infrastructure.web.validation

import jakarta.validation.Constraint
import jakarta.validation.Payload
import kotlin.reflect.KClass

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [PaypalHandleValidator::class])
annotation class PaypalHandle(
    val message: String = "must be a valid PayPal.Me handle (1-20 letters or digits)",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = [],
)
