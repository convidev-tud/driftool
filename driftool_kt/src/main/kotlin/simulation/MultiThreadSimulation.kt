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

class MultiThreadSimulation(gitModeConfiguration: GitModeConfiguration) : GitSimulation(gitModeConfiguration) {

    val threadCount = gitModeConfiguration.pc.threads

    override fun run(): DriftReport {
        super.prepareReferenceRepository()
        //get all branch combinations
        //TODO
        //copy to all the threads
        //TODO
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

    private fun getBranchCombinations(): List<Pair<String, String>> {
        val branchCombinations = mutableListOf<Pair<String, String>>()

        throw NotImplementedError("Not yet implemented")
    }

}