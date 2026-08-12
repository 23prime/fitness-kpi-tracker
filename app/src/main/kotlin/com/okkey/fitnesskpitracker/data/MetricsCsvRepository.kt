package com.okkey.fitnesskpitracker.data

sealed interface CsvImportResult {
    data object Success : CsvImportResult

    data class Failure(
        val reason: MetricsCsvParseResult.Failure,
    ) : CsvImportResult
}

class MetricsCsvRepository(
    private val dao: DailyMetricsCsvDao,
) {
    suspend fun exportCsv(): String = MetricsCsv.format(dao.findAll())

    suspend fun importCsv(csv: String): CsvImportResult =
        when (val result = MetricsCsv.parse(csv)) {
            is MetricsCsvParseResult.Success -> {
                dao.replaceAll(result.entities)
                CsvImportResult.Success
            }

            is MetricsCsvParseResult.Failure -> {
                CsvImportResult.Failure(result)
            }
        }
}
