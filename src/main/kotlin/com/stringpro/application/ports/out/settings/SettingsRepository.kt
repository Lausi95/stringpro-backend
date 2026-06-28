package com.stringpro.application.ports.out.settings

import com.stringpro.application.domain.model.settings.Settings

interface SettingsRepository {
    fun find(): Settings?

    fun save(settings: Settings): Settings
}
