package com.example.data.local

/**
 * Local sync metadata state for offline-first persistence.
 * Internal data layer concept - not exposed directly to consumer UI.
 */
enum class SyncState {
    LOCAL_ONLY,
    PENDING_UPLOAD,
    SYNCED,
    PENDING_DELETE,
    CONFLICT
}
