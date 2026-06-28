package com.stringpro.infrastructure.web

import java.math.BigDecimal
import java.math.RoundingMode

// Shared euro <-> integer-cent conversions for the API edge (see ADR 0002).

fun eurosToCents(euros: BigDecimal): Long = euros.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact()

fun centsToEuros(cents: Long): BigDecimal = BigDecimal.valueOf(cents).movePointLeft(2)
