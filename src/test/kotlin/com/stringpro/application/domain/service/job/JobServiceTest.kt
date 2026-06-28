package com.stringpro.application.domain.service.job

import com.stringpro.application.domain.model.PageResult
import com.stringpro.application.domain.model.customer.Customer
import com.stringpro.application.domain.model.customer.CustomerNotFoundException
import com.stringpro.application.domain.model.job.InvalidStageTransitionException
import com.stringpro.application.domain.model.job.InvalidStringSetupException
import com.stringpro.application.domain.model.job.Job
import com.stringpro.application.domain.model.job.JobNotFoundException
import com.stringpro.application.domain.model.job.RacketNotOwnedByCustomerException
import com.stringpro.application.domain.model.job.Stage
import com.stringpro.application.domain.model.job.StringChoice
import com.stringpro.application.domain.model.job.StringSourceType
import com.stringpro.application.domain.model.racket.Racket
import com.stringpro.application.domain.model.racket.RacketNotFoundException
import com.stringpro.application.domain.model.reel.Material
import com.stringpro.application.domain.model.reel.Reel
import com.stringpro.application.domain.model.reel.ReelNotFoundException
import com.stringpro.application.domain.model.reel.ReelState
import com.stringpro.application.ports.`in`.job.ChangeJobStageCommand
import com.stringpro.application.ports.`in`.job.CreateJobCommand
import com.stringpro.application.ports.`in`.job.ListJobsQuery
import com.stringpro.application.ports.`in`.job.StringChoiceInput
import com.stringpro.application.ports.`in`.job.UpdateJobCommand
import com.stringpro.application.ports.out.customer.CustomerRepository
import com.stringpro.application.ports.out.job.JobRepository
import com.stringpro.application.ports.out.racket.RacketRepository
import com.stringpro.application.ports.out.reel.ReelRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant
import java.time.LocalDate

class JobServiceTest {
    private val jobRepository: JobRepository = mockk()
    private val customerRepository: CustomerRepository = mockk()
    private val racketRepository: RacketRepository = mockk()
    private val reelRepository: ReelRepository = mockk()
    private val service = JobService(jobRepository, customerRepository, racketRepository, reelRepository)

    @Test
    fun `should create job at ANNOUNCED when references are valid`() {
        val slot = slot<Job>()
        every { customerRepository.findById("cust-1") } returns aCustomer()
        every { racketRepository.findById("rack-1") } returns aRacket()
        every { reelRepository.findById("reel-1") } returns aReel()
        every { jobRepository.save(capture(slot)) } answers { slot.captured }

        val result = service.create(aCreateCommand())

        assertNotNull(result.id)
        assertEquals("cust-1", result.customerId)
        assertEquals("rack-1", result.racketId)
        assertEquals(Stage.ANNOUNCED, result.stage)
        assertEquals(StringSourceType.REEL, result.mainsString.sourceType)
        assertEquals(3000, result.mainsString.feeCents)
        assertNull(result.crossesString)
        assertNull(result.deletedAt)
    }

    @Test
    fun `should throw CustomerNotFoundException when customer is missing`() {
        every { customerRepository.findById("cust-1") } returns null

        assertThrows<CustomerNotFoundException> { service.create(aCreateCommand()) }
        verify(exactly = 0) { jobRepository.save(any()) }
    }

    @Test
    fun `should throw RacketNotFoundException when racket is missing`() {
        every { customerRepository.findById("cust-1") } returns aCustomer()
        every { racketRepository.findById("rack-1") } returns null

        assertThrows<RacketNotFoundException> { service.create(aCreateCommand()) }
        verify(exactly = 0) { jobRepository.save(any()) }
    }

    @Test
    fun `should throw RacketNotOwnedByCustomerException when racket belongs to another customer`() {
        every { customerRepository.findById("cust-1") } returns aCustomer()
        every { racketRepository.findById("rack-1") } returns aRacket(customerId = "other")

        assertThrows<RacketNotOwnedByCustomerException> { service.create(aCreateCommand()) }
        verify(exactly = 0) { jobRepository.save(any()) }
    }

    @Test
    fun `should throw ReelNotFoundException when a reel side references a missing reel`() {
        every { customerRepository.findById("cust-1") } returns aCustomer()
        every { racketRepository.findById("rack-1") } returns aRacket()
        every { reelRepository.findById("reel-1") } returns null

        assertThrows<ReelNotFoundException> { service.create(aCreateCommand()) }
        verify(exactly = 0) { jobRepository.save(any()) }
    }

    @Test
    fun `should throw InvalidStringSetupException when hybrid is true but crosses is missing`() {
        every { customerRepository.findById("cust-1") } returns aCustomer()
        every { racketRepository.findById("rack-1") } returns aRacket()

        assertThrows<InvalidStringSetupException> {
            service.create(aCreateCommand(hybrid = true, crosses = null))
        }
    }

    @Test
    fun `should throw InvalidStringSetupException when crosses is present but not hybrid`() {
        every { customerRepository.findById("cust-1") } returns aCustomer()
        every { racketRepository.findById("rack-1") } returns aRacket()

        assertThrows<InvalidStringSetupException> {
            service.create(aCreateCommand(hybrid = false, crosses = ownInput("Gut")))
        }
    }

    @Test
    fun `should force own-string fee to zero`() {
        val slot = slot<Job>()
        every { customerRepository.findById("cust-1") } returns aCustomer()
        every { racketRepository.findById("rack-1") } returns aRacket()
        every { jobRepository.save(capture(slot)) } answers { slot.captured }

        val result = service.create(aCreateCommand(mains = ownInput("Wilson NXT")))

        assertEquals(StringSourceType.OWN, result.mainsString.sourceType)
        assertEquals(0, result.mainsString.feeCents)
        assertEquals(2500, result.totalCents - result.totalStringFeeCents) // only service fee remains
    }

    @Test
    fun `should sum string fees of both reels for a hybrid job`() {
        val slot = slot<Job>()
        every { customerRepository.findById("cust-1") } returns aCustomer()
        every { racketRepository.findById("rack-1") } returns aRacket()
        every { reelRepository.findById("reel-1") } returns aReel(id = "reel-1")
        every { reelRepository.findById("reel-2") } returns aReel(id = "reel-2")
        every { jobRepository.save(capture(slot)) } answers { slot.captured }

        val result =
            service.create(
                aCreateCommand(
                    hybrid = true,
                    mains = reelInput("reel-1", 3000),
                    crosses = reelInput("reel-2", 2000),
                ),
            )

        assertEquals(5000, result.totalStringFeeCents)
        assertEquals(7500, result.totalCents) // 2500 service + 5000 string
    }

    @Test
    fun `should get job by id`() {
        every { jobRepository.findById("job-1") } returns aJob()

        assertEquals("job-1", service.get("job-1").id)
    }

    @Test
    fun `should throw JobNotFoundException when job does not exist`() {
        every { jobRepository.findById("unknown") } returns null

        assertThrows<JobNotFoundException> { service.get("unknown") }
    }

    @Test
    fun `should pass list filters through to repository`() {
        val query = ListJobsQuery(0, 20, Stage.IN_PROGRESS, "cust-1", "rack-1", "reel-1")
        every {
            jobRepository.findAll(0, 20, Stage.IN_PROGRESS, "cust-1", "rack-1", "reel-1")
        } returns PageResult(listOf(aJob()), 1L, 1, 0, 20)

        val result = service.list(query)

        assertEquals(1, result.totalElements)
    }

    @Test
    fun `should not change customer or racket on update`() {
        val slot = slot<Job>()
        every { jobRepository.findById("job-1") } returns aJob(customerId = "cust-1", racketId = "rack-1")
        every { jobRepository.save(capture(slot)) } answers { slot.captured }

        val result = service.update("job-1", anUpdateCommand(mains = ownInput("Gut")))

        assertEquals("cust-1", result.customerId)
        assertEquals("rack-1", result.racketId)
    }

    @Test
    fun `should not re-validate reel on update when reelId is unchanged`() {
        val slot = slot<Job>()
        every { jobRepository.findById("job-1") } returns aJob(mains = StringChoice.Reel("reel-1", 3000))
        every { jobRepository.save(capture(slot)) } answers { slot.captured }

        // No reelRepository stub: if the service looked it up, the test would fail with a MockK error.
        val result = service.update("job-1", anUpdateCommand(mains = reelInput("reel-1", 9999)))

        assertEquals(9999, result.mainsString.feeCents)
        verify(exactly = 0) { reelRepository.findById(any()) }
    }

    @Test
    fun `should re-validate reel on update when reelId changes`() {
        every { jobRepository.findById("job-1") } returns aJob(mains = StringChoice.Reel("reel-1", 3000))
        every { reelRepository.findById("reel-9") } returns null

        assertThrows<ReelNotFoundException> {
            service.update("job-1", anUpdateCommand(mains = reelInput("reel-9", 3000)))
        }
    }

    @Test
    fun `should throw JobNotFoundException when updating a missing job`() {
        every { jobRepository.findById("unknown") } returns null

        assertThrows<JobNotFoundException> { service.update("unknown", anUpdateCommand()) }
    }

    @Test
    fun `should advance stage forward`() {
        val slot = slot<Job>()
        every { jobRepository.findById("job-1") } returns aJob(stage = Stage.ANNOUNCED)
        every { jobRepository.save(capture(slot)) } answers { slot.captured }

        val result = service.changeStage("job-1", ChangeJobStageCommand(Stage.IN_PROGRESS))

        assertEquals(Stage.IN_PROGRESS, result.stage)
    }

    @Test
    fun `should reject moving stage backward`() {
        every { jobRepository.findById("job-1") } returns aJob(stage = Stage.DONE)

        assertThrows<InvalidStageTransitionException> {
            service.changeStage("job-1", ChangeJobStageCommand(Stage.PICKED_UP))
        }
    }

    @Test
    fun `should soft delete job`() {
        val slot = slot<Job>()
        every { jobRepository.findById("job-1") } returns aJob()
        every { jobRepository.save(capture(slot)) } answers { slot.captured }

        service.delete("job-1")

        assertNotNull(slot.captured.deletedAt)
    }

    @Test
    fun `should throw JobNotFoundException when deleting a missing job`() {
        every { jobRepository.findById("unknown") } returns null

        assertThrows<JobNotFoundException> { service.delete("unknown") }
    }

    // --- builders ---

    private fun reelInput(
        reelId: String,
        feeCents: Long,
    ) = StringChoiceInput(StringSourceType.REEL, null, reelId, feeCents)

    private fun ownInput(name: String) = StringChoiceInput(StringSourceType.OWN, name, null, null)

    private fun aCreateCommand(
        hybrid: Boolean = false,
        mains: StringChoiceInput = reelInput("reel-1", 3000),
        crosses: StringChoiceInput? = null,
    ) = CreateJobCommand(
        customerId = "cust-1",
        racketId = "rack-1",
        dueDate = LocalDate.of(2026, 12, 1),
        notes = "handle with care",
        mainsTensionDeciKg = 235,
        crossesTensionDeciKg = 225,
        hybrid = hybrid,
        mains = mains,
        crosses = crosses,
        serviceFeeCents = 2500,
    )

    private fun anUpdateCommand(
        hybrid: Boolean = false,
        mains: StringChoiceInput = reelInput("reel-1", 3000),
        crosses: StringChoiceInput? = null,
    ) = UpdateJobCommand(
        // a past date — updates allow any date
        dueDate = LocalDate.of(2020, 1, 1),
        notes = "edited",
        mainsTensionDeciKg = 240,
        crossesTensionDeciKg = 240,
        hybrid = hybrid,
        mains = mains,
        crosses = crosses,
        serviceFeeCents = 2600,
    )

    private fun aCustomer(id: String = "cust-1") =
        Customer(
            id = id,
            firstName = "Tom",
            lastName = "Lausmann",
            email = "tom@example.com",
            phoneNumber = "+49123456",
            notes = null,
            createdAt = Instant.now(),
        )

    private fun aRacket(
        id: String = "rack-1",
        customerId: String = "cust-1",
    ) = Racket(
        id = id,
        customerId = customerId,
        brand = "Babolat",
        model = "Pure Aero",
        headSize = 645,
        stringMains = 16,
        stringCrosses = 19,
        notes = null,
        createdAt = Instant.now(),
    )

    private fun aReel(id: String = "reel-1") =
        Reel(
            id = id,
            brand = "Luxilon",
            model = "ALU Power",
            material = Material.POLYESTER,
            gaugeHundredthsMm = 125,
            reelLengthMeters = 200,
            costCents = 12000,
            stringFeeCents = 2500,
            metersPerJob = 11,
            purchaseDate = LocalDate.of(2026, 1, 15),
            state = ReelState.NEW,
            createdAt = Instant.now(),
        )

    private fun aJob(
        id: String = "job-1",
        customerId: String = "cust-1",
        racketId: String = "rack-1",
        stage: Stage = Stage.ANNOUNCED,
        mains: StringChoice = StringChoice.Reel("reel-1", 3000),
    ) = Job(
        id = id,
        customerId = customerId,
        racketId = racketId,
        dueDate = LocalDate.of(2026, 12, 1),
        notes = null,
        mainsTensionDeciKg = 235,
        crossesTensionDeciKg = 225,
        hybrid = false,
        mainsString = mains,
        crossesString = null,
        serviceFeeCents = 2500,
        stage = stage,
        createdAt = Instant.now(),
    )
}
