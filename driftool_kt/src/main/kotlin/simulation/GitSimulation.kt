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

abstract class GitSimulation(val configuration: GitModeConfiguration) : Simulation() {

    /**
     * The reference repository is the repository that is used as the base for the simulation.
     * It is cloned from the input repository and is not modified during the simulation.
     * The reference repository is used to create the working repository for each simulation run.
     */
    protected val referenceRepository = Repository.cloneFromPath(
        configuration.pc.absoluteInputRepositoryPath,
        DataProvider.getDirectoryHandler().createTemporalDirectory()
    )

    fun prepareReferenceRepository() {
        //find all branches
        val rawBranchList = referenceRepository.findAllBranches()
        Log.append(rawBranchList.toString())

        //build list of branches of interest (naming, timing)
        val interestingBranchList = referenceRepository.findBranchesOfInterest(configuration.fc.timeoutDays, configuration.fc.ignoreBranches)

        referenceRepository.initializeCurrentBranch()
        referenceRepository.defaultBranch = referenceRepository.getCurrentBranch()

        for(branch in interestingBranchList){
            Log.append("Preparing branch $branch")
            referenceRepository.sanitize()
            referenceRepository.checkoutBranch(branch)
            referenceRepository.sanitize()
            referenceRepository.applyWhiteList(configuration.fc.fileWhiteList)
            referenceRepository.commitChanges("Apply whitelist")
            referenceRepository.applyBlackList(configuration.fc.fileBlackList)
            referenceRepository.commitChanges("Apply blacklist")
            Log.append(">>> Branch $branch prepared")
        }
        referenceRepository.sanitize()
    }

    fun createWorkingRepository(): Repository {
        val workingRepository = Repository.cloneFromPath(
            referenceRepository.location,
            DataProvider.getDirectoryHandler().createTemporalDirectory()
        )
        workingRepository.defaultBranch = referenceRepository.defaultBranch
        workingRepository.overrideBranchesOfInterest(referenceRepository.getBranchesOfInterest())
        workingRepository.checkoutBranch(referenceRepository.defaultBranch!!)
        return workingRepository
    }

    protected fun getBranchCombinations(includeSymmetries: Boolean, includeIdentities: Boolean): List<Pair<String, String>> {
        val branchCombinations = mutableListOf<Pair<String, String>>()
        val branches = referenceRepository.getBranchesOfInterest()
        for (a in branches){
            for (b in branches){
                if(a == b) {
                    if (includeIdentities && !branchCombinations.contains(Pair(a, b))){
                        branchCombinations.add(Pair(a, b))
                    }else{
                        continue
                    }
                }
                if(!branchCombinations.contains(Pair(a, b)) && !branchCombinations.contains(Pair(b, a))){
                    branchCombinations.add(Pair(a, b))
                    if (includeSymmetries){
                        branchCombinations.add(Pair(b, a))
                    }
                }
            }
        }
        return branchCombinations
    }

    fun deleteReferenceRepository() {
        referenceRepository.deleteRepository()
    }

}