package com.stringpro.application.domain.service.reel

import com.stringpro.application.domain.model.reel.Reel
import com.stringpro.application.domain.model.reel.ReelNotFoundException
import com.stringpro.application.domain.model.reel.ReelState
import com.stringpro.application.ports.`in`.reel.ChangeReelStateCommand
import com.stringpro.application.ports.`in`.reel.ChangeReelStateUseCase
import com.stringpro.application.ports.`in`.reel.CreateReelCommand
import com.stringpro.application.ports.`in`.reel.CreateReelUseCase
import com.stringpro.application.ports.`in`.reel.DeleteReelUseCase
import com.stringpro.application.ports.`in`.reel.GetReelUseCase
import com.stringpro.application.ports.`in`.reel.ListReelsQuery
import com.stringpro.application.ports.`in`.reel.ListReelsUseCase
import com.stringpro.application.ports.`in`.reel.UpdateReelCommand
import com.stringpro.application.ports.`in`.reel.UpdateReelUseCase
import com.stringpro.application.ports.out.reel.ReelRepository
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

@Service
class ReelService(
    private val reelRepository: ReelRepository,
) : CreateReelUseCase, GetReelUseCase, ListReelsUseCase, UpdateReelUseCase, ChangeReelStateUseCase, DeleteReelUseCase {
    override fun create(command: CreateReelCommand): Reel =
        reelRepository.save(
            Reel(
                id = UUID.randomUUID().toString(),
                brand = command.brand,
                model = command.model,
                material = command.material,
                gaugeHundredthsMm = command.gaugeHundredthsMm,
                reelLengthMeters = command.reelLengthMeters,
                costCents = command.costCents,
                stringFeeCents = command.stringFeeCents,
                metersPerJob = command.metersPerJob,
                purchaseDate = command.purchaseDate,
                state = ReelState.NEW,
                createdAt = Instant.now(),
            ),
        )

    override fun get(id: String): Reel = reelRepository.findById(id) ?: throw ReelNotFoundException(id)

    override fun list(query: ListReelsQuery): List<Reel> =
        if (query.state == null) {
            reelRepository.findAll()
        } else {
            reelRepository.findByState(query.state)
        }

    override fun update(
        id: String,
        command: UpdateReelCommand,
    ): Reel {
        val existing = reelRepository.findById(id) ?: throw ReelNotFoundException(id)
        return reelRepository.save(
            existing.copy(
                brand = command.brand,
                model = command.model,
                material = command.material,
                gaugeHundredthsMm = command.gaugeHundredthsMm,
                reelLengthMeters = command.reelLengthMeters,
                costCents = command.costCents,
                stringFeeCents = command.stringFeeCents,
                metersPerJob = command.metersPerJob,
                purchaseDate = command.purchaseDate,
            ),
        )
    }

    override fun changeState(
        id: String,
        command: ChangeReelStateCommand,
    ): Reel {
        val existing = reelRepository.findById(id) ?: throw ReelNotFoundException(id)
        return reelRepository.save(existing.copy(state = command.targetState))
    }

    override fun delete(id: String) {
        val existing = reelRepository.findById(id) ?: throw ReelNotFoundException(id)
        reelRepository.save(existing.copy(deletedAt = Instant.now()))
    }
}
