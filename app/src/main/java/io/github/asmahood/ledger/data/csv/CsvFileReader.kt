package io.github.asmahood.ledger.data.csv

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileNotFoundException
import javax.inject.Inject

data class CsvFile(val name: String, val table: CsvTable)

interface CsvFileReader {
    suspend fun read(uri: Uri): CsvFile
}

class ContentResolverCsvFileReader @Inject constructor(
    @ApplicationContext private val context: Context,
) : CsvFileReader {

    override suspend fun read(uri: Uri): CsvFile = withContext(Dispatchers.IO) {
        val stream = context.contentResolver.openInputStream(uri)
            ?: throw FileNotFoundException("Unable to open input stream for $uri")
        val text = stream.use { input -> input.reader(Charsets.UTF_8).readText() }
        CsvFile(name = resolveName(uri), table = CsvTable.parse(text))
    }

    private fun resolveName(uri: Uri): String {
        val displayName = context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0 && cursor.moveToFirst()) cursor.getString(nameIndex) else null
        }
        return displayName ?: uri.lastPathSegment ?: ""
    }
}
