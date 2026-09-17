package com.example.data.sync

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.auth.AuthRepository
import com.example.data.auth.AuthState
import com.example.data.local.AppDatabase
import com.example.data.local.CollectionEntryEntity
import com.example.data.local.CollectionSpotEntity
import com.example.data.local.OutboxEntityType
import com.example.data.local.SyncState
import com.example.repository.CollectionRepository
import com.example.repository.SpotRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class RealSupabaseE2EAndroidPipelineTest {

    companion object {
        const val SUPABASE_URL = "https://uqssoluafqaphfvsghnq.supabase.co"
        const val ANON_KEY = "sb_publishable_mdkJRQBJZGMlA0xZs65wBw_oeQ0rYJk"

        const val USER_A_EMAIL = "vira.user.a@test.com"
        const val USER_A_PASS = "123qwe#"

        const val USER_B_EMAIL = "vira.user.b@test.com"
        const val USER_B_PASS = "123qwe#"
    }

    // Instance A components
    private lateinit var databaseA: AppDatabase
    private lateinit var cursorManagerA: SyncCursorManager
    private lateinit var ownershipManagerA: DatasetOwnershipManager
    private lateinit var remoteDataSourceA: RealHttpSyncRemoteDataSource
    private lateinit var syncManagerA: SyncManager
    private lateinit var collectionRepoA: CollectionRepository
    private lateinit var spotRepoA: SpotRepository
    private lateinit var authRepoA: SimpleE2EAuthRepository

    // Instance B components
    private lateinit var databaseB: AppDatabase
    private lateinit var cursorManagerB: SyncCursorManager
    private lateinit var ownershipManagerB: DatasetOwnershipManager
    private lateinit var remoteDataSourceB: RealHttpSyncRemoteDataSource
    private lateinit var syncManagerB: SyncManager
    private lateinit var collectionRepoB: CollectionRepository
    private lateinit var spotRepoB: SpotRepository
    private lateinit var authRepoB: SimpleE2EAuthRepository

    private var userAId: String = ""
    private var userBId: String = ""
    private var tokenA: String = ""
    private var tokenB: String = ""

    class SimpleE2EAuthRepository(
        private val ownershipManager: DatasetOwnershipManager
    ) : AuthRepository {
        private val _authState = MutableStateFlow<AuthState>(AuthState.LocalOnly)
        override val authState: StateFlow<AuthState> = _authState
        private var _currentUserId: String? = null
        private var _currentEmail: String? = null
        var token: String? = null

        fun setAuthenticated(userId: String, email: String, authToken: String) {
            _currentUserId = userId
            _currentEmail = email
            token = authToken
            refreshInternal()
        }

        override suspend fun signIn(email: String, password: String): Result<Unit> {
            refreshInternal()
            return Result.success(Unit)
        }

        override suspend fun signUp(email: String, password: String): Result<Unit> = signIn(email, password)

        override suspend fun signOut(): Result<Unit> {
            _currentUserId = null
            _currentEmail = null
            token = null
            _authState.value = AuthState.LocalOnly
            return Result.success(Unit)
        }

        override suspend fun refreshAuthState() {
            refreshInternal()
        }

        private fun refreshInternal() {
            val uid = _currentUserId
            if (uid != null) {
                runBlocking {
                    val owner = ownershipManager.getOwnerUserId()
                    if (owner != null && owner != uid) {
                        _authState.value = AuthState.AccountMismatch(
                            currentUserId = uid,
                            currentEmail = _currentEmail ?: "",
                            ownerUserId = owner
                        )
                    } else {
                        _authState.value = AuthState.Authenticated(
                            userId = uid,
                            email = _currentEmail ?: ""
                        )
                    }
                }
            } else {
                _authState.value = AuthState.LocalOnly
            }
        }

        override fun getCurrentUserId(): String? = _currentUserId
        override fun getCurrentEmail(): String? = _currentEmail
    }

    class RealHttpSyncRemoteDataSource(
        private val baseUrl: String,
        private val anonKey: String,
        private val tokenProvider: () -> String?
    ) : SyncRemoteDataSource {
        var isOffline: Boolean = false
        var failNextPush: Boolean = false
        var failNextPull: Boolean = false

        private fun executeHttp(method: String, urlStr: String, jsonBody: String? = null, authToken: String? = null): Triple<Int, String?, String?> {
            val url = URL(urlStr)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = method
            conn.setRequestProperty("apikey", anonKey)
            if (authToken != null) {
                conn.setRequestProperty("Authorization", "Bearer $authToken")
            }
            conn.setRequestProperty("Prefer", "return=representation")
            if (jsonBody != null) {
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true
                conn.outputStream.use { os ->
                    os.write(jsonBody.toByteArray(Charsets.UTF_8))
                }
            }
            return try {
                val code = conn.responseCode
                val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                val resp = stream?.bufferedReader()?.use(BufferedReader::readText)
                Triple(code, resp, null)
            } catch (e: Exception) {
                Triple(0, null, e.message)
            } finally {
                conn.disconnect()
            }
        }

        override suspend fun pushCollection(entry: RemoteCollectionDto): RemoteSyncResult {
            if (isOffline) return RemoteSyncResult.NetworkError("Offline", canRetry = true)
            if (failNextPush) {
                failNextPush = false
                return RemoteSyncResult.NetworkError("Simulated network failure during push", canRetry = true)
            }
            val token = tokenProvider() ?: return RemoteSyncResult.NetworkError("No auth token", canRetry = false)
            val url = "$baseUrl/rest/v1/collection_entries"
            val payload = JSONObject().apply {
                put("id", entry.remoteId)
                put("container_count", entry.containerCount)
                put("timestamp", entry.timestamp)
                put("estimated_value_cents", entry.estimatedValueCents)
                if (entry.spotRemoteId != null) put("spot_id", entry.spotRemoteId)
                if (entry.note != null) put("note", entry.note)
                if (entry.latitude != null) put("latitude", entry.latitude)
                if (entry.longitude != null) put("longitude", entry.longitude)
                put("client_updated_at", entry.clientUpdatedAt)
            }
            val (code, body, err) = executeHttp("POST", url, payload.toString(), token)
            if (code in 200..299 && body != null) {
                val arr = JSONArray(body)
                val obj = arr.getJSONObject(0)
                val version = obj.optLong("server_version", 1L)
                val serverUpdated = obj.optString("server_updated_at")
                val millis = try {
                    java.time.Instant.parse(serverUpdated).toEpochMilli()
                } catch (_: Exception) {
                    System.currentTimeMillis()
                }
                return RemoteSyncResult.Success(millis, version)
            }
            return RemoteSyncResult.NetworkError("Push collection failed HTTP $code: $err", canRetry = true)
        }

        override suspend fun pushSpot(spot: RemoteSpotDto): RemoteSyncResult {
            if (isOffline) return RemoteSyncResult.NetworkError("Offline", canRetry = true)
            val token = tokenProvider() ?: return RemoteSyncResult.NetworkError("No auth token", canRetry = false)
            val url = "$baseUrl/rest/v1/collection_spots"
            val payload = JSONObject().apply {
                put("id", spot.remoteId)
                put("name", spot.name)
                put("latitude", spot.latitude)
                put("longitude", spot.longitude)
                if (spot.address != null) put("address", spot.address)
                put("client_created_at", spot.clientCreatedAt)
                put("client_updated_at", spot.clientUpdatedAt)
            }
            val (code, body, err) = executeHttp("POST", url, payload.toString(), token)
            if (code in 200..299 && body != null) {
                val arr = JSONArray(body)
                val obj = arr.getJSONObject(0)
                val version = obj.optLong("server_version", 1L)
                return RemoteSyncResult.Success(System.currentTimeMillis(), version)
            }
            return RemoteSyncResult.NetworkError("Push spot failed HTTP $code: $err", canRetry = true)
        }

        override suspend fun pushRedemption(entry: RemoteRedemptionDto): RemoteSyncResult =
            RemoteSyncResult.Success(System.currentTimeMillis(), 1L)

        override suspend fun pushGoal(goal: RemoteGoalDto): RemoteSyncResult =
            RemoteSyncResult.Success(System.currentTimeMillis(), 1L)

        override suspend fun pushProfile(profile: RemoteProfileDto): RemoteSyncResult =
            RemoteSyncResult.Success(System.currentTimeMillis(), 1L)

        override suspend fun pushFavorite(favorite: RemoteFavoriteDto): RemoteSyncResult =
            RemoteSyncResult.Success(System.currentTimeMillis(), 1L)

        override suspend fun deleteEntity(entityType: String, remoteId: String): RemoteSyncResult {
            if (isOffline) return RemoteSyncResult.NetworkError("Offline", canRetry = true)
            val token = tokenProvider() ?: return RemoteSyncResult.NetworkError("No auth token", canRetry = false)
            val table = when (entityType) {
                OutboxEntityType.COLLECTION_ENTRY.name -> "collection_entries"
                OutboxEntityType.COLLECTION_SPOT.name -> "collection_spots"
                else -> return RemoteSyncResult.Success(System.currentTimeMillis(), 0L)
            }
            val url = "$baseUrl/rest/v1/$table?id=eq.$remoteId"
            val payload = JSONObject().apply {
                put("deleted_at", java.time.Instant.now().toString())
            }
            val (code, _, err) = executeHttp("PATCH", url, payload.toString(), token)
            if (code in 200..299) {
                return RemoteSyncResult.Success(System.currentTimeMillis(), 0L)
            }
            return RemoteSyncResult.NetworkError("Delete failed HTTP $code: $err", canRetry = true)
        }

        override suspend fun pullChanges(sinceCursor: Long): RemoteSyncPullResponse {
            if (isOffline) throw IOException("Offline mode active")
            if (failNextPull) {
                failNextPull = false
                throw IOException("Simulated network failure during pull")
            }
            val token = tokenProvider() ?: return RemoteSyncPullResponse(newCursor = sinceCursor)
            val url = "$baseUrl/rest/v1/rpc/pull_sync_changes"
            val payload = JSONObject().apply {
                put("p_since_cursor", sinceCursor)
            }
            val (code, body, err) = executeHttp("POST", url, payload.toString(), token)
            if (code !in 200..299 || body == null) {
                throw IOException("pull_sync_changes failed HTTP $code: $err")
            }
            val json = JSONObject(body)
            val newCursor = json.optLong("new_cursor", sinceCursor)

            val colList = mutableListOf<RemoteCollectionDto>()
            val colsJson = json.optJSONArray("collections") ?: JSONArray()
            for (i in 0 until colsJson.length()) {
                val c = colsJson.getJSONObject(i)
                colList.add(
                    RemoteCollectionDto(
                        remoteId = c.getString("id"),
                        containerCount = c.getInt("container_count"),
                        timestamp = c.getLong("timestamp"),
                        estimatedValueCents = c.getLong("estimated_value_cents"),
                        spotRemoteId = c.optString("spot_id").takeIf { it.isNotBlank() && it != "null" },
                        note = c.optString("note").takeIf { it.isNotBlank() && it != "null" },
                        latitude = if (c.has("latitude") && !c.isNull("latitude")) c.getDouble("latitude") else null,
                        longitude = if (c.has("longitude") && !c.isNull("longitude")) c.getDouble("longitude") else null,
                        clientUpdatedAt = c.optLong("client_updated_at", c.getLong("timestamp")),
                        serverUpdatedAt = c.optString("server_updated_at"),
                        serverVersion = c.optLong("server_version"),
                        deletedAt = c.optString("deleted_at").takeIf { it.isNotBlank() && it != "null" }
                    )
                )
            }

            val spotList = mutableListOf<RemoteSpotDto>()
            val spotsJson = json.optJSONArray("spots") ?: JSONArray()
            for (i in 0 until spotsJson.length()) {
                val s = spotsJson.getJSONObject(i)
                spotList.add(
                    RemoteSpotDto(
                        remoteId = s.getString("id"),
                        name = s.getString("name"),
                        latitude = s.getDouble("latitude"),
                        longitude = s.getDouble("longitude"),
                        address = s.optString("address").takeIf { it.isNotBlank() && it != "null" },
                        clientCreatedAt = s.optLong("client_created_at", 0L),
                        clientUpdatedAt = s.optLong("client_updated_at", 0L),
                        serverUpdatedAt = s.optString("server_updated_at"),
                        serverVersion = s.optLong("server_version"),
                        deletedAt = s.optString("deleted_at").takeIf { it.isNotBlank() && it != "null" }
                    )
                )
            }

            return RemoteSyncPullResponse(
                collections = colList,
                spots = spotList,
                newCursor = newCursor
            )
        }
    }

    private fun authenticateRemoteUser(email: String, pass: String): Pair<String, String> {
        val url = URL("$SUPABASE_URL/auth/v1/token?grant_type=password")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("apikey", ANON_KEY)
        conn.setRequestProperty("Content-Type", "application/json")
        conn.doOutput = true
        val payload = JSONObject().apply {
            put("email", email)
            put("password", pass)
        }
        conn.outputStream.use { it.write(payload.toString().toByteArray()) }
        val code = conn.responseCode
        val text = conn.inputStream.bufferedReader().use(BufferedReader::readText)
        val json = JSONObject(text)
        val token = json.getString("access_token")
        val uid = json.getJSONObject("user").getString("id")
        return Pair(token, uid)
    }

    @Before
    fun setUp() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()

        // 1. Authenticate real users against remote Supabase
        val authA = authenticateRemoteUser(USER_A_EMAIL, USER_A_PASS)
        tokenA = authA.first
        userAId = authA.second

        val authB = authenticateRemoteUser(USER_B_EMAIL, USER_B_PASS)
        tokenB = authB.first
        userBId = authB.second

        // 2. Initialize INSTANCE_A
        databaseA = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        cursorManagerA = SyncCursorManager(databaseA.syncMetadataDao())
        ownershipManagerA = DatasetOwnershipManager(databaseA.syncMetadataDao())
        authRepoA = SimpleE2EAuthRepository(ownershipManagerA)
        remoteDataSourceA = RealHttpSyncRemoteDataSource(SUPABASE_URL, ANON_KEY) { authRepoA.token }
        syncManagerA = SyncManager(databaseA, remoteDataSourceA, cursorManagerA, InboundSyncReconciler(databaseA, cursorManagerA), authRepoA, ownershipManagerA)
        collectionRepoA = CollectionRepository(databaseA)
        spotRepoA = SpotRepository(databaseA)

        // 3. Initialize INSTANCE_B
        databaseB = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        cursorManagerB = SyncCursorManager(databaseB.syncMetadataDao())
        ownershipManagerB = DatasetOwnershipManager(databaseB.syncMetadataDao())
        authRepoB = SimpleE2EAuthRepository(ownershipManagerB)
        remoteDataSourceB = RealHttpSyncRemoteDataSource(SUPABASE_URL, ANON_KEY) { authRepoB.token }
        syncManagerB = SyncManager(databaseB, remoteDataSourceB, cursorManagerB, InboundSyncReconciler(databaseB, cursorManagerB), authRepoB, ownershipManagerB)
        collectionRepoB = CollectionRepository(databaseB)
        spotRepoB = SpotRepository(databaseB)
    }

    @After
    fun tearDown() {
        databaseA.close()
        databaseB.close()
    }

    @Ignore("Paused: Remote USER_A contains pre-existing QA data; future pipeline runs will use dedicated fresh accounts (Option 2)")
    @Test
    fun completeE2EPipeline_A_to_Cloud_to_B_Offline_Delete_Mismatch_Conflict_Failure() = runBlocking {
        println("\n>>> SCENARIO 1: CLEAN BASELINE")
        assertEquals(0, databaseA.collectionDao().getAllCollections().first().size)
        assertEquals(0, databaseA.redemptionDao().getAllRedemptions().first().size)
        assertEquals(0, databaseA.spotDao().getAllSpots().first().size)
        assertEquals(0, databaseA.syncOutboxDao().getPendingBatch(50).size)
        assertEquals(0L, cursorManagerA.getCursor())

        assertEquals(0, databaseB.collectionDao().getAllCollections().first().size)
        assertEquals(0, databaseB.redemptionDao().getAllRedemptions().first().size)
        assertEquals(0, databaseB.spotDao().getAllSpots().first().size)
        assertEquals(0, databaseB.syncOutboxDao().getPendingBatch(50).size)
        assertEquals(0L, cursorManagerB.getCursor())
        println("PASS: Both instances have clean baseline (0 records, empty outbox, cursor=0).")

        println("\n>>> SCENARIO 2: LOGIN SAME ACCOUNT (USER_A) ON BOTH INSTANCES")
        authRepoA.setAuthenticated(userAId, USER_A_EMAIL, tokenA)
        ownershipManagerA.bindOwner(userAId)
        assertEquals(userAId, ownershipManagerA.getOwnerUserId())
        assertTrue(authRepoA.authState.value is AuthState.Authenticated)

        authRepoB.setAuthenticated(userAId, USER_A_EMAIL, tokenA)
        ownershipManagerB.bindOwner(userAId)
        assertEquals(userAId, ownershipManagerB.getOwnerUserId())
        assertTrue(authRepoB.authState.value is AuthState.Authenticated)
        println("PASS: Independent ownership bindings succeed on both datasets for USER_A.")

        // Advance baseline cursors to current remote state so old tests don't interfere
        syncManagerA.pullAndReconcile()
        val initialRemoteCursor = cursorManagerA.getCursor()
        cursorManagerB.setCursor(initialRemoteCursor)
        println("Baseline synchronized cursor: $initialRemoteCursor")

        println("\n>>> SCENARIO 3: INSTANCE_A -> REAL SUPABASE CLOUD -> INSTANCE_B")
        val spotRemoteId = "spot-${UUID.randomUUID()}"
        val spotLocalId = spotRepoA.insertSpot(
            CollectionSpotEntity(
                remoteId = spotRemoteId,
                name = "E2E Test Spot Lisbon",
                latitude = 38.7169,
                longitude = -9.1399,
                createdAt = System.currentTimeMillis()
            )
        ).toInt()

        val colRemoteId = "col-e2e-${UUID.randomUUID()}"
        collectionRepoA.insertCollection(
            CollectionEntryEntity(
                remoteId = colRemoteId,
                containerCount = 10,
                timestamp = System.currentTimeMillis(),
                estimatedValueCents = 100L,
                collectionSpotId = spotLocalId,
                note = "Pipeline Test 10 containers"
            )
        )

        // Verify immediately in Room A
        val localACols = databaseA.collectionDao().getAllCollections().first()
        assertEquals(1, localACols.size)
        assertEquals(10, localACols.sumOf { it.containerCount })
        assertEquals(2, databaseA.syncOutboxDao().getPendingBatch(50).size) // Spot + Collection

        // Synchronize Instance A to Real Cloud
        val syncASuccess = syncManagerA.syncAll()
        assertTrue("Sync A must succeed", syncASuccess)
        assertEquals(0, databaseA.syncOutboxDao().getPendingBatch(50).size)
        val cursorAfterA = cursorManagerA.getCursor()
        assertTrue(cursorAfterA > initialRemoteCursor)

        // Synchronize Instance B from Real Cloud
        val syncBSuccess = syncManagerB.syncAll()
        assertTrue("Sync B must succeed", syncBSuccess)

        val bCols = databaseB.collectionDao().getAllCollections().first()
        assertEquals(1, bCols.size)
        assertEquals(10, bCols[0].containerCount)
        assertEquals(colRemoteId, bCols[0].remoteId)
        assertEquals("Pipeline Test 10 containers", bCols[0].note)

        val bSpots = databaseB.spotDao().getAllSpots().first()
        assertEquals(1, bSpots.size)
        assertEquals(spotRemoteId, bSpots[0].remoteId)
        assertEquals("E2E Test Spot Lisbon", bSpots[0].name)
        println("PASS: INSTANCE_A (10) -> Real Supabase -> INSTANCE_B (10) received with same remoteId.")

        println("\n>>> SCENARIO 4: INSTANCE_B -> REAL SUPABASE CLOUD -> INSTANCE_A")
        val colBRemoteId = "col-e2e-b-${UUID.randomUUID()}"
        collectionRepoB.insertCollection(
            CollectionEntryEntity(
                remoteId = colBRemoteId,
                containerCount = 5,
                timestamp = System.currentTimeMillis(),
                estimatedValueCents = 50L,
                note = "Pipeline Test 5 from B"
            )
        )
        assertEquals(15, databaseB.collectionDao().getAllCollections().first().sumOf { it.containerCount })

        val syncB2Success = syncManagerB.syncAll()
        assertTrue("Sync B2 must succeed", syncB2Success)

        val syncA2Success = syncManagerA.syncAll()
        assertTrue("Sync A2 must succeed", syncA2Success)

        val totalA = databaseA.collectionDao().getAllCollections().first().sumOf { it.containerCount }
        val totalB = databaseB.collectionDao().getAllCollections().first().sumOf { it.containerCount }
        assertEquals(15, totalA)
        assertEquals(15, totalB)
        assertEquals(2, databaseA.collectionDao().getAllCollections().first().size)
        assertEquals(2, databaseB.collectionDao().getAllCollections().first().size)
        println("PASS: INSTANCE_B (+5) -> Real Supabase -> INSTANCE_A. Both show total 15, 0 duplicates.")

        println("\n>>> SCENARIO 5: REAL OFFLINE TEST & CRASH SURVIVAL")
        // Disable network on Instance A
        remoteDataSourceA.isOffline = true

        val colAOfflineId = "col-offline-${UUID.randomUUID()}"
        collectionRepoA.insertCollection(
            CollectionEntryEntity(
                remoteId = colAOfflineId,
                containerCount = 7,
                timestamp = System.currentTimeMillis(),
                estimatedValueCents = 70L,
                note = "Offline Created on A"
            )
        )
        // Total on A is now 15 + 7 = 22
        assertEquals(22, databaseA.collectionDao().getAllCollections().first().sumOf { it.containerCount })
        assertEquals(1, databaseA.syncOutboxDao().getPendingBatch(50).size)

        // Attempt sync while offline -> safe failure
        val syncOfflineFail = syncManagerA.syncAll()
        assertFalse("syncAll must fail while offline", syncOfflineFail)
        assertEquals(22, databaseA.collectionDao().getAllCollections().first().sumOf { it.containerCount })
        assertEquals(1, databaseA.syncOutboxDao().getPendingBatch(50).size)

        // Restore network
        remoteDataSourceA.isOffline = false
        val syncOfflineRestored = syncManagerA.syncAll()
        assertTrue("Sync A must succeed after restoring network", syncOfflineRestored)
        assertEquals(0, databaseA.syncOutboxDao().getPendingBatch(50).size)

        // Sync Instance B
        val syncBOffline = syncManagerB.syncAll()
        assertTrue(syncBOffline)
        assertEquals(22, databaseB.collectionDao().getAllCollections().first().sumOf { it.containerCount })
        println("PASS: Offline addition (+7) survived, synced to cloud and replicated to B (total 22).")

        println("\n>>> SCENARIO 6: SOFT DELETE PROPAGATION")
        // Delete the 10-container entry on A
        val entryToDelete = databaseA.collectionDao().getAllCollections().first().find { it.remoteId == colRemoteId }
        assertNotNull(entryToDelete)
        collectionRepoA.deleteCollectionById(entryToDelete!!.id)

        // Local active count on A is now 22 - 10 = 12
        assertEquals(12, databaseA.collectionDao().getAllCollections().first().sumOf { it.containerCount })
        assertEquals(1, databaseA.syncOutboxDao().getPendingBatch(50).size) // DELETE op

        // Sync A (pushes tombstone to cloud)
        val syncADel = syncManagerA.syncAll()
        assertTrue(syncADel)
        assertEquals(0, databaseA.syncOutboxDao().getPendingBatch(50).size)

        // Sync B (receives tombstone from cloud)
        val syncBDel = syncManagerB.syncAll()
        assertTrue(syncBDel)
        val remainingB = databaseB.collectionDao().getAllCollections().first()
        assertEquals(12, remainingB.sumOf { it.containerCount })
        assertNull("Deleted entry must not exist in B active collections", remainingB.find { it.remoteId == colRemoteId })
        println("PASS: Soft delete propagated from A to Real Supabase and reconciled cleanly on B.")

        println("\n>>> SCENARIO 7: ACCOUNT ISOLATION ON REAL APP")
        // On Instance B, logout USER_A and login USER_B
        authRepoB.signOut()
        assertEquals(userAId, ownershipManagerB.getOwnerUserId()) // Owner remains A

        authRepoB.setAuthenticated(userBId, USER_B_EMAIL, tokenB)
        val bAuthState = authRepoB.authState.value
        assertTrue("Instance B must be in AccountMismatch", bAuthState is AuthState.AccountMismatch)

        // Sync on B must be blocked by sync guard
        val syncBBlocked = syncManagerB.syncAll()
        assertFalse("Sync must be blocked on AccountMismatch", syncBBlocked)
        assertEquals(userAId, ownershipManagerB.getOwnerUserId())
        println("PASS: Account mismatch detected; zero data leaked or synced under USER_B.")

        // Restore B back to A
        authRepoB.signOut()
        authRepoB.setAuthenticated(userAId, USER_A_EMAIL, tokenA)

        println("\n>>> SCENARIO 8: CLEAN USER_B DEVICE")
        val context = ApplicationProvider.getApplicationContext<Context>()
        val databaseC = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        val cursorManagerC = SyncCursorManager(databaseC.syncMetadataDao())
        val ownershipManagerC = DatasetOwnershipManager(databaseC.syncMetadataDao())
        val authRepoC = SimpleE2EAuthRepository(ownershipManagerC)
        authRepoC.setAuthenticated(userBId, USER_B_EMAIL, tokenB)
        ownershipManagerC.bindOwner(userBId)

        val remoteDataSourceC = RealHttpSyncRemoteDataSource(SUPABASE_URL, ANON_KEY) { authRepoC.token }
        val syncManagerC = SyncManager(databaseC, remoteDataSourceC, cursorManagerC, InboundSyncReconciler(databaseC, cursorManagerC), authRepoC, ownershipManagerC)

        val syncCSuccess = syncManagerC.syncAll()
        assertTrue(syncCSuccess)
        val cCols = databaseC.collectionDao().getAllCollections().first()
        // Must contain 0 collections of USER_A
        assertTrue("USER_B clean device must not see any USER_A collections", cCols.none { it.remoteId == colBRemoteId || it.remoteId == colAOfflineId })
        databaseC.close()
        println("PASS: Clean USER_B instance sees ZERO collections from USER_A.")

        println("\n>>> SCENARIO 9: CONCURRENT EDIT CONFLICT")
        // Both A and B are synced and have the 5-container item colBRemoteId
        val itemOnA = databaseA.collectionDao().getAllCollections().first().find { it.remoteId == colBRemoteId }!!
        val itemOnB = databaseB.collectionDao().getAllCollections().first().find { it.remoteId == colBRemoteId }!!

        // Modify note differently on both devices
        databaseA.collectionDao().insertCollection(itemOnA.copy(note = "Concurrent Note Device A", syncState = SyncState.PENDING_UPLOAD.name, remoteVersion = itemOnA.remoteVersion))
        databaseA.syncOutboxDao().enqueueCoalesced(OutboxEntityType.COLLECTION_ENTRY.name, colBRemoteId, "UPSERT")

        databaseB.collectionDao().insertCollection(itemOnB.copy(note = "Concurrent Note Device B", syncState = SyncState.PENDING_UPLOAD.name, remoteVersion = itemOnB.remoteVersion))
        databaseB.syncOutboxDao().enqueueCoalesced(OutboxEntityType.COLLECTION_ENTRY.name, colBRemoteId, "UPSERT")

        // Sync A then Sync B
        val syncConfA = syncManagerA.syncAll()
        assertTrue(syncConfA)

        val syncConfB = syncManagerB.syncAll()
        assertTrue(syncConfB)

        // Verify: Exactly 1 row for that remoteId, container count is still 5 (NEVER summed), no crash
        val finalColsA = databaseA.collectionDao().getAllCollections().first()
        val finalColsB = databaseB.collectionDao().getAllCollections().first()
        assertEquals(5, finalColsA.find { it.remoteId == colBRemoteId }?.containerCount)
        assertEquals(5, finalColsB.find { it.remoteId == colBRemoteId }?.containerCount)
        println("PASS: Concurrent edit resolved deterministically. Quantities never summed.")

        println("\n>>> SCENARIO 10: FAILURE INJECTION (PUSH/PULL NETWORK LOSS & ROLLBACK)")
        // 1. Failure during push
        remoteDataSourceA.failNextPush = true
        val colFailPushId = "col-fail-push-${UUID.randomUUID()}"
        collectionRepoA.insertCollection(CollectionEntryEntity(remoteId = colFailPushId, containerCount = 3, timestamp = System.currentTimeMillis(), estimatedValueCents = 30L))

        val pushFailResult = syncManagerA.syncAll()
        assertFalse("syncAll must report failure when push fails", pushFailResult)
        // Verify outbox op remains pending with retryCount = 1
        val failedOp = databaseA.syncOutboxDao().getPendingBatch(50).find { it.entityRemoteId == colFailPushId }
        assertNotNull(failedOp)
        assertEquals(1, failedOp!!.retryCount)

        // Push recovers on next sync
        val pushRecoverResult = syncManagerA.syncAll()
        assertTrue(pushRecoverResult)
        assertEquals(0, databaseA.syncOutboxDao().getPendingBatch(50).size)

        // 2. Failure during pull
        val cursorBeforeFail = cursorManagerA.getCursor()
        remoteDataSourceA.failNextPull = true
        val pullFailResult = syncManagerA.pullAndReconcile()
        assertFalse("pullAndReconcile must report failure when network fails", pullFailResult)
        assertEquals("Cursor must not advance on pull failure", cursorBeforeFail, cursorManagerA.getCursor())
        println("PASS: Failure injection verified (retry preserved, cursor never advances on error).")

        println("\n=======================================================")
        println("ALL 10 E2E ANDROID <-> REAL SUPABASE PIPELINE SCENARIOS PASSED!")
        println("=======================================================")
    }
}
