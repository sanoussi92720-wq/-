package com.example

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.example.data.engine.FileRepository
import com.example.data.engine.ImageProcessorEngine
import com.example.data.engine.SafDocumentHelper
import com.example.data.local.AppDatabase
import com.example.data.local.SettingsDataStore

class FileMasterApp : Application(), ImageLoaderFactory {

    lateinit var database: AppDatabase
        private set
    lateinit var settingsDataStore: SettingsDataStore
        private set
    lateinit var safHelper: SafDocumentHelper
        private set
    lateinit var imageEngine: ImageProcessorEngine
        private set
    lateinit var fileRepository: FileRepository
        private set

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getInstance(this)
        settingsDataStore = SettingsDataStore(this)
        safHelper = SafDocumentHelper(this)
        imageEngine = ImageProcessorEngine(this)
        fileRepository = FileRepository(
            context = this,
            safHelper = safHelper,
            imageEngine = imageEngine,
            recentEntryDao = database.recentEntryDao(),
            batchHistoryDao = database.batchHistoryDao(),
            settingsDataStore = settingsDataStore
        )
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizePercent(0.05)
                    .build()
            }
            .crossfade(true)
            .build()
    }
}
