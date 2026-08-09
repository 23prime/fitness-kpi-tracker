package com.okkey.fitnesskpitracker.data

import java.time.LocalDate
import java.time.ZoneId

fun latestWeightPerDate(
    samples: List<WeightSample>,
    zoneId: ZoneId = ZoneId.systemDefault(),
): Map<LocalDate, Double> =
    samples
        .groupBy { it.instant.atZone(zoneId).toLocalDate() }
        .mapValues { (_, samplesForDate) -> samplesForDate.maxBy { it.instant }.weightKg }
