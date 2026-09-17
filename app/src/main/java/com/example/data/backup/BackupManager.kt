package com.example.data.backup

import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.room.withTransaction
import com.example.data.auth.AuthRepository
import com.example.data.local.AppDatabase
import com.example.data.local.CollectionEntryEntity
import com.example.data.local.CollectionSpotEntity
import com.example.data.local.GoalEntity
import com.example.data.local.RedemptionEntryEntity
import com.example.data.local.SyncState
import com.example.data.preferences.UserPreferencesRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.io.BufferedReader
import java.io.InputStreamReader
import java.security.MessageDigest
import java.time.Instant
import java.util.UUID

class BackupManager(
    private val context: Context,
    private val database: AppDatabase,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val backupPreferencesRepository: BackupPreferencesRepository,
    private val authRepository: AuthRepository,
    private val supabaseClient: SupabaseClient?
) {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = false
        encodeDefaults = true
    }

    /**
     * Creates an in-memory snapshot of all personal Vira data.
     * Excludes public return points.
     */
    suspend fun createSnapshotPayload(): ViraBackupPayload = withContext(Dispatchers.IO) {
        val spots = database.spotDao().getAllSpots().first()
        val collections = database.collectionDao().getAllCollections().first()
        val redemptions = database.redemptionDao().getAllRedemptions().first()
        val goals = database.goalDao().getAllGoals().first()
        val profileData = userPreferencesRepository.profileData.first()

        val spotIdToKeyMap = spots.associate { it.id to (it.remoteId ?: UUID.randomUUID().toString()) }

        val backupSpots = spots.map { spot ->
            ViraBackupSpot(
                clientKey = spotIdToKeyMap[spot.id] ?: UUID.randomUUID().toString(),
                name = spot.name,
                latitude = spot.latitude,
                longitude = spot.longitude,
                address = spot.address,
                createdAt = spot.createdAt,
                updatedAt = spot.updatedAt,
                lastVisitedAt = spot.lastVisitedAt,
                totalVisits = spot.totalVisits,
                lifetimeContainers = spot.lifetimeContainers,
                averageContainersPerVisit = spot.averageContainersPerVisit
            )
        }

        val backupCollections = collections.map { col ->
            ViraBackupCollection(
                clientKey = col.remoteId ?: UUID.randomUUID().toString(),
                spotClientKey = col.collectionSpotId?.let { spotIdToKeyMap[it] },
                containerCount = col.containerCount,
                timestamp = col.timestamp,
                estimatedValueCents = col.estimatedValueCents,
                note = col.note,
                latitude = col.latitude,
                longitude = col.longitude,
                createdAt = col.createdAt,
                updatedAt = col.updatedAt
            )
        }

        val backupRedemptions = redemptions.map { red ->
            ViraBackupRedemption(
                clientKey = red.remoteId ?: UUID.randomUUID().toString(),
                presentedContainers = red.presentedContainers,
                acceptedContainers = red.acceptedContainers,
                rejectedContainers = red.rejectedContainers,
                actualRecoveredCents = red.actualRecoveredCents,
                timestamp = red.timestamp,
                note = red.note,
                createdAt = red.createdAt,
                updatedAt = red.updatedAt
            )
        }

        val backupGoals = goals.map { goal ->
            ViraBackupGoal(
                type = goal.type,
                targetValue = goal.targetValue,
                period = goal.period,
                isActive = goal.isActive,
                createdAt = goal.createdAt
            )
        }

        val backupProfile = ViraBackupProfile(
            displayName = profileData.displayName,
            username = profileData.username,
            city = profileData.city
        )

        val totalContainers = collections.sumOf { it.containerCount }
        val nowIso = Instant.now().toString()

        val preliminaryMetadata = ViraBackupMetadata(
            formatVersion = 1,
            appVersion = "0.2.0-beta",
            createdAt = nowIso,
            deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}",
            collectionsCount = backupCollections.size,
            spotsCount = backupSpots.size,
            redemptionsCount = backupRedemptions.size,
            goalsCount = backupGoals.size,
            totalContainers = totalContainers,
            checksumSha256 = ""
        )

        val preliminaryPayload = ViraBackupPayload(
            metadata = preliminaryMetadata,
            spots = backupSpots,
            collections = backupCollections,
            redemptions = backupRedemptions,
            goals = backupGoals,
            profile = backupProfile
        )

        val rawDataString = json.encodeToString(preliminaryPayload)
        val checksum = computeSha256(rawDataString)

        preliminaryPayload.copy(
            metadata = preliminaryMetadata.copy(checksumSha256 = checksum)
        )
    }

    /**
     * Uploads snapshot to the user's private cloud backup storage in Supabase.
     * Enforces conservative retention policy: last 7 daily + last 4 weekly backups.
     */
    suspend fun uploadSnapshot(
        payload: ViraBackupPayload,
        backupType: String = "MANUAL"
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val client = supabaseClient 
                ?: return@withContext Result.failure(IllegalStateException("Serviço de cópia na nuvem indisponível"))
            val userId = authRepository.getCurrentUserId()
                ?: return@withContext Result.failure(IllegalStateException("Requer autenticação para backup na nuvem"))

            val payloadJsonString = json.encodeToString(payload)
            val sizeBytes = payloadJsonString.toByteArray(Charsets.UTF_8).size.toLong()

            val insertObj = buildJsonObject {
                put("user_id", userId)
                put("device_model", payload.metadata.deviceModel)
                put("collections_count", payload.metadata.collectionsCount)
                put("spots_count", payload.metadata.spotsCount)
                put("size_bytes", sizeBytes)
                put("backup_type", backupType)
                put("payload", Json.parseToJsonElement(payloadJsonString))
            }

            client.from("user_backups").insert(insertObj)

            pruneOldBackupsRetention(client)

            backupPreferencesRepository.recordBackupSuccess(System.currentTimeMillis())
            Result.success(Unit)
        } catch (e: Exception) {
            val err = e.message ?: "Falha ao enviar backup para a nuvem"
            backupPreferencesRepository.recordBackupFailure(err)
            Result.failure(e)
        }
    }

    /**
     * Prunes backups according to retention policy:
     * - Last 7 DAILY backups
     * - Last 4 WEEKLY backups
     * - Last 5 MANUAL backups
     */
    private suspend fun pruneOldBackupsRetention(client: SupabaseClient) {
        try {
            val allBackups = client.from("user_backups")
                .select {
                    order("created_at", Order.DESCENDING)
                }
                .decodeList<CloudBackupItem>()

            val toDeleteIds = mutableListOf<String>()

            val daily = allBackups.filter { it.backupType == "DAILY" }
            if (daily.size > 7) {
                toDeleteIds.addAll(daily.drop(7).map { it.id })
            }

            val weekly = allBackups.filter { it.backupType == "WEEKLY" }
            if (weekly.size > 4) {
                toDeleteIds.addAll(weekly.drop(4).map { it.id })
            }

            val manual = allBackups.filter { it.backupType == "MANUAL" }
            if (manual.size > 5) {
                toDeleteIds.addAll(manual.drop(5).map { it.id })
            }

            for (backupId in toDeleteIds) {
                client.from("user_backups").delete {
                    filter {
                        eq("id", backupId)
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("BackupManager", "Retention pruning encountered non-fatal error: ${e.message}")
        }
    }

    /**
     * Fetches the user's available cloud snapshots.
     */
    suspend fun fetchCloudBackups(): Result<List<CloudBackupItem>> = withContext(Dispatchers.IO) {
        try {
            val client = supabaseClient 
                ?: return@withContext Result.failure(IllegalStateException("Serviço de cópia na nuvem indisponível"))
            val userId = authRepository.getCurrentUserId()
                ?: return@withContext Result.failure(IllegalStateException("Requer autenticação"))

            val list = client.from("user_backups")
                .select {
                    order("created_at", Order.DESCENDING)
                }
                .decodeList<CloudBackupItem>()

            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Restores a snapshot with safe re-keying.
     * Prevents collisions with old accounts by generating fresh UUIDs and resetting sync state.
     */
    suspend fun restoreSnapshot(payload: ViraBackupPayload): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            database.withTransaction {
                val personalTables = listOf(
                    "collection_entry",
                    "collection_spot",
                    "redemption_entry",
                    "goal",
                    "sync_outbox"
                )
                for (table in personalTables) {
                    database.openHelper.writableDatabase.execSQL("DELETE FROM $table")
                }

                val spotKeyToNewId = mutableMapOf<String, Int>()
                for (bSpot in payload.spots) {
                    val spotEntity = CollectionSpotEntity(
                        id = 0,
                        name = bSpot.name,
                        latitude = bSpot.latitude,
                        longitude = bSpot.longitude,
                        address = bSpot.address,
                        createdAt = bSpot.createdAt,
                        updatedAt = bSpot.updatedAt,
                        lastVisitedAt = bSpot.lastVisitedAt,
                        totalVisits = bSpot.totalVisits,
                        lifetimeContainers = bSpot.lifetimeContainers,
                        averageContainersPerVisit = bSpot.averageContainersPerVisit,
                        remoteId = UUID.randomUUID().toString(), // SAFELY RE-KEYED
                        deletedAt = null,
                        syncState = SyncState.LOCAL_ONLY.name,
                        serverUpdatedAt = null,
                        remoteVersion = 0L
                    )
                    val newId = database.spotDao().insertSpot(spotEntity).toInt()
                    spotKeyToNewId[bSpot.clientKey] = newId
                }

                for (bCol in payload.collections) {
                    val mappedSpotId = bCol.spotClientKey?.let { spotKeyToNewId[it] }
                    val colEntity = CollectionEntryEntity(
                        id = 0,
                        containerCount = bCol.containerCount,
                        timestamp = bCol.timestamp,
                        estimatedValueCents = bCol.estimatedValueCents,
                        collectionSpotId = mappedSpotId,
                        note = bCol.note,
                        latitude = bCol.latitude,
                        longitude = bCol.longitude,
                        createdAt = bCol.createdAt,
                        updatedAt = bCol.updatedAt,
                        remoteId = UUID.randomUUID().toString(), // SAFELY RE-KEYED
                        deletedAt = null,
                        syncState = SyncState.LOCAL_ONLY.name,
                        serverUpdatedAt = null,
                        remoteVersion = 0L
                    )
                    database.collectionDao().insertCollection(colEntity)
                }

                for (bRed in payload.redemptions) {
                    val redEntity = RedemptionEntryEntity(
                        id = 0,
                        presentedContainers = bRed.presentedContainers,
                        acceptedContainers = bRed.acceptedContainers,
                        rejectedContainers = bRed.rejectedContainers,
                        actualRecoveredCents = bRed.actualRecoveredCents,
                        timestamp = bRed.timestamp,
                        note = bRed.note,
                        createdAt = bRed.createdAt,
                        updatedAt = bRed.updatedAt,
                        remoteId = UUID.randomUUID().toString(), // SAFELY RE-KEYED
                        deletedAt = null,
                        syncState = SyncState.LOCAL_ONLY.name,
                        serverUpdatedAt = null,
                        remoteVersion = 0L
                    )
                    database.redemptionDao().insertRedemption(redEntity)
                }

                for (bGoal in payload.goals) {
                    val goalEntity = GoalEntity(
                        id = 0,
                        type = bGoal.type,
                        targetValue = bGoal.targetValue,
                        period = bGoal.period,
                        isActive = bGoal.isActive,
                        createdAt = bGoal.createdAt,
                        updatedAt = bGoal.createdAt,
                        remoteId = UUID.randomUUID().toString(), // SAFELY RE-KEYED
                        deletedAt = null,
                        syncState = SyncState.LOCAL_ONLY.name,
                        serverUpdatedAt = null,
                        remoteVersion = 0L
                    )
                    database.goalDao().insertGoal(goalEntity)
                }
            }

            payload.profile?.let { prof ->
                userPreferencesRepository.saveProfile(
                    displayName = prof.displayName,
                    username = prof.username,
                    city = prof.city
                )
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Exports snapshot to a user-selected file URI using Storage Access Framework.
     */
    suspend fun exportToUri(uri: Uri, payload: ViraBackupPayload): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val jsonString = json.encodeToString(payload)
            context.contentResolver.openOutputStream(uri)?.use { os ->
                os.write(jsonString.toByteArray(Charsets.UTF_8))
                os.flush()
            } ?: return@withContext Result.failure(IllegalStateException("Não foi possível aceder ao ficheiro de destino"))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Reads and validates a portable backup file from URI.
     */
    suspend fun importFromUri(uri: Uri): Result<ViraBackupPayload> = withContext(Dispatchers.IO) {
        try {
            val stringBuilder = StringBuilder()
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { reader ->
                    var line: String? = reader.readLine()
                    while (line != null) {
                        stringBuilder.append(line)
                        line = reader.readLine()
                    }
                }
            } ?: return@withContext Result.failure(IllegalStateException("Não foi possível ler o ficheiro"))

            val rawJson = stringBuilder.toString()
            val payload = json.decodeFromString<ViraBackupPayload>(rawJson)

            if (payload.metadata.formatVersion > 1) {
                return@withContext Result.failure(IllegalStateException("Versão de cópia não suportada por esta versão da Vira"))
            }

            Result.success(payload)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun computeSha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
