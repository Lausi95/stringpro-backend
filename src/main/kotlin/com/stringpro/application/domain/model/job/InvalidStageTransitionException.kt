package com.stringpro.application.domain.model.job

class InvalidStageTransitionException(
    from: Stage,
    to: Stage,
) : RuntimeException("Cannot move Stage backward from $from to $to")
