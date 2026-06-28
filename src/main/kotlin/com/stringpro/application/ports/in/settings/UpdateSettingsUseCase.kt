package com.stringpro.application.ports.`in`.settings

import com.stringpro.application.domain.model.settings.Settings

interface UpdateSettingsUseCase {
    fun update(command: UpdateSettingsCommand): Settings
}

data class UpdateSettingsCommand(
    val serviceFeeCents: Long,
    val fullName: String,
    val email: String,
    val iban: String,
    val address: String,
)
