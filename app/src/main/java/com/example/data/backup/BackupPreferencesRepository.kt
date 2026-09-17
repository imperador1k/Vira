package com.example.data.backup

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.backupPrefsDataStore: DataStore<Preferences> by preferencesDataStore(name = "vira_backup_preferences")

enum class BackupFrequency {
    DAILY,
    WEEKLY
}

enum class BackupNetworkConstraint {
    WIFI_ONLY,
    ANY_NETWORK
}

data class BackupPreferences(
    val isAutoBackupEnabled: Boolean = false,
    val frequency: BackupFrequency = BackupFrequency.DAILY,
    val networkConstraint: BackupNetworkConstraint = BackupNetworkConstraint.WIFI_ONLY,
    val lastBackupTimestamp: Long = 0L,
    val lastBackupStatus: String = "Nenhum backup realizado"
)

class BackupPreferencesRepository(private val context: Context) {

    private val keyAutoBackupEnabled = booleanPreferencesKey("auto_backup_enabled")
    private val keyFrequency = stringPreferencesKey("backup_frequency")
    private val keyNetworkConstraint = stringPreferencesKey("backup_network_constraint")
    private val keyLastBackupTimestamp = longPreferencesKey("last_backup_timestamp")
    private val keyLastBackupStatus = stringPreferencesKey("last_backup_status")

    val preferences: Flow<BackupPreferences> = context.backupPrefsDataStore.data.map { prefs ->
        val freqStr = prefs[keyFrequency] ?: BackupFrequency.DAILY.name
        val freq = try { BackupFrequency.valueOf(freqStr) } catch (_: Exception) { BackupFrequency.DAILY }

        val netStr = prefs[keyNetworkConstraint] ?: BackupNetworkConstraint.WIFI_ONLY.name
        val net = try { BackupNetworkConstraint.valueOf(netStr) } catch (_: Exception) { BackupNetworkConstraint.WIFI_ONLY }

        BackupPreferences(
            isAutoBackupEnabled = prefs[keyAutoBackupEnabled] ?: false,
            frequency = freq,
            networkConstraint = net,
            lastBackupTimestamp = prefs[keyLastBackupTimestamp] ?: 0L,
            lastBackupStatus = prefs[keyLastBackupStatus] ?: "Nenhum backup realizado"
        )
    }

    suspend fun setAutoBackupEnabled(enabled: Boolean) {
        context.backupPrefsDataStore.edit { prefs ->
            prefs[keyAutoBackupEnabled] = enabled
        }
    }

    suspend fun setFrequency(frequency: BackupFrequency) {
        context.backupPrefsDataStore.edit { prefs ->
            prefs[keyFrequency] = frequency.name
        }
    }

    suspend fun setNetworkConstraint(networkConstraint: BackupNetworkConstraint) {
        context.backupPrefsDataStore.edit { prefs ->
            prefs[keyNetworkConstraint] = networkConstraint.name
        }
    }

    suspend fun recordBackupSuccess(timestamp: Long, status: String = "Cópia concluída") {
        context.backupPrefsDataStore.edit { prefs ->
            prefs[keyLastBackupTimestamp] = timestamp
            prefs[keyLastBackupStatus] = status
        }
    }

    suspend fun recordBackupFailure(status: String) {
        context.backupPrefsDataStore.edit { prefs ->
            prefs[keyLastBackupStatus] = status
        }
    }
}
