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

import io.driftool.DataProvider
import io.driftool.Log
import io.driftool.data.GitModeConfiguration
import io.driftool.gitmapping.Repository

abstract class GitSimulation(configuration: GitModeConfiguration) : Simulation() {

    /**
     * The reference repository is the repository that is used as the base for the simulation.
     * It is cloned from the input repository and is not modified during the simulation.
     * The reference repository is used to create the working repository for each simulation run.
     */
    private val referenceRepository = Repository.cloneFromPath(
        configuration.pc.absoluteInputRepositoryPath,
        DataProvider.getDirectoryHandler().createTemporalDirectory()
    )

    private var workingRepository: Repository? = null

    fun prepareReferenceRepository() {
        //find all branches
        val rawBranchList = referenceRepository.findAllBranches()
        println(rawBranchList.toString())
        Log.append(rawBranchList.toString())

        //build list of branches of interest (naming, timing)
        val interestingBranchList = referenceRepository.getBranchesOfInterest()

        //NEXT TODO
        //checkout interesting branches
        //apply whitelist then blacklist rules and commit
    }

    fun createWorkingRepository(): Repository {
        throw NotImplementedError("Not yet implemented")
    }

    fun executeMerges(branchCombinations: List<Pair<String, String>>): PartialSimulation {
        throw NotImplementedError("Not yet implemented")
    }

    fun deleteWorkingRepository() {
        workingRepository?.deleteRepository()
    }

    fun deleteReferenceRepository() {
        referenceRepository.deleteRepository()
    }

}