package com.romexplorer.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "romexplorer_settings")

private val KEY_RA_USERNAME = stringPreferencesKey("ra_username")
private val KEY_RA_API_KEY = stringPreferencesKey("ra_api_key")
private val KEY_ROM_FOLDER_URI = stringPreferencesKey("rom_folder_uri")

class Settings(private val context: Context) {

    val raUsername: Flow<String?> = context.dataStore.data.map { it[KEY_RA_USERNAME] }
    val raApiKey: Flow<String?> = context.dataStore.data.map { it[KEY_RA_API_KEY] }
    val romFolderUri: Flow<String?> = context.dataStore.data.map { it[KEY_ROM_FOLDER_URI] }

    suspend fun setRaCredentials(username: String, apiKey: String) {
        context.dataStore.edit {
            it[KEY_RA_USERNAME] = username
            it[KEY_RA_API_KEY] = apiKey
        }
    }

    suspend fun setRomFolder(uri: String) {
        context.dataStore.edit { it[KEY_ROM_FOLDER_URI] = uri }
    }
}
