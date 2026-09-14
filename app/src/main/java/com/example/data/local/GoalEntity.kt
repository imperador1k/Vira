package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "goal")
data class GoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: String, // CONTAINERS, VALUE
    val targetValue: Int, // containers count or cents
    val period: String, // MONTHLY, WEEKLY
    val isActive: Boolean
)
