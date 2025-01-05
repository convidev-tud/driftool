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

package io.driftool.simulation

import io.driftool.Log
import io.driftool.data.GitModeConfiguration
import io.driftool.data.GitModeConfigurationFile
import io.driftool.reporting.DistanceResult
import io.driftool.reporting.DriftReport
import io.driftool.reporting.MatrixResult
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class MainThreadSimulation(val gitModeConfiguration: GitModeConfiguration) : GitSimulation(gitModeConfiguration) {

        override fun run(): DriftReport {
            val startingTimestampMillis = System.currentTimeMillis()
            super.prepareReferenceRepository()
            val workingRepository = super.createWorkingRepository()
            val branchCombinations = super.getBranchCombinations(
                includeSymmetries = gitModeConfiguration.pc.symmetry,
                includeIdentities = false)
            val mergeHandler = MergeHandler(workingRepository, 0)
            val distanceResult: DistanceResult = mergeHandler.executeMerges(branchCombinations)
            Log.mergeAsyncLogs()
            val endingTimestampMillis = System.currentTimeMillis()
            val durationMillis = endingTimestampMillis - startingTimestampMillis

            return makeReport(distanceResult, durationMillis, startingTimestampMillis, numberThreads = 1)
        }

}