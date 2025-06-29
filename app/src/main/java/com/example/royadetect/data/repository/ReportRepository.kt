// data/repository/ReportRepository.kt
package com.example.royadetect.data.repository

import com.example.royadetect.data.dao.ReportDao
import com.example.royadetect.data.entity.Report
import kotlinx.coroutines.flow.Flow

class ReportRepository(private val reportDao: ReportDao) {
    fun getAllReports(): Flow<List<Report>> = reportDao.getAllReports()

    suspend fun insertReport(report: Report): Long = reportDao.insertReport(report)

    suspend fun deleteReport(report: Report) = reportDao.deleteReport(report)

    suspend fun deleteReportById(reportId: Int) = reportDao.deleteReportById(reportId)
}