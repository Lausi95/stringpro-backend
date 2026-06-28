package com.stringpro.application.ports.`in`.job

import com.stringpro.application.domain.model.job.StringSourceType

/**
 * One side's string choice as it arrives in a command. The service resolves it into a domain
 * [com.stringpro.application.domain.model.job.StringChoice]: OWN requires [stringName] (fee is
 * always zero); REEL requires [reelId] and [stringFeeCents].
 */
data class StringChoiceInput(
    val type: StringSourceType,
    val stringName: String?,
    val reelId: String?,
    val stringFeeCents: Long?,
)
