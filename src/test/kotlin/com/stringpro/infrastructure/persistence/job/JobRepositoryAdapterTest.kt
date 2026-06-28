package com.stringpro.infrastructure.persistence.job

import com.stringpro.application.domain.model.job.Job
import com.stringpro.application.domain.model.job.Stage
import com.stringpro.application.domain.model.job.StringChoice
import com.stringpro.application.domain.model.job.StringSourceType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest
import org.springframework.data.mongodb.core.MongoTemplate
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@DataMongoTest
class JobRepositoryAdapterTest {
    @Autowired private lateinit var mongoRepository: JobMongoRepository

    @Autowired private lateinit var mongoTemplate: MongoTemplate
    private lateinit var adapter: JobRepositoryAdapter

    @BeforeEach
    fun setUp() {
        mongoRepository.deleteAll()
        adapter = JobRepositoryAdapter(mongoRepository, mongoTemplate)
    }

    @Test
    fun `should round-trip a non-hybrid reel job`() {
        val saved = adapter.save(aJob(mains = StringChoice.Reel("reel-1", 3000)))

        val found = adapter.findById(saved.id)

        assertNotNull(found)
        assertEquals(false, found!!.hybrid)
        assertEquals(StringSourceType.REEL, found.mainsString.sourceType)
        assertEquals(3000, found.mainsString.feeCents)
        assertNull(found.crossesString)
        assertEquals(Stage.ANNOUNCED, found.stage)
        assertEquals(LocalDate.of(2026, 12, 1), found.dueDate)
    }

    @Test
    fun `should round-trip a non-hybrid own-string job`() {
        val saved = adapter.save(aJob(mains = StringChoice.Own("Wilson NXT")))

        val found = adapter.findById(saved.id)!!

        assertEquals(StringSourceType.OWN, found.mainsString.sourceType)
        assertEquals("Wilson NXT", (found.mainsString as StringChoice.Own).stringName)
        assertEquals(0, found.mainsString.feeCents)
    }

    @Test
    fun `should round-trip a hybrid own plus reel job`() {
        val saved =
            adapter.save(
                aJob(
                    hybrid = true,
                    mains = StringChoice.Own("Natural Gut"),
                    crosses = StringChoice.Reel("reel-2", 2000),
                ),
            )

        val found = adapter.findById(saved.id)!!

        assertTrue(found.hybrid)
        assertEquals(StringSourceType.OWN, found.mainsString.sourceType)
        assertEquals(StringSourceType.REEL, found.crossesString!!.sourceType)
        assertEquals("reel-2", (found.crossesString as StringChoice.Reel).reelId)
        assertEquals(2000, found.crossesString!!.feeCents)
    }

    @Test
    fun `should round-trip a hybrid own plus own job`() {
        val saved =
            adapter.save(
                aJob(
                    hybrid = true,
                    mains = StringChoice.Own("Natural Gut"),
                    crosses = StringChoice.Own("Synthetic Gut"),
                ),
            )

        val found = adapter.findById(saved.id)!!

        assertEquals(StringSourceType.OWN, found.mainsString.sourceType)
        assertEquals("Natural Gut", (found.mainsString as StringChoice.Own).stringName)
        assertEquals(StringSourceType.OWN, found.crossesString!!.sourceType)
        assertEquals("Synthetic Gut", (found.crossesString as StringChoice.Own).stringName)
        assertEquals(0, found.totalStringFeeCents)
    }

    @Test
    fun `should round-trip a hybrid reel plus reel job`() {
        val saved =
            adapter.save(
                aJob(
                    hybrid = true,
                    mains = StringChoice.Reel("reel-1", 3000),
                    crosses = StringChoice.Reel("reel-2", 2000),
                ),
            )

        val found = adapter.findById(saved.id)!!

        assertEquals("reel-1", (found.mainsString as StringChoice.Reel).reelId)
        assertEquals("reel-2", (found.crossesString as StringChoice.Reel).reelId)
        assertEquals(5000, found.totalStringFeeCents)
    }

    @Test
    fun `should not find a soft-deleted job`() {
        val saved = adapter.save(aJob())
        adapter.save(saved.copy(deletedAt = Instant.now()))

        assertNull(adapter.findById(saved.id))
    }

    @Test
    fun `should exclude soft-deleted jobs from list`() {
        adapter.save(aJob())
        val toDelete = adapter.save(aJob())
        adapter.save(toDelete.copy(deletedAt = Instant.now()))

        val result = adapter.findAll(0, 20, null, null, null, null)

        assertEquals(1, result.totalElements)
    }

    @Test
    fun `should filter by stage`() {
        adapter.save(aJob(stage = Stage.IN_PROGRESS))
        adapter.save(aJob(stage = Stage.IN_PROGRESS))
        adapter.save(aJob(stage = Stage.ANNOUNCED))

        val result = adapter.findAll(0, 20, Stage.IN_PROGRESS, null, null, null)

        assertEquals(2, result.totalElements)
        assertTrue(result.content.all { it.stage == Stage.IN_PROGRESS })
    }

    @Test
    fun `should filter by customer`() {
        adapter.save(aJob(customerId = "cust-1"))
        adapter.save(aJob(customerId = "cust-2"))

        val result = adapter.findAll(0, 20, null, "cust-1", null, null)

        assertEquals(1, result.totalElements)
        assertEquals("cust-1", result.content.single().customerId)
    }

    @Test
    fun `should filter by reel matching either side`() {
        adapter.save(aJob(mains = StringChoice.Reel("reel-1", 3000))) // mains match
        adapter.save(
            aJob(
                hybrid = true,
                mains = StringChoice.Own("Gut"),
                crosses = StringChoice.Reel("reel-1", 2000),
            ),
        ) // crosses match
        adapter.save(aJob(mains = StringChoice.Reel("reel-9", 3000))) // no match

        val result = adapter.findAll(0, 20, null, null, null, "reel-1")

        assertEquals(2, result.totalElements)
    }

    @Test
    fun `should combine filters with AND`() {
        adapter.save(aJob(stage = Stage.IN_PROGRESS, customerId = "cust-1"))
        adapter.save(aJob(stage = Stage.IN_PROGRESS, customerId = "cust-2"))
        adapter.save(aJob(stage = Stage.ANNOUNCED, customerId = "cust-1"))

        val result = adapter.findAll(0, 20, Stage.IN_PROGRESS, "cust-1", null, null)

        assertEquals(1, result.totalElements)
    }

    @Test
    fun `should sort by due date ascending`() {
        adapter.save(aJob(dueDate = LocalDate.of(2026, 3, 1)))
        adapter.save(aJob(dueDate = LocalDate.of(2026, 1, 1)))
        adapter.save(aJob(dueDate = LocalDate.of(2026, 2, 1)))

        val result = adapter.findAll(0, 20, null, null, null, null)

        assertEquals(
            listOf(
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 2, 1),
                LocalDate.of(2026, 3, 1),
            ),
            result.content.map { it.dueDate },
        )
    }

    @Test
    fun `should respect page and size boundaries`() {
        repeat(3) { adapter.save(aJob()) }

        val firstPage = adapter.findAll(0, 2, null, null, null, null)
        val secondPage = adapter.findAll(1, 2, null, null, null, null)

        assertEquals(3, firstPage.totalElements)
        assertEquals(2, firstPage.totalPages)
        assertEquals(2, firstPage.content.size)
        assertEquals(1, secondPage.content.size)
    }

    private fun aJob(
        customerId: String = "cust-1",
        racketId: String = "rack-1",
        stage: Stage = Stage.ANNOUNCED,
        hybrid: Boolean = false,
        mains: StringChoice = StringChoice.Reel("reel-1", 3000),
        crosses: StringChoice? = null,
        dueDate: LocalDate = LocalDate.of(2026, 12, 1),
    ) = Job(
        id = UUID.randomUUID().toString(),
        customerId = customerId,
        racketId = racketId,
        dueDate = dueDate,
        notes = null,
        mainsTensionDeciKg = 235,
        crossesTensionDeciKg = 225,
        hybrid = hybrid,
        mainsString = mains,
        crossesString = crosses,
        serviceFeeCents = 2500,
        stage = stage,
        createdAt = Instant.now(),
    )
}
