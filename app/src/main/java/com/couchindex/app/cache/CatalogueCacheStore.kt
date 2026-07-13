package com.couchindex.app.cache

import android.content.Context
import android.util.AtomicFile

class CatalogueCacheStore(context: Context) {
    private val file = AtomicFile(context.filesDir.resolve(FILE_NAME))

    fun load(): CatalogueSnapshot? =
        runCatching {
            file.openRead().bufferedReader().use { CatalogueSnapshotCodec.decode(it.readText()) }
        }.getOrNull()

    fun save(snapshot: CatalogueSnapshot) {
        val output = file.startWrite()
        try {
            output.write(CatalogueSnapshotCodec.encode(snapshot).toByteArray(Charsets.UTF_8))
            output.flush()
            file.finishWrite(output)
        } catch (error: Exception) {
            file.failWrite(output)
            throw error
        }
    }

    companion object {
        private const val FILE_NAME = "catalogue-snapshot-v1.json"
    }
}
