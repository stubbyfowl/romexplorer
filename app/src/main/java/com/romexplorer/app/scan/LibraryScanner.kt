package com.romexplorer.app.scan

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import com.romexplorer.app.data.RomEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Walks a user-chosen Storage Access Framework tree (so this works fine on
 * scoped-storage devices without needing MANAGE_EXTERNAL_STORAGE) and turns
 * every recognized ROM file into a RomEntry ready to insert into Room.
 */
class LibraryScanner(private val context: Context) {

    suspend fun scan(treeUri: Uri): List<RomEntry> = withContext(Dispatchers.IO) {
        val results = mutableListOf<RomEntry>()
        val rootDocId = DocumentsContract.getTreeDocumentId(treeUri)
        walk(treeUri, rootDocId, parentFolderName = null, results)
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
            val idCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val nameCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            val mimeCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
            val sizeCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_SIZE)

            while (cursor.moveToNext()) {
                val childId = cursor.getString(idCol)
                val name = cursor.getString(nameCol) ?: continue
                val mime = cursor.getString(mimeCol)
                val size = cursor.getLong(sizeCol)

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
            }
        }
    }
}
