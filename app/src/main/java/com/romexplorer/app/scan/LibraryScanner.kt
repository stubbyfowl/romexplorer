package com.romexplorer.app.scan

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import com.romexplorer.app.data.RomEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LibraryScanner(private val context: Context) {

    suspend fun scan(treeUri: Uri): List<RomEntry> = withContext(Dispatchers.IO) {
        val results = mutableListOf<RomEntry>()
        try {
            val rootDocId = DocumentsContract.getTreeDocumentId(treeUri)
            walk(treeUri, rootDocId, parentFolderName = null, results)
        } catch (e: Exception) {
            Log.e("LibraryScanner", "Scan failed", e)
        }
        results
    }

    private fun walk(
        treeUri: Uri,
        docId: String,
        parentFolderName: String?,
        out: MutableList<RomEntry>
    ) {
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, docId)
        val resolver = context.contentResolver

        try {
            resolver.query(
                childrenUri,
                arrayOf(
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    DocumentsContract.Document.COLUMN_MIME_TYPE,
                    DocumentsContract.Document.COLUMN_SIZE
                ),
                null, null, null
            )?.use { cursor ->
                val idCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
                val sizeCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)

                if (idCol < 0 || nameCol < 0) return@use

                while (cursor.moveToNext()) {
                    try {
                        val childId = cursor.getString(idCol) ?: continue
                        val name = cursor.getString(nameCol) ?: continue
                        val mime = if (mimeCol >= 0) cursor.getString(mimeCol) else null
                        val size = if (sizeCol >= 0) cursor.getLong(sizeCol) else 0L

                        if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                            walk(treeUri, childId, parentFolderName = name, out)
                            continue
                        }

                        val ext = name.substringAfterLast('.', "").lowercase()
                        if (ext !in SystemDetector.knownExtensions) continue

                        val childUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, childId)
                        val system = SystemDetector.detect(name, parentFolderName)
                        out += RomEntry(
                            uri = childUri.toString(),
                            fileName = name,
                            displayTitle = SystemDetector.cleanTitle(name),
                            system = system,
                            sizeBytes = size,
                            folderUri = treeUri.toString()
                        )
                    } catch (rowError: Exception) {
                        Log.e("LibraryScanner", "Skipping bad row", rowError)
                    }
                }
            }
        } catch (queryError: Exception) {
            Log.e("LibraryScanner", "Skipping folder $docId", queryError)
        }
    }
}