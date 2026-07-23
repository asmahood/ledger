package io.github.asmahood.ledger.data.csv

import android.net.Uri

/**
 * Controllable fake for [CsvFileReader] used in ViewModel unit tests.
 *
 * Unit tests never construct a real [Uri] — they drive the ViewModel through
 * `onFileLoaded` instead — so [read] exists only to satisfy the interface.
 */
class FakeCsvFileReader : CsvFileReader {
    var file: CsvFile = CsvFile("empty.csv", CsvTable(emptyList(), emptyList()))
    var readError: Throwable? = null

    override suspend fun read(uri: Uri): CsvFile {
        readError?.let { throw it }
        return file
    }
}
