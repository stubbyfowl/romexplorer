package com.romexplorer.app

import android.app.Application
import com.romexplorer.app.data.AppDatabase
import com.romexplorer.app.data.Settings

class RomExplorerApp : Application() {
    val database by lazy { AppDatabase.get(this) }
    val settings by lazy { Settings(this) }
}
