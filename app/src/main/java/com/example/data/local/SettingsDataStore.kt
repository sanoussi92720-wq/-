package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.SortDirection
import com.example.model.SortField
import com.example.model.ViewMode
import com.example.ui.theme.ThemePreference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "filemaster_prefs")

data class UserSettings(
    val themePreference: ThemePreference = ThemePreference.DARK,
    val dynamicColor: Boolean = false,
    val defaultViewMode: ViewMode = ViewMode.LIST,
    val defaultSortField: SortField = SortField.NAME,
    val defaultSortDirection: SortDirection = SortDirection.ASCENDING,
    val preserveOriginalsByDefault: Boolean = true,
    val confirmBeforeOverwrite: Boolean = true,
    val autoOrientExif: Boolean = true,
    val safTreeUris: Set<String> = emptySet()
)

class SettingsDataStore(private val context: Context) {

    private object Keys {
        val THEME = stringPreferencesKey("theme_preference")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val VIEW_MODE = stringPreferencesKey("default_view_mode")
        val SORT_FIELD = stringPreferencesKey("default_sort_field")
        val SORT_DIRECTION = stringPreferencesKey("default_sort_direction")
        val PRESERVE_ORIGINALS = booleanPreferencesKey("preserve_originals")
        val CONFIRM_OVERWRITE = booleanPreferencesKey("confirm_overwrite")
        val AUTO_ORIENT_EXIF = booleanPreferencesKey("auto_orient_exif")
        val SAF_TREE_URIS = stringSetPreferencesKey("saf_tree_uris")
    }

    val settingsFlow: Flow<UserSettings> = context.dataStore.data.map { prefs ->
        UserSettings(
            themePreference = prefs[Keys.THEME]?.let { runCatching { ThemePreference.valueOf(it) }.getOrNull() }
                ?: ThemePreference.DARK,
            dynamicColor = prefs[Keys.DYNAMIC_COLOR] ?: false,
            defaultViewMode = prefs[Keys.VIEW_MODE]?.let { runCatching { ViewMode.valueOf(it) }.getOrNull() }
                ?: ViewMode.LIST,
            defaultSortField = prefs[Keys.SORT_FIELD]?.let { runCatching { SortField.valueOf(it) }.getOrNull() }
                ?: SortField.NAME,
            defaultSortDirection = prefs[Keys.SORT_DIRECTION]?.let { runCatching { SortDirection.valueOf(it) }.getOrNull() }
                ?: SortDirection.ASCENDING,
            preserveOriginalsByDefault = prefs[Keys.PRESERVE_ORIGINALS] ?: true,
            confirmBeforeOverwrite = prefs[Keys.CONFIRM_OVERWRITE] ?: true,
            autoOrientExif = prefs[Keys.AUTO_ORIENT_EXIF] ?: true,
            safTreeUris = prefs[Keys.SAF_TREE_URIS] ?: emptySet()
        )
    }

    suspend fun setThemePreference(theme: ThemePreference) {
        context.dataStore.edit { it[Keys.THEME] = theme.name }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DYNAMIC_COLOR] = enabled }
    }

    suspend fun setDefaultViewMode(mode: ViewMode) {
        context.dataStore.edit { it[Keys.VIEW_MODE] = mode.name }
    }

    suspend fun setDefaultSort(field: SortField, direction: SortDirection) {
        context.dataStore.edit {
            it[Keys.SORT_FIELD] = field.name
            it[Keys.SORT_DIRECTION] = direction.name
        }
    }

    suspend fun setPreserveOriginals(preserve: Boolean) {
        context.dataStore.edit { it[Keys.PRESERVE_ORIGINALS] = preserve }
    }

    suspend fun setConfirmBeforeOverwrite(confirm: Boolean) {
        context.dataStore.edit { it[Keys.CONFIRM_OVERWRITE] = confirm }
    }

    suspend fun setAutoOrientExif(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_ORIENT_EXIF] = enabled }
    }

    suspend fun addSafTreeUri(uriString: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.SAF_TREE_URIS]?.toMutableSet() ?: mutableSetOf()
            current.add(uriString)
            prefs[Keys.SAF_TREE_URIS] = current
        }
    }

    suspend fun removeSafTreeUri(uriString: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.SAF_TREE_URIS]?.toMutableSet() ?: mutableSetOf()
            current.remove(uriString)
            prefs[Keys.SAF_TREE_URIS] = current
        }
    }
}
