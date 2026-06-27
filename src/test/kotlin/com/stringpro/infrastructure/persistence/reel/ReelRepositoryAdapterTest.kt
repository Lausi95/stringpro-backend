package com.stringpro.infrastructure.persistence.reel

import com.stringpro.application.domain.model.reel.Material
import com.stringpro.application.domain.model.reel.Reel
import com.stringpro.application.domain.model.reel.ReelState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@DataMongoTest
class ReelRepositoryAdapterTest {
    @Autowired private lateinit var mongoRepository: ReelMongoRepository
    private lateinit var adapter: ReelRepositoryAdapter

    @BeforeEach
    fun setUp() {
        mongoRepository.deleteAll()
        adapter = ReelRepositoryAdapter(mongoRepository)
    }

    @Test
    fun `should save and find reel by id`() {
        val saved = adapter.save(aReel())

        val found = adapter.findById(saved.id)

        assertNotNull(found)
        assertEquals(saved.id, found!!.id)
        assertEquals("Luxilon", found.brand)
        assertEquals(Material.POLYESTER, found.material)
        assertEquals(125, found.gaugeHundredthsMm)
        assertEquals(12000, found.costCents)
        assertEquals(ReelState.NEW, found.state)
        assertEquals(LocalDate.of(2026, 1, 15), found.purchaseDate)
    }

    @Test
    fun `should not find soft-deleted reel by id`() {
        val saved = adapter.save(aReel())
        adapter.save(saved.copy(deletedAt = Instant.now()))

        val found = adapter.findById(saved.id)

        assertNull(found)
    }

    @Test
    fun `should find all reels excluding soft-deleted`() {
        adapter.save(aReel())
        val toDelete = adapter.save(aReel())
        adapter.save(toDelete.copy(deletedAt = Instant.now()))

        val result = adapter.findAll(0, 20, null)

        assertEquals(1, result.totalElements)
        assertEquals(1, result.content.size)
    }

    @Test
    fun `should find reels by state excluding soft-deleted`() {
        adapter.save(aReel(state = ReelState.IN_USE))
        adapter.save(aReel(state = ReelState.IN_USE))
        adapter.save(aReel(state = ReelState.NEW))
        val toDelete = adapter.save(aReel(state = ReelState.IN_USE))
        adapter.save(toDelete.copy(deletedAt = Instant.now()))

        val result = adapter.findAll(0, 20, ReelState.IN_USE)

        assertEquals(2, result.totalElements)
        assertTrue(result.content.all { it.state == ReelState.IN_USE })
    }

    @Test
    fun `should return empty page when no reels match state`() {
        adapter.save(aReel(state = ReelState.NEW))

        val result = adapter.findAll(0, 20, ReelState.USED_UP)

        assertEquals(0, result.totalElements)
        assertEquals(0, result.content.size)
    }

    @Test
    fun `should sort reels by createdAt descending`() {
        adapter.save(aReel(createdAt = Instant.parse("2026-01-01T00:00:00Z")))
        adapter.save(aReel(createdAt = Instant.parse("2026-03-01T00:00:00Z")))
        adapter.save(aReel(createdAt = Instant.parse("2026-02-01T00:00:00Z")))

        val result = adapter.findAll(0, 20, null)

        assertEquals(
            listOf(
                Instant.parse("2026-03-01T00:00:00Z"),
                Instant.parse("2026-02-01T00:00:00Z"),
                Instant.parse("2026-01-01T00:00:00Z"),
            ),
            result.content.map { it.createdAt },
        )
    }

    @Test
    fun `should respect page and size boundaries`() {
        repeat(3) { adapter.save(aReel()) }

        val firstPage = adapter.findAll(0, 2, null)
        val secondPage = adapter.findAll(1, 2, null)

        assertEquals(3, firstPage.totalElements)
        assertEquals(2, firstPage.totalPages)
        assertEquals(2, firstPage.content.size)
        assertEquals(1, secondPage.content.size)
    }

    private fun aReel(
        state: ReelState = ReelState.NEW,
        createdAt: Instant = Instant.now(),
    ) = Reel(
        id = UUID.randomUUID().toString(),
        brand = "Luxilon",
        model = "ALU Power",
        material = Material.POLYESTER,
        gaugeHundredthsMm = 125,
        reelLengthMeters = 200,
        costCents = 12000,
        stringFeeCents = 2500,
        metersPerJob = 11,
        purchaseDate = LocalDate.of(2026, 1, 15),
        state = state,
        createdAt = createdAt,
    )
}
