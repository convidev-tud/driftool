/**
 * Copyright 2024 Karl Kegel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

