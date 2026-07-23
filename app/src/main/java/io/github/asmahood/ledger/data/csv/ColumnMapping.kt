package io.github.asmahood.ledger.data.csv

data class ColumnMapping(val columns: Map<ImportField, Int>) {
    operator fun get(field: ImportField): Int? = columns[field]

    fun with(field: ImportField, columnIndex: Int?): ColumnMapping {
        return if (columnIndex == null) {
            ColumnMapping(columns - field)
        } else {
            ColumnMapping(columns + (field to columnIndex))
        }
    }

    val isComplete: Boolean
        get() = ImportField.entries.filter { it.required }.all { columns.containsKey(it) }

    companion object {
        val EMPTY = ColumnMapping(emptyMap())

        fun autoDetect(headers: List<String>): ColumnMapping {
            val columns = mutableMapOf<ImportField, Int>()
            val claimedIndices = mutableSetOf<Int>()

            for (field in ImportField.entries) {
                for ((index, header) in headers.withIndex()) {
                    if (index !in claimedIndices && normalize(header) in field.synonyms) {
                        columns[field] = index
                        claimedIndices.add(index)
                        break
                    }
                }
            }

            return ColumnMapping(columns)
        }

        fun normalize(header: String): String {
            return header.trim().lowercase().filter { it.isLetterOrDigit() }
        }
    }
}
