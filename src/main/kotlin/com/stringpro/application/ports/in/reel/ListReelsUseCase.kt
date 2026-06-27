package com.stringpro.application.ports.`in`.reel

import com.stringpro.application.domain.model.reel.Reel
import com.stringpro.application.domain.model.reel.ReelState

interface ListReelsUseCase {
    fun list(query: ListReelsQuery): List<Reel>
}

data class ListReelsQuery(
    val state: ReelState?,
)
