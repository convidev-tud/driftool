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

import io.driftool.data.GitModeConfiguration
import io.driftool.data.GitModeConfigurationFile
import io.driftool.reporting.DriftReport

class MultiThreadSimulation(val gitModeConfiguration: GitModeConfiguration) : GitSimulation(gitModeConfiguration) {

    val threadCount = gitModeConfiguration.pc.threads

    override fun run(): DriftReport {
        super.prepareReferenceRepository()
        //get all branch combinations
        val branchCombinations = super.getBranchCombinations(includeSymmetries = true, includeIdentities = false)
        //copy to all the threads
        //create thread job distribution
        val threadJobs = createThreadJobs(branchCombinations)
        //run the threads
        //TODO
        //wait for all threads to finish
        //TODO
        //merge the results
        //TODO
        //return the report
        //TODO
        throw NotImplementedError("Not yet implemented")
    }

    private fun createThreadJobs(branchCombinations: List<Pair<String, String>>): List<List<Pair<String, String>>> {
        val threadJobs = mutableListOf<MutableList<Pair<String, String>>>()
        var threadIndex = 0
        for(branchCombination in branchCombinations) {
            if(threadJobs.size <= threadIndex) {
                threadJobs.add(mutableListOf<Pair<String, String>>())
            }
            threadJobs[threadIndex].add(branchCombination)
            threadIndex = (threadIndex + 1) % threadCount
        }
        return threadJobs
    }



}