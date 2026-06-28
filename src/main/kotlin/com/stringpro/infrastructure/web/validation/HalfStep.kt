package com.stringpro.infrastructure.web.validation

import jakarta.validation.Constraint
import jakarta.validation.Payload
import kotlin.reflect.KClass

/** A decimal value that must be a multiple of 0.5 (e.g. tension in half-kg steps). */
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [HalfStepValidator::class])
annotation class HalfStep(
    val message: String = "must be in steps of 0.5",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = [],
)
