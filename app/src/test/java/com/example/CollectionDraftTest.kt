package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.domain.BalanceService
import com.example.repository.CollectionRepository
import com.example.repository.RedemptionRepository
import com.example.repository.SpotRepository
import com.example.ui.home.HomeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
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

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class CollectionDraftTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var database: AppDatabase
    private lateinit var collectionRepo: CollectionRepository
    private lateinit var spotRepo: SpotRepository
    private lateinit var balanceService: BalanceService
    private lateinit var viewModel: HomeViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        val context = ApplicationProvider.getApplicationContext<Context>()
        val executor = testDispatcher.asExecutor()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .setTransactionExecutor(executor)
            .setQueryExecutor(executor)
            .build()
        collectionRepo = CollectionRepository(database)
        val redemptionRepo = RedemptionRepository(database)
        balanceService = BalanceService(collectionRepo, redemptionRepo)
        spotRepo = SpotRepository(database)
        viewModel = HomeViewModel(collectionRepo, balanceService, spotRepo)
    }

    @After
    fun tearDown() {
        database.close()
        Dispatchers.resetMain()
    }

    @Test
    fun newDraftDefaultsCorrectly() {
        val draft = viewModel.draft.value
        assertEquals(1, draft.quantity)
        assertNull(draft.selectedSpotId)
        assertNull(draft.selectedLatitude)
        assertNull(draft.selectedLongitude)
        assertNull(draft.note)
        assertFalse(draft.hasLocation)
        assertFalse(draft.isSheetOpen)
    }

    @Test
    fun quantityChangesTo10() {
        viewModel.setDraftQuantity(10)
        assertEquals(10, viewModel.draft.value.quantity)
    }

    @Test
    fun mapPickerResultUpdatesLocationAndPreservesQuantity10() {
        viewModel.setDraftQuantity(10)
        assertEquals(10, viewModel.draft.value.quantity)

        // User picks coordinates on MapPicker
        viewModel.setDraftLocation(39.7436, -8.8071)

        val updatedDraft = viewModel.draft.value
        assertEquals("Quantity must remain 10 after location update", 10, updatedDraft.quantity)
        assertEquals(39.7436, updatedDraft.selectedLatitude!!, 0.0001)
        assertEquals(-8.8071, updatedDraft.selectedLongitude!!, 0.0001)
        assertTrue(updatedDraft.hasLocation)
        assertTrue("Sheet must reopen when location is returned", updatedDraft.isSheetOpen)
    }

    @Test
    fun cancelingMapPickerKeepsQuantity10() {
        viewModel.setDraftQuantity(10)
        assertEquals(10, viewModel.draft.value.quantity)

        assertEquals(10, viewModel.draft.value.quantity)
        assertFalse(viewModel.draft.value.hasLocation)
    }

    @Test
    fun savingPersists10AndResetsDraft() = runTest(testDispatcher) {
        viewModel.setDraftQuantity(10)
        viewModel.setDraftLocation(39.7436, -8.8071)
        viewModel.setDraftNote("Test collection")

        var savedCallbackCalled = false
        viewModel.saveDraftCollection {
            savedCallbackCalled = true
        }
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(savedCallbackCalled)
        val entries = collectionRepo.getAllCollections().first()
        assertEquals(1, entries.size)
        val savedEntry = entries.first()
        assertEquals(10, savedEntry.containerCount)
        assertEquals(100L, savedEntry.estimatedValueCents) // 10 * 10c = 100c = 1.00 EUR
        assertEquals(39.7436, savedEntry.latitude!!, 0.0001)
        assertEquals(-8.8071, savedEntry.longitude!!, 0.0001)
        assertEquals("Test collection", savedEntry.note)

        // After successful save, draft must reset to default state
        val resetDraft = viewModel.draft.value
        assertEquals(1, resetDraft.quantity)
        assertNull(resetDraft.selectedLatitude)
        assertNull(resetDraft.selectedLongitude)
        assertFalse(resetDraft.hasLocation)
        assertFalse(resetDraft.isSheetOpen)
    }
}
