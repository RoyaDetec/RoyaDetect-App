// data/dao/ReportDao.kt
package com.example.royadetect.data.dao

import androidx.room.*
import com.example.royadetect.data.entity.Report
import kotlinx.coroutines.flow.Flow

@Dao
interface ReportDao {
    @Query("SELECT * FROM reports ORDER BY timestamp DESC")
    fun getAllReports(): Flow<List<Report>>

    @Insert
    suspend fun insertReport(report: Report): Long

    @Delete
    suspend fun deleteReport(report: Report)

    @Query("DELETE FROM reports WHERE id = :reportId")
    suspend fun deleteReportById(reportId: Int)
}