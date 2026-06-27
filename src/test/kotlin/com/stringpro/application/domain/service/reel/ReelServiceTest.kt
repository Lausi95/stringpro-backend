package com.stringpro.application.domain.service.reel

import com.stringpro.application.domain.model.reel.Material
import com.stringpro.application.domain.model.reel.Reel
import com.stringpro.application.domain.model.reel.ReelNotFoundException
import com.stringpro.application.domain.model.reel.ReelState
import com.stringpro.application.ports.`in`.reel.ChangeReelStateCommand
import com.stringpro.application.ports.`in`.reel.CreateReelCommand
import com.stringpro.application.ports.`in`.reel.ListReelsQuery
import com.stringpro.application.ports.`in`.reel.UpdateReelCommand
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

class ReelServiceTest {

    private val reelRepository: ReelRepository = mockk()
    private val service = ReelService(reelRepository)

    @Test
    fun `should create reel in NEW state`() {
        val slot = slot<Reel>()
        every { reelRepository.save(capture(slot)) } answers { slot.captured }

        val result = service.create(
            CreateReelCommand(
                brand = "Luxilon",
                model = "ALU Power",
                material = Material.POLYESTER,
                gaugeHundredthsMm = 125,
                reelLengthMeters = 200,
                costCents = 12000,
                stringFeeCents = 2500,
                metersPerJob = 11,
                purchaseDate = LocalDate.of(2026, 1, 15),
            ),
        )

        assertNotNull(result.id)
        assertEquals("Luxilon", result.brand)
        assertEquals(Material.POLYESTER, result.material)
        assertEquals(125, result.gaugeHundredthsMm)
        assertEquals(12000, result.costCents)
        assertEquals(2500, result.stringFeeCents)
        assertEquals(ReelState.NEW, result.state)
        assertNull(result.deletedAt)
    }

    @Test
    fun `should get reel by id`() {
        every { reelRepository.findById("id-1") } returns aReel(id = "id-1")

        val result = service.get("id-1")

        assertEquals("id-1", result.id)
    }

    @Test
    fun `should throw ReelNotFoundException when reel does not exist`() {
        every { reelRepository.findById("unknown") } returns null

        assertThrows<ReelNotFoundException> { service.get("unknown") }
    }

    @Test
    fun `should list all reels when no state filter`() {
        every { reelRepository.findAll() } returns listOf(aReel(), aReel(id = "id-2"))

        val result = service.list(ListReelsQuery(state = null))

        assertEquals(2, result.size)
        verify(exactly = 0) { reelRepository.findByState(any()) }
    }

    @Test
    fun `should list reels filtered by state`() {
        every { reelRepository.findByState(ReelState.IN_USE) } returns listOf(aReel(state = ReelState.IN_USE))

        val result = service.list(ListReelsQuery(state = ReelState.IN_USE))

        assertEquals(1, result.size)
        verify(exactly = 0) { reelRepository.findAll() }
    }

    @Test
    fun `should update reel attributes`() {
        val slot = slot<Reel>()
        every { reelRepository.findById("id-1") } returns aReel(id = "id-1")
        every { reelRepository.save(capture(slot)) } answers { slot.captured }

        val result = service.update(
            "id-1",
            UpdateReelCommand(
                brand = "Babolat",
                model = "RPM Blast",
                material = Material.POLYESTER,
                gaugeHundredthsMm = 130,
                reelLengthMeters = 100,
                costCents = 9000,
                stringFeeCents = 3000,
                metersPerJob = 12,
                purchaseDate = LocalDate.of(2026, 2, 1),
            ),
        )

        assertEquals("Babolat", result.brand)
        assertEquals("RPM Blast", result.model)
        assertEquals(130, result.gaugeHundredthsMm)
        assertEquals(9000, result.costCents)
    }

    @Test
    fun `should not change state on update`() {
        val slot = slot<Reel>()
        every { reelRepository.findById("id-1") } returns aReel(id = "id-1", state = ReelState.IN_USE)
        every { reelRepository.save(capture(slot)) } answers { slot.captured }

        val result = service.update(
            "id-1",
            UpdateReelCommand(
                brand = "Babolat",
                model = "RPM Blast",
                material = Material.POLYESTER,
                gaugeHundredthsMm = 130,
                reelLengthMeters = 100,
                costCents = 9000,
                stringFeeCents = 3000,
                metersPerJob = 12,
                purchaseDate = LocalDate.of(2026, 2, 1),
            ),
        )

        assertEquals(ReelState.IN_USE, result.state)
    }

    @Test
    fun `should throw ReelNotFoundException when updating non-existent reel`() {
        every { reelRepository.findById("unknown") } returns null

        assertThrows<ReelNotFoundException> {
            service.update(
                "unknown",
                UpdateReelCommand(
                    brand = "Babolat",
                    model = "RPM Blast",
                    material = Material.POLYESTER,
                    gaugeHundredthsMm = 130,
                    reelLengthMeters = 100,
                    costCents = 9000,
                    stringFeeCents = 3000,
                    metersPerJob = 12,
                    purchaseDate = LocalDate.of(2026, 2, 1),
                ),
            )
        }
    }

    @Test
    fun `should change reel state in any direction`() {
        val slot = slot<Reel>()
        every { reelRepository.findById("id-1") } returns aReel(id = "id-1", state = ReelState.USED_UP)
        every { reelRepository.save(capture(slot)) } answers { slot.captured }

        val result = service.changeState("id-1", ChangeReelStateCommand(ReelState.IN_USE))

        assertEquals(ReelState.IN_USE, result.state)
    }

    @Test
    fun `should throw ReelNotFoundException when changing state of non-existent reel`() {
        every { reelRepository.findById("unknown") } returns null

        assertThrows<ReelNotFoundException> {
            service.changeState("unknown", ChangeReelStateCommand(ReelState.USED_UP))
        }
    }

    @Test
    fun `should soft delete reel`() {
        val slot = slot<Reel>()
        every { reelRepository.findById("id-1") } returns aReel(id = "id-1")
        every { reelRepository.save(capture(slot)) } answers { slot.captured }

        service.delete("id-1")

        assertNotNull(slot.captured.deletedAt)
    }

    @Test
    fun `should throw ReelNotFoundException when deleting non-existent reel`() {
        every { reelRepository.findById("unknown") } returns null

        assertThrows<ReelNotFoundException> { service.delete("unknown") }
    }

    private fun aReel(
        id: String = "id-1",
        state: ReelState = ReelState.NEW,
    ) = Reel(
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
        state = state,
        createdAt = Instant.now(),
    )
}
