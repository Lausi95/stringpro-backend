package com.stringpro.application.ports.`in`.job

import com.stringpro.application.domain.model.PageResult
import com.stringpro.application.domain.model.job.Job
import com.stringpro.application.domain.model.job.Stage

interface ListJobsUseCase {
    fun list(query: ListJobsQuery): PageResult<Job>
}

data class ListJobsQuery(
    val page: Int,
    val size: Int,
    val stage: Stage?,
    val customerId: String?,
    val racketId: String?,
    val reelId: String?,
)
