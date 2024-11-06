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
import io.driftool.data.GitModeConfiguration
import io.driftool.gitmapping.Repository

abstract class GitSimulation(private val configuration: GitModeConfiguration) : Simulation() {

    private val referenceRepository = Repository.cloneFromPath(
        configuration.repositoryPath,
        DataProvider.getDirectoryHandler().createTemporalDirectory()
    )

    private var workingRepository: Repository? = null

    fun prepareReferenceRepository() {
        referenceRepository.findAllBranches()
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