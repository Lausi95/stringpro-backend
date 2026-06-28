package com.stringpro.infrastructure.persistence.settings

import org.springframework.data.mongodb.repository.MongoRepository

interface SettingsMongoRepository : MongoRepository<SettingsDocument, String>
