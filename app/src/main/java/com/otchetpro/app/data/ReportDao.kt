package com.otchetpro.app.data

import androidx.room.*

@Dao
interface ReportDao {
    @Insert
    suspend fun insert(report: Report): Long

    @Update
    suspend fun update(report: Report)

    @Query("SELECT * FROM reports WHERE dept = :dept ORDER BY createdAt DESC")
    suspend fun getByDept(dept: String): List<Report>

    @Query("SELECT * FROM reports WHERE id = :id")
    suspend fun getById(id: Long): Report?

    @Query("DELETE FROM reports WHERE id = :id")
    suspend fun delete(id: Long)
}
