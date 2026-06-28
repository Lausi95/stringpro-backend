package com.stringpro.application.ports.`in`.settings

import com.stringpro.application.domain.model.settings.Settings

interface GetSettingsUseCase {
    fun get(): Settings
}
