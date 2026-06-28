package com.stringpro.application.ports.`in`.job

import com.stringpro.application.domain.model.job.Job

interface GetJobUseCase {
    fun get(id: String): Job
}
