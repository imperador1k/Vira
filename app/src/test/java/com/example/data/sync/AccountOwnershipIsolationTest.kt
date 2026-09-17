package com.example.data.sync

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.auth.AuthRepository
import com.example.data.auth.AuthState
import com.example.data.local.AppDatabase
import com.example.data.local.CollectionEntryEntity
import com.example.data.local.OutboxEntityType
import com.example.data.local.OutboxOperationType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class AccountOwnershipIsolationTest {

    private lateinit var database: AppDatabase
    private lateinit var cursorManager: SyncCursorManager
    private lateinit var ownershipManager: DatasetOwnershipManager
    private lateinit var syncManager: SyncManager
    private lateinit var fakeRemote: FakeSyncRemoteDataSource
    private lateinit var testAuthRepo: TestAuthRepository

    private val userAId = "user-a-uuid-1111"
    private val userBId = "user-b-uuid-2222"

    class TestAuthRepository(
        private val ownershipManager: DatasetOwnershipManager
    ) : AuthRepository {
        private val _authState = MutableStateFlow<AuthState>(AuthState.LocalOnly)
        override val authState: StateFlow<AuthState> = _authState
        private var currentUserId: String? = null
        private var currentEmail: String? = null

        override suspend fun signIn(email: String, password: String): Result<Unit> {
            currentEmail = email
            currentUserId = if (email.contains("user_b")) "user-b-uuid-2222" else "user-a-uuid-1111"
            refreshAuthState()
            return Result.success(Unit)
        }

        override suspend fun signUp(email: String, password: String): Result<Unit> = signIn(email, password)

        override suspend fun signOut(): Result<Unit> {
            currentUserId = null
            currentEmail = null
            _authState.value = AuthState.LocalOnly
            return Result.success(Unit)
        }

        override suspend fun refreshAuthState() {
            val uid = currentUserId
            if (uid != null) {
                val owner = ownershipManager.getOwnerUserId()
                if (owner != null && owner != uid) {
                    _authState.value = AuthState.AccountMismatch(
                        currentUserId = uid,
                        currentEmail = currentEmail ?: "",
                        ownerUserId = owner
                    )
                } else {
                    _authState.value = AuthState.Authenticated(
                        userId = uid,
                        email = currentEmail ?: ""
                    )
                }
            } else {
                _authState.value = AuthState.LocalOnly
            }
        }

        override fun getCurrentUserId(): String? = currentUserId
        override fun getCurrentEmail(): String? = currentEmail
    }

    @Before
    fun setUp() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        cursorManager = SyncCursorManager(database.syncMetadataDao())
        ownershipManager = DatasetOwnershipManager(database.syncMetadataDao())
        fakeRemote = FakeSyncRemoteDataSource()
        testAuthRepo = TestAuthRepository(ownershipManager)

        syncManager = SyncManager(
            database = database,
            remoteDataSource = fakeRemote,
            cursorManager = cursorManager,
            authRepository = testAuthRepo,
            ownershipManager = ownershipManager
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun exactManualScenario_52Initial_LoginA_Add10_LogoutA_LoginB_BlocksMismatch() = runBlocking {
        // Step 1: App starts with 52 unowned containers stored locally before login
        assertNull(ownershipManager.getOwnerUserId())
        for (i in 1..52) {
            database.collectionDao().insertCollection(
                CollectionEntryEntity(
                    remoteId = "initial-$i",
                    containerCount = 1,
                    timestamp = 1000L + i,
                    estimatedValueCents = 10L,
                    remoteVersion = 0L
                )
            )
        }
        val initialList = database.collectionDao().getAllCollections().first()
        assertEquals(52, initialList.size)
        assertEquals(52, initialList.sumOf { it.containerCount })

        // Step 2: Login as USER_A -> link dataset to A
        testAuthRepo.signIn("user_a@test.com", "passA123")
        assertTrue(testAuthRepo.authState.value is AuthState.Authenticated)
        fakeRemote.currentUserId = userAId

        // Initial sync pushes the 52 local records to A
        val syncASuccess = syncManager.syncAll()
        assertTrue(syncASuccess)
        assertEquals(userAId, ownershipManager.getOwnerUserId())
        assertEquals(52, fakeRemote.pushedCollections.size)

        // Step 4: Add +10 containers -> total 62
        for (i in 1..10) {
            database.collectionDao().insertCollection(
                CollectionEntryEntity(
                    remoteId = "user-a-add-$i",
                    containerCount = 1,
                    timestamp = 2000L + i,
                    estimatedValueCents = 10L,
                    remoteVersion = 0L
                )
            )
        }
        val total62List = database.collectionDao().getAllCollections().first()
        assertEquals(62, total62List.sumOf { it.containerCount })

        // Step 5: Logout A
        testAuthRepo.signOut()
        assertEquals(AuthState.LocalOnly, testAuthRepo.authState.value)
        assertNull(testAuthRepo.getCurrentUserId())

        // Step 6: Local total remains 62, and owner remains A
        val afterLogoutList = database.collectionDao().getAllCollections().first()
        assertEquals(62, afterLogoutList.sumOf { it.containerCount })
        assertEquals(userAId, ownershipManager.getOwnerUserId())

        // Step 7: Login as a DIFFERENT USER_B
        testAuthRepo.signIn("user_b@test.com", "passB123")
        fakeRemote.currentUserId = userBId

        // Assert: ACCOUNT_MISMATCH is detected!
        val authStateB = testAuthRepo.authState.value
        assertTrue("Expected AccountMismatch state for USER_B", authStateB is AuthState.AccountMismatch)
        val mismatch = authStateB as AuthState.AccountMismatch
        assertEquals(userBId, mismatch.currentUserId)
        assertEquals(userAId, mismatch.ownerUserId)

        // Assert: Sync guard blocks syncAll for B!
        val syncBResult = syncManager.syncAll()
        assertFalse("syncAll must be aborted for USER_B due to account mismatch", syncBResult)

        // Assert: No records of A are uploaded to B!
        val recordsUploadedUnderB = fakeRemote.pushedCollections.filter { it.remoteId.startsWith("user-a") }
        // All 52 original records were pushed under A, zero pushed under B
        assertEquals(52, fakeRemote.pushedCollections.size)

        // Owner remains A
        assertEquals(userAId, ownershipManager.getOwnerUserId())

        // Step 8: Login USER_A again
        testAuthRepo.signOut()
        testAuthRepo.signIn("user_a@test.com", "passA123")
        fakeRemote.currentUserId = userAId

        assertTrue(testAuthRepo.authState.value is AuthState.Authenticated)
        val syncAResumeResult = syncManager.syncAll()
        assertTrue("Sync must resume successfully for legitimate owner USER_A", syncAResumeResult)

        // The additional 10 items added by A are now synced under A (52 + 10 = 62)
        assertEquals(62, fakeRemote.pushedCollections.size)
    }

    @Test
    fun crossAccountContaminationTest_PendingOutboxOfA_NeverSentWithBJwt() = runBlocking {
        // Given USER_A is logged in and dataset is bound to A
        ownershipManager.bindOwner(userAId)
        testAuthRepo.signIn("user_a@test.com", "passA123")
        fakeRemote.currentUserId = userAId

        // A creates a collection and it is queued into outbox
        val recordAId = "pending-outbox-a-${UUID.randomUUID()}"
        database.collectionDao().insertCollection(
            CollectionEntryEntity(
                remoteId = recordAId,
                containerCount = 25,
                timestamp = 3000L,
                estimatedValueCents = 250L,
                remoteVersion = 0L
            )
        )
        database.syncOutboxDao().enqueueCoalesced(
            entityType = OutboxEntityType.COLLECTION_ENTRY.name,
            entityRemoteId = recordAId,
            operationType = OutboxOperationType.UPSERT.name,
            isLocallyCreatedOnly = true
        )

        // Verify outbox has 1 pending operation
        assertEquals(1, database.syncOutboxDao().getPendingBatch(50).size)

        // Logout USER_A
        testAuthRepo.signOut()
        assertEquals(userAId, ownershipManager.getOwnerUserId())

        // Login USER_B
        testAuthRepo.signIn("user_b@test.com", "passB123")
        fakeRemote.currentUserId = userBId

        // Attempt to process outbox while logged in as B
        val batchResult = syncManager.processOutboxBatch()
        assertFalse("processOutboxBatch must abort when current user is B and dataset owner is A", batchResult)

        val syncAllResult = syncManager.syncAll()
        assertFalse("syncAll must abort when current user is B and dataset owner is A", syncAllResult)

        // Invariant: ZERO operations sent to remote
        assertEquals(0, fakeRemote.pushedCollections.size)

        // The outbox operation remains intact and unconsumed
        assertEquals(1, database.syncOutboxDao().getPendingBatch(50).size)
    }

    @Test
    fun destructiveWipeAndRebind_ClearsPersonalDataAndBindsCleanDatasetToB() = runBlocking {
        // Given dataset owned by A with 62 containers and pending outbox
        ownershipManager.bindOwner(userAId)
        for (i in 1..62) {
            database.collectionDao().insertCollection(
                CollectionEntryEntity(
                    remoteId = "a-$i",
                    containerCount = 1,
                    timestamp = 1000L,
                    estimatedValueCents = 10L,
                    remoteVersion = 0L
                )
            )
        }
        database.syncOutboxDao().enqueueCoalesced(
            entityType = OutboxEntityType.COLLECTION_ENTRY.name,
            entityRemoteId = "a-1",
            operationType = OutboxOperationType.UPSERT.name,
            isLocallyCreatedOnly = true
        )
        cursorManager.setCursor(42L)

        // Login B -> Mismatch
        testAuthRepo.signIn("user_b@test.com", "passB123")
        fakeRemote.currentUserId = userBId
        assertTrue(testAuthRepo.authState.value is AuthState.AccountMismatch)

        // USER_B explicitly confirms Option B: clear personal dataset and rebind
        database.clearPersonalDatasetAndRebind(userBId)
        testAuthRepo.refreshAuthState()

        // Verify dataset ownership is now bound to B
        assertEquals(userBId, ownershipManager.getOwnerUserId())
        assertTrue(testAuthRepo.authState.value is AuthState.Authenticated)

        // Verify personal records were wiped
        val remainingCollections = database.collectionDao().getAllCollections().first()
        assertEquals(0, remainingCollections.size)

        // Verify outbox was cleared
        assertEquals(0, database.syncOutboxDao().getPendingBatch(50).size)

        // Verify cursor was reset to 0
        assertEquals(0L, cursorManager.getCursor())

        // Verify sync for B now proceeds cleanly without errors
        val syncBSuccess = syncManager.syncAll()
        assertTrue(syncBSuccess)
    }
}
