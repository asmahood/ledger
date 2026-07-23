package io.github.asmahood.ledger.data.csv

import com.github.doyaaaaaken.kotlincsv.dsl.context.ExcessFieldsRowBehaviour
import com.github.doyaaaaaken.kotlincsv.dsl.context.InsufficientFieldsRowBehaviour
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader

data class CsvTable(val headers: List<String>, val rows: List<List<String>>) {
    companion object {
        private const val BYTE_ORDER_MARK = "\uFEFF"

        fun parse(text: String): CsvTable {
            val cleaned = text.removePrefix(BYTE_ORDER_MARK)
            if (cleaned.isBlank()) return CsvTable(emptyList(), emptyList())

            val lines = csvReader {
                skipEmptyLine = true
                insufficientFieldsRowBehaviour = InsufficientFieldsRowBehaviour.EMPTY_STRING
                excessFieldsRowBehaviour = ExcessFieldsRowBehaviour.TRIM
            }.readAll(cleaned)

            if (lines.isEmpty()) return CsvTable(emptyList(), emptyList())

            val headers = lines.first()
            val rows = lines.drop(1)
            return CsvTable(headers, rows)
        }
    }
}
