package com.stringpro.application.ports.`in`.reel

import com.stringpro.application.domain.model.reel.Reel
import com.stringpro.application.domain.model.reel.ReelState

interface ChangeReelStateUseCase {
    fun changeState(id: String, command: ChangeReelStateCommand): Reel
}

data class ChangeReelStateCommand(
    val targetState: ReelState,
)
