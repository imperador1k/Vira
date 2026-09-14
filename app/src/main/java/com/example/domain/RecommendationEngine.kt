package com.example.domain

import com.example.data.local.CollectionEntryEntity
import java.util.Calendar
import java.util.Locale

sealed class Insight {
    data class BestDay(val dayOfWeek: String, val averageContainers: Double) : Insight()
    data class Consistency(val weeksInARow: Int) : Insight()
    data class Milestone(val message: String) : Insight()
}

class RecommendationEngine {
    
    fun generateInsights(collections: List<CollectionEntryEntity>): List<Insight> {
        if (collections.isEmpty()) return emptyList()
        
        val insights = mutableListOf<Insight>()
        
        // Best Day of Week
        val calendar = Calendar.getInstance()
        val dayGroups = collections.groupBy { entry ->
            calendar.timeInMillis = entry.timestamp
            calendar.get(Calendar.DAY_OF_WEEK)
        }
        
        val bestDay = dayGroups.maxByOrNull { it.value.sumOf { entry -> entry.containerCount } }
        if (bestDay != null && bestDay.value.isNotEmpty()) {
            val total = bestDay.value.sumOf { it.containerCount }
            val average = total.toDouble() / bestDay.value.size
            val dayName = calendar.apply { set(Calendar.DAY_OF_WEEK, bestDay.key) }
                .getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale("pt", "PT"))
                ?: "Dia"
            insights.add(Insight.BestDay(dayName.replaceFirstChar { it.uppercase() }, average))
        }
        
        // Milestone
        val totalContainers = collections.sumOf { it.containerCount }
        when {
            totalContainers >= 1000 -> insights.add(Insight.Milestone("Incrível! Já recolheste mais de 1000 embalagens."))
            totalContainers >= 500 -> insights.add(Insight.Milestone("Meio milhar! Já recolheste mais de 500 embalagens."))
            totalContainers >= 100 -> insights.add(Insight.Milestone("Parabéns! Ultrapassaste as 100 embalagens."))
            totalContainers >= 10 -> insights.add(Insight.Milestone("Um ótimo começo. Já tens mais de 10 embalagens!"))
        }

        return insights
    }
}
