package com.stringpro.infrastructure.persistence.settings

import com.stringpro.application.domain.model.settings.Settings
import com.stringpro.application.ports.out.settings.SettingsRepository
import org.springframework.stereotype.Component

@Component
class SettingsRepositoryAdapter(
    private val mongoRepository: SettingsMongoRepository,
) : SettingsRepository {
    override fun find(): Settings? = mongoRepository.findById(SINGLETON_ID).orElse(null)?.toDomain()

    override fun save(settings: Settings): Settings = mongoRepository.save(settings.toDocument()).toDomain()

    private fun Settings.toDocument() =
        SettingsDocument(
            id = SINGLETON_ID,
            serviceFeeCents = serviceFeeCents,
            fullName = fullName,
            paypalHandle = paypalHandle,
            iban = iban,
            address = address,
            updatedAt = updatedAt,
        )

    private fun SettingsDocument.toDomain() =
        Settings(
            serviceFeeCents = serviceFeeCents,
            fullName = fullName,
            paypalHandle = paypalHandle,
            iban = iban,
            address = address,
            updatedAt = updatedAt,
        )

    companion object {
        private const val SINGLETON_ID = "settings"
    }
}
