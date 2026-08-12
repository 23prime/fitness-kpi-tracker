package com.okkey.fitnesskpitracker.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed interface CsvImportResult {
    data object Success : CsvImportResult

    data class Failure(
        val reason: MetricsCsvParseResult.Failure,
    ) : CsvImportResult
}

class MetricsCsvRepository(
    private val dao: DailyMetricsCsvDao,
) {
    suspend fun exportCsv(): String {
        val entities = dao.findAll()
        return withContext(Dispatchers.Default) { MetricsCsv.format(entities) }
    }

    suspend fun importCsv(csv: String): CsvImportResult {
        val result = withContext(Dispatchers.Default) { MetricsCsv.parse(csv) }
        return when (result) {
            is MetricsCsvParseResult.Success -> {
                dao.replaceAll(result.entities)
                CsvImportResult.Success
            }

            is MetricsCsvParseResult.Failure -> {
                CsvImportResult.Failure(result)
            }
        }
    }
}
