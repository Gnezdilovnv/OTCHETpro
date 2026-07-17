package com.otchetpro.app.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "reports",
    indices = [
        Index(value = ["dept"], name = "idx_reports_dept"),
        Index(value = ["status"], name = "idx_reports_status"),
        Index(value = ["createdAt"], name = "idx_reports_created_at")
    ]
)
data class Report(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "dept")
    val dept: String = "",
    
    @ColumnInfo(name = "templateName")
    val templateName: String = "",
    
    @ColumnInfo(name = "text")
    val text: String = "",
    
    @ColumnInfo(name = "variables")
    val variables: String = "",
    
    @ColumnInfo(name = "subDepts")
    val subDepts: String = "",
    
    @ColumnInfo(name = "status", defaultValue = "saved")
    val status: String = "saved",
    
    @ColumnInfo(name = "createdAt")
    val createdAt: Long = System.currentTimeMillis()
)
