package io.driftool.reporting

import java.time.LocalDateTime

data class DriftReport (
    val reportTitle: String,
    val analysisTimestamp: LocalDateTime,
    val analysisDurationMillis: Long,
    val numberOfBranchesTotal: Int,
    val numberOfBranchesAnalyzed: Int,
    val drift: Double,
    val sortedBranchList: List<String>,
    val distanceMatrix: MatrixResult,
    val pointCloud: PointCloud
){
    fun printReport() {
        println("Report Title: $reportTitle")
        println("Analysis Timestamp: $analysisTimestamp")
        println("Analysis Duration: $analysisDurationMillis")
        println("Number of Branches Total: $numberOfBranchesTotal")
        println("Number of Branches Analyzed: $numberOfBranchesAnalyzed")
        println("Drift: $drift")
        println("Sorted Branch List: $sortedBranchList")
        println("Distance Matrix: $distanceMatrix")
        println("Point Cloud: $pointCloud")
    }
}

