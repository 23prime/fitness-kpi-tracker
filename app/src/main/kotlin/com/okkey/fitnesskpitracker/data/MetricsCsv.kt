package com.okkey.fitnesskpitracker.data

import java.time.LocalDate
import java.time.format.DateTimeParseException

sealed interface MetricsCsvParseResult {
    data class Success(
        val entities: List<DailyMetricsEntity>,
    ) : MetricsCsvParseResult

    sealed interface Failure : MetricsCsvParseResult {
        data object HeaderMismatch : Failure

        data class InvalidDate(
            val lineNumber: Int,
        ) : Failure

        data class DuplicateDate(
            val lineNumber: Int,
        ) : Failure

        data class InvalidNumber(
            val lineNumber: Int,
        ) : Failure
    }
}

private class CsvRowParseException(
    val failure: MetricsCsvParseResult.Failure,
) : Exception()

object MetricsCsv {
    const val HEADER =
        "date,steps_health_connect,steps_manual," +
            "cycling_distance_km_health_connect,cycling_distance_km_manual," +
            "weight_kg_health_connect,weight_kg_manual,workout_sets"

    private val columns = HEADER.split(",")
    private val lineEnding = Regex("\r\n|\n")

    fun format(entities: List<DailyMetricsEntity>): String {
        val rows = listOf(HEADER) + entities.map(::formatRow)
        return rows.joinToString("\n")
    }

    private fun formatRow(entity: DailyMetricsEntity): String =
        listOf(
            entity.date.toString(),
            entity.stepsHealthConnect?.toString().orEmpty(),
            entity.stepsManual?.toString().orEmpty(),
            entity.cyclingDistanceKmHealthConnect?.toString().orEmpty(),
            entity.cyclingDistanceKmManual?.toString().orEmpty(),
            entity.weightKgHealthConnect?.toString().orEmpty(),
            entity.weightKgManual?.toString().orEmpty(),
            entity.workoutSets?.toString().orEmpty(),
        ).joinToString(",")

    fun parse(csv: String): MetricsCsvParseResult {
        val lines = csv.split(lineEnding)
        val headerColumns = lines.first().split(",")
        if (headerColumns.size != columns.size || headerColumns.toSet() != columns.toSet()) {
            return MetricsCsvParseResult.Failure.HeaderMismatch
        }
        val columnIndex = columns.associateWith { headerColumns.indexOf(it) }
        val rows =
            lines.drop(1).mapIndexed { index, line -> (index + 2) to line }.filter { (_, line) -> line.isNotEmpty() }
        val seenDates = mutableSetOf<LocalDate>()
        return try {
            val entities =
                rows.map { (lineNumber, line) -> parseRow(line, columnIndex, lineNumber, seenDates) }
            MetricsCsvParseResult.Success(entities)
        } catch (e: CsvRowParseException) {
            e.failure
        }
    }

    private fun parseRow(
        line: String,
        columnIndex: Map<String, Int>,
        lineNumber: Int,
        seenDates: MutableSet<LocalDate>,
    ): DailyMetricsEntity {
        val cells = line.split(",")

        fun cell(column: String): String =
            cells.getOrNull(columnIndex.getValue(column))
                ?: throw CsvRowParseException(MetricsCsvParseResult.Failure.InvalidNumber(lineNumber))

        fun <T> parseNumber(
            column: String,
            parse: (String) -> T?,
        ): T? =
            cell(column).takeIf { it.isNotEmpty() }?.let {
                parse(it) ?: throw CsvRowParseException(MetricsCsvParseResult.Failure.InvalidNumber(lineNumber))
            }

        fun parseLong(column: String) = parseNumber(column, String::toLongOrNull)

        fun parseInt(column: String) = parseNumber(column, String::toIntOrNull)

        fun parseDouble(column: String) = parseNumber(column) { it.toDoubleOrNull()?.takeIf(Double::isFinite) }

        val date =
            try {
                LocalDate.parse(cell("date"))
            } catch (e: DateTimeParseException) {
                throw CsvRowParseException(MetricsCsvParseResult.Failure.InvalidDate(lineNumber))
            }
        if (!seenDates.add(date)) {
            throw CsvRowParseException(MetricsCsvParseResult.Failure.DuplicateDate(lineNumber))
        }

        return DailyMetricsEntity(
            date = date,
            stepsHealthConnect = parseLong("steps_health_connect"),
            stepsManual = parseLong("steps_manual"),
            cyclingDistanceKmHealthConnect = parseDouble("cycling_distance_km_health_connect"),
            cyclingDistanceKmManual = parseDouble("cycling_distance_km_manual"),
            weightKgHealthConnect = parseDouble("weight_kg_health_connect"),
            weightKgManual = parseDouble("weight_kg_manual"),
            workoutSets = parseInt("workout_sets"),
        )
    }
}
