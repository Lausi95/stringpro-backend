package com.stringpro.application.domain.service.racket

import com.stringpro.application.domain.model.customer.CustomerNotFoundException
import com.stringpro.application.domain.model.racket.Racket
import com.stringpro.application.domain.model.racket.RacketNotFoundException
import com.stringpro.application.ports.`in`.racket.CreateRacketCommand
import com.stringpro.application.ports.`in`.racket.CreateRacketUseCase
import com.stringpro.application.ports.`in`.racket.DeleteRacketUseCase
import com.stringpro.application.ports.`in`.racket.GetRacketUseCase
import com.stringpro.application.ports.`in`.racket.ListRacketsQuery
import com.stringpro.application.ports.`in`.racket.ListRacketsUseCase
import com.stringpro.application.ports.`in`.racket.UpdateRacketCommand
import com.stringpro.application.ports.`in`.racket.UpdateRacketUseCase
import com.stringpro.application.ports.out.customer.CustomerRepository
import com.stringpro.application.ports.out.racket.RacketRepository
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

@Service
class RacketService(
    private val racketRepository: RacketRepository,
    private val customerRepository: CustomerRepository,
) : CreateRacketUseCase, GetRacketUseCase, ListRacketsUseCase, UpdateRacketUseCase, DeleteRacketUseCase {

    override fun create(command: CreateRacketCommand): Racket {
        customerRepository.findById(command.customerId) ?: throw CustomerNotFoundException(command.customerId)
        return racketRepository.save(
            Racket(
                id = UUID.randomUUID().toString(),
                customerId = command.customerId,
                brand = command.brand,
                model = command.model,
                headSize = command.headSize,
                stringMains = command.stringMains,
                stringCrosses = command.stringCrosses,
                notes = command.notes,
                createdAt = Instant.now(),
            ),
        )
    }

    override fun get(id: String): Racket =
        racketRepository.findById(id) ?: throw RacketNotFoundException(id)

    override fun list(query: ListRacketsQuery): List<Racket> =
        racketRepository.findByCustomerId(query.customerId)

    override fun update(id: String, command: UpdateRacketCommand): Racket {
        val existing = racketRepository.findById(id) ?: throw RacketNotFoundException(id)
        return racketRepository.save(
            existing.copy(
                brand = command.brand,
                model = command.model,
                headSize = command.headSize,
                stringMains = command.stringMains,
                stringCrosses = command.stringCrosses,
                notes = command.notes,
            ),
        )
    }

    override fun delete(id: String) {
        val existing = racketRepository.findById(id) ?: throw RacketNotFoundException(id)
        racketRepository.save(existing.copy(deletedAt = Instant.now()))
    }
}
