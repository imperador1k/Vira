package com.example.domain.collection

/**
 * In-flow state model representing a collection record before persistence.
 * Preserves quantity, selected location and notes across navigation boundaries.
 */
data class CollectionDraft(
    val quantity: Int = 1,
    val selectedSpotId: Int? = null,
    val selectedLatitude: Double? = null,
    val selectedLongitude: Double? = null,
    val note: String? = null,
    val isSheetOpen: Boolean = false
) {
    val hasLocation: Boolean
        get() = selectedLatitude != null && selectedLongitude != null
}
