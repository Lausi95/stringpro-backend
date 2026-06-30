package com.stringpro.application.domain.service.job

import com.stringpro.application.domain.model.PageResult
import com.stringpro.application.domain.model.customer.CustomerNotFoundException
import com.stringpro.application.domain.model.job.InvalidStringSetupException
import com.stringpro.application.domain.model.job.Job
import com.stringpro.application.domain.model.job.JobNotFoundException
import com.stringpro.application.domain.model.job.RacketNotOwnedByCustomerException
import com.stringpro.application.domain.model.job.Stage
import com.stringpro.application.domain.model.job.StringChoice
import com.stringpro.application.domain.model.job.StringSourceType
import com.stringpro.application.domain.model.racket.RacketNotFoundException
import com.stringpro.application.domain.model.reel.ReelNotFoundException
import com.stringpro.application.ports.`in`.job.ChangeJobStageCommand
import com.stringpro.application.ports.`in`.job.ChangeJobStageUseCase
import com.stringpro.application.ports.`in`.job.CreateJobCommand
import com.stringpro.application.ports.`in`.job.CreateJobUseCase
import com.stringpro.application.ports.`in`.job.DeleteJobUseCase
import com.stringpro.application.ports.`in`.job.GetJobUseCase
import com.stringpro.application.ports.`in`.job.ListJobsQuery
import com.stringpro.application.ports.`in`.job.ListJobsUseCase
import com.stringpro.application.ports.`in`.job.StringChoiceInput
import com.stringpro.application.ports.`in`.job.UpdateJobCommand
import com.stringpro.application.ports.`in`.job.UpdateJobUseCase
import com.stringpro.application.ports.out.customer.CustomerRepository
import com.stringpro.application.ports.out.job.JobRepository
import com.stringpro.application.ports.out.payment.PaymentRepository
import com.stringpro.application.ports.out.racket.RacketRepository
import com.stringpro.application.ports.out.reel.ReelRepository
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

@Service
class JobService(
    private val jobRepository: JobRepository,
    private val customerRepository: CustomerRepository,
    private val racketRepository: RacketRepository,
    private val reelRepository: ReelRepository,
    private val paymentRepository: PaymentRepository,
) : CreateJobUseCase,
    GetJobUseCase,
    ListJobsUseCase,
    UpdateJobUseCase,
    ChangeJobStageUseCase,
    DeleteJobUseCase {
    override fun create(command: CreateJobCommand): Job {
        customerRepository.findById(command.customerId)
            ?: throw CustomerNotFoundException(command.customerId)
        val racket =
            racketRepository.findById(command.racketId)
                ?: throw RacketNotFoundException(command.racketId)
        if (racket.customerId != command.customerId) {
            throw RacketNotOwnedByCustomerException(command.racketId, command.customerId)
        }

        requireHybridConsistency(command.hybrid, command.crosses)
        val mains = resolveStringChoice(command.mains, existing = null)
        val crosses = command.crosses?.let { resolveStringChoice(it, existing = null) }

        return jobRepository.save(
            Job(
                id = UUID.randomUUID().toString(),
                customerId = command.customerId,
                racketId = command.racketId,
                dueDate = command.dueDate,
                notes = command.notes,
                mainsTensionDeciKg = command.mainsTensionDeciKg,
                crossesTensionDeciKg = command.crossesTensionDeciKg,
                hybrid = command.hybrid,
                mainsString = mains,
                crossesString = crosses,
                serviceFeeCents = command.serviceFeeCents,
                stage = Stage.ANNOUNCED,
                createdAt = Instant.now(),
            ),
        )
    }

    override fun get(id: String): Job = jobRepository.findById(id) ?: throw JobNotFoundException(id)

    override fun list(query: ListJobsQuery): PageResult<Job> =
        jobRepository.findAll(
            query.page,
            query.size,
            query.stage,
            query.customerId,
            query.racketId,
            query.reelId,
            query.fullyPaid,
        )

    override fun update(
        id: String,
        command: UpdateJobCommand,
    ): Job {
        val existing = jobRepository.findById(id) ?: throw JobNotFoundException(id)

        requireHybridConsistency(command.hybrid, command.crosses)
        val mains = resolveStringChoice(command.mains, existing.mainsString)
        val crosses = command.crosses?.let { resolveStringChoice(it, existing.crossesString) }

        // The price may change, so fullyPaid must be re-derived against the new total in the same
        // copy (a later withAmountPaid would be too late — the copy's init invariant runs first).
        // amountPaidCents is preserved from the existing Job; only the threshold moves.
        val newTotalCents = command.serviceFeeCents + mains.feeCents + (crosses?.feeCents ?: 0)

        return jobRepository.save(
            existing.copy(
                dueDate = command.dueDate,
                notes = command.notes,
                mainsTensionDeciKg = command.mainsTensionDeciKg,
                crossesTensionDeciKg = command.crossesTensionDeciKg,
                hybrid = command.hybrid,
                mainsString = mains,
                crossesString = crosses,
                serviceFeeCents = command.serviceFeeCents,
                fullyPaid = existing.amountPaidCents >= newTotalCents,
            ),
        )
    }

    override fun changeStage(
        id: String,
        command: ChangeJobStageCommand,
    ): Job {
        val existing = jobRepository.findById(id) ?: throw JobNotFoundException(id)
        return jobRepository.save(existing.withStage(command.targetStage))
    }

    override fun delete(id: String) {
        val existing = jobRepository.findById(id) ?: throw JobNotFoundException(id)
        val now = Instant.now()
        jobRepository.save(existing.copy(deletedAt = now))
        // Cascade: a Payment must never outlive its visible Job.
        paymentRepository.findAllByJobId(id).forEach { payment ->
            paymentRepository.save(payment.copy(deletedAt = now))
        }
    }

    private fun requireHybridConsistency(
        hybrid: Boolean,
        crosses: StringChoiceInput?,
    ) {
        if (hybrid != (crosses != null)) {
            throw InvalidStringSetupException(
                "A hybrid Job must supply a crosses string; a non-hybrid Job must not",
            )
        }
    }

    /**
     * Resolve an input into a domain [StringChoice]. A REEL side's referenced Reel is validated
     * for existence only when it differs from [existing] (so a Job keeping a since-deleted Reel
     * stays editable). Pass [existing] = null on create to always validate.
     */
    private fun resolveStringChoice(
        input: StringChoiceInput,
        existing: StringChoice?,
    ): StringChoice =
        when (input.type) {
            StringSourceType.OWN -> {
                val name =
                    input.stringName?.takeIf { it.isNotBlank() }
                        ?: throw InvalidStringSetupException("An own string requires a name")
                StringChoice.Own(name)
            }

            StringSourceType.REEL -> {
                val reelId =
                    input.reelId
                        ?: throw InvalidStringSetupException("A reel string requires a reelId")
                val feeCents =
                    input.stringFeeCents
                        ?: throw InvalidStringSetupException("A reel string requires a string fee")
                val unchanged = existing is StringChoice.Reel && existing.reelId == reelId
                if (!unchanged) {
                    reelRepository.findById(reelId) ?: throw ReelNotFoundException(reelId)
                }
                StringChoice.Reel(reelId, feeCents)
            }
        }
}
