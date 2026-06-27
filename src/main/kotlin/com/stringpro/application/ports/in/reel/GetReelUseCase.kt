package com.stringpro.application.ports.`in`.reel

import com.stringpro.application.domain.model.reel.Reel

interface GetReelUseCase {
    fun get(id: String): Reel
}
