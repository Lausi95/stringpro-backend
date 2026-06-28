package com.stringpro.application.ports.out.reel

import com.stringpro.application.domain.model.PageResult
import com.stringpro.application.domain.model.reel.Reel
import com.stringpro.application.domain.model.reel.ReelState

interface ReelRepository {
    fun save(reel: Reel): Reel

    fun findById(id: String): Reel?

    fun findAll(
        page: Int,
        size: Int,
        state: ReelState?,
    ): PageResult<Reel>
}
