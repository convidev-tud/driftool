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

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

data class DriftReport (
    val reportTitle: String,
    val analysisTimestamp: String,
    val analysisDurationMillisCheckout: Long,
    val analysisDurationMillisCompare: Long,
    val numberOfBranchesTotal: Int,
    val numberOfBranchesAnalyzed: Int,
    val numberOfFinalBranches: Int,
    val analysisParallelism: Int,
    val sortedBranchList: List<String>,
    val sortedFinalBranchList: List<String>,
    val lineDrift: Double,
    val conflictDrift: Double,
    val fileDrift: Double,
    val lineDistanceMatrix: MatrixResult,
    val conflictDistanceMatrix: MatrixResult,
    val fileDistanceMatrix: MatrixResult,
    val linePointCloud: PointCloud,
    val conflictPointCloud: PointCloud,
    val filePointCloud: PointCloud
){
    fun printReport() {
        println("Report Title: $reportTitle")
        println("Analysis Timestamp: $analysisTimestamp")
        println("Analysis Duration Checkout: $analysisDurationMillisCheckout")
        println("Analysis Duration Compare: $analysisDurationMillisCompare")
        println("Number of Branches Total: $numberOfBranchesTotal")
        println("Number of Branches Analyzed: $numberOfBranchesAnalyzed")
        println("Number of Final Branches: $numberOfFinalBranches")
        println("Sorted Branch List: $sortedBranchList")
        println("Sorted Final Branch List: $sortedFinalBranchList")
        println("Line Drift: $lineDrift")
        println("Conflict Drift: $conflictDrift")
        println("File Drift: $fileDrift")
    }

    fun toJson(): String {
        val mapper = jacksonObjectMapper()
        mapper.enable(SerializationFeature.INDENT_OUTPUT)
        val json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(this)
        return json
    }
}

