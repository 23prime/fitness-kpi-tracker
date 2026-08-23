package com.okkey.fitnesskpitracker.domain

import kotlin.math.ceil

fun remainingSteps(remainingScore: Double): Long = ceil(remainingScore.coerceAtLeast(0.0) / STEPS_COEFFICIENT).toLong()
