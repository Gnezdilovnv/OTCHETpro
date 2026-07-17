package com.otchetpro.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reports")
data class Report(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val dept: String = "",
    val templateName: String = "",
    val text: String = "",
    val variables: String = "",
    val subDepts: String = "",
    val status: String = "saved",
    val createdAt: Long = System.currentTimeMillis()
)
