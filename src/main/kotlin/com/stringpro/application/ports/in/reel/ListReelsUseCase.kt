package com.stringpro.application.ports.`in`.reel

import com.stringpro.application.domain.model.PageResult
import com.stringpro.application.domain.model.reel.Reel
import com.stringpro.application.domain.model.reel.ReelState

interface ListReelsUseCase {
    fun list(query: ListReelsQuery): PageResult<Reel>
}

data class ListReelsQuery(
    val page: Int,
    val size: Int,
    val state: ReelState?,
)
