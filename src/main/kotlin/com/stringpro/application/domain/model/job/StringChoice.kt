package com.stringpro.application.domain.model.job

/**
 * The string used on one side (mains or crosses) of a Job. Either the Customer's Own String
 * (identified only by a name, never charged) or a Reel from inventory (carrying a snapshotted
 * String Fee).
 */
sealed interface StringChoice {
    val sourceType: StringSourceType
    val feeCents: Long

    data class Own(
        val stringName: String,
    ) : StringChoice {
        init {
            require(stringName.isNotBlank()) { "Own string requires a name" }
        }

        override val sourceType: StringSourceType get() = StringSourceType.OWN
        override val feeCents: Long get() = 0
    }

    data class Reel(
        val reelId: String,
        val stringFeeCents: Long,
    ) : StringChoice {
        init {
            require(stringFeeCents >= 0) { "String fee cannot be negative" }
        }

        override val sourceType: StringSourceType get() = StringSourceType.REEL
        override val feeCents: Long get() = stringFeeCents
    }
}
