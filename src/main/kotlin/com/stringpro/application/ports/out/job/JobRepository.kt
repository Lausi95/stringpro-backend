package com.stringpro.application.ports.out.job

import com.stringpro.application.domain.model.PageResult
import com.stringpro.application.domain.model.job.Job
import com.stringpro.application.domain.model.job.Stage

interface JobRepository {
    fun save(job: Job): Job

    fun findById(id: String): Job?

    fun findAll(
        page: Int,
        size: Int,
        stage: Stage?,
        customerId: String?,
        racketId: String?,
        reelId: String?,
        fullyPaid: Boolean?,
    ): PageResult<Job>
}
