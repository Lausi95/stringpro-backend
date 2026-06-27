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

        val result = adapter.findAll()

        assertEquals(1, result.size)
    }

    @Test
    fun `should find reels by state excluding soft-deleted`() {
        adapter.save(aReel(state = ReelState.IN_USE))
        adapter.save(aReel(state = ReelState.IN_USE))
        adapter.save(aReel(state = ReelState.NEW))
        val toDelete = adapter.save(aReel(state = ReelState.IN_USE))
        adapter.save(toDelete.copy(deletedAt = Instant.now()))

        val result = adapter.findByState(ReelState.IN_USE)

        assertEquals(2, result.size)
        assertTrue(result.all { it.state == ReelState.IN_USE })
    }

    @Test
    fun `should return empty list when no reels match state`() {
        adapter.save(aReel(state = ReelState.NEW))

        val result = adapter.findByState(ReelState.USED_UP)

        assertEquals(0, result.size)
    }

    private fun aReel(state: ReelState = ReelState.NEW) = Reel(
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
        createdAt = Instant.now(),
    )
}
