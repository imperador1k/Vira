package com.example.util

import java.text.NumberFormat
import java.util.Locale

object FormatUtils {
    // Portuguese from Portugal (pt-PT) locale for currency
    private val ptPTLocale = Locale("pt", "PT")
    
    fun formatCurrency(cents: Long): String {
        val format = NumberFormat.getCurrencyInstance(ptPTLocale)
        return format.format(cents / 100.0)
    }
}
