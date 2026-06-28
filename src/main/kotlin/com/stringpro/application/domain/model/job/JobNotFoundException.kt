package com.stringpro.application.domain.model.job

class JobNotFoundException(id: String) : RuntimeException("Job not found: $id")
