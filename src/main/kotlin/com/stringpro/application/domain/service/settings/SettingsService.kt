package com.stringpro.application.domain.service.settings

import com.stringpro.application.domain.model.settings.Settings
import com.stringpro.application.ports.`in`.settings.GetSettingsUseCase
import com.stringpro.application.ports.`in`.settings.UpdateSettingsCommand
import com.stringpro.application.ports.`in`.settings.UpdateSettingsUseCase
import com.stringpro.application.ports.out.settings.SettingsRepository
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class SettingsService(
    private val settingsRepository: SettingsRepository,
) : GetSettingsUseCase, UpdateSettingsUseCase {
    override fun get(): Settings = settingsRepository.find() ?: Settings.DEFAULT

    override fun update(command: UpdateSettingsCommand): Settings =
        settingsRepository.save(
            Settings(
                serviceFeeCents = command.serviceFeeCents,
                fullName = command.fullName,
                email = command.email,
                iban = command.iban,
                address = command.address,
                updatedAt = Instant.now(),
            ),
        )
}
