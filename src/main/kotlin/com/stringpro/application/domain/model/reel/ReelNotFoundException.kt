package com.stringpro.application.domain.model.reel

class ReelNotFoundException(id: String) : RuntimeException("Reel not found: $id")
