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
import io.driftool.gitmapping.Repository
import io.driftool.reporting.DistanceRelation
import io.driftool.reporting.DistanceResult

class MergeHandler(val workingRepository: Repository, val idx: Int) {

    fun executeMerges(branchCombinations: List<Pair<String, String>>): DistanceResult {
        Log.appendAsync(idx, "Merging branches...")
        val distanceResult = DistanceResult(
            DistanceRelation(mutableSetOf()),
            DistanceRelation(mutableSetOf()),
            DistanceRelation(mutableSetOf()))

        val batchSize = branchCombinations.size
        var current = 1

        for ((baseBranch, incomingBranch) in branchCombinations) {
            println("--> [$idx] Merging $current / $batchSize")
            current++
            Log.appendAsync(idx, "Merging $incomingBranch into $baseBranch")
            val distance = workingRepository.mergeAndCountConflicts(baseBranch, incomingBranch, idx)
            distanceResult.lineDistances.addValue(baseBranch, incomingBranch, distance.lineDistance.toFloat())
            distanceResult.conflictDistances.addValue(baseBranch, incomingBranch, distance.conflictDistance.toFloat())
            distanceResult.fileDistances.addValue(baseBranch, incomingBranch, distance.fileDistance.toFloat())
        }
        Log.appendAsync(idx, "Merging complete")
        return distanceResult
    }

}