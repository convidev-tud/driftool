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
import io.driftool.reporting.DistanceResult
import io.driftool.reporting.DriftReport
import io.driftool.reporting.MatrixResult
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

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
        referenceRepository.initGitUser()
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

    fun makeReport(distanceResult: DistanceResult,
                   durationMillisCheckout: Long,
                   startTimeCompare: Long,
                   startingTimestampMillis: Long,
                   numberThreads: Int): DriftReport {

        val lineDistanceMatrix = MatrixResult.fromDistanceRelation(distanceResult.lineDistances,
            referenceRepository.getBranchesOfInterest(),
            isComplete = configuration.pc.symmetry,
            ensureSymmetry = true,
            zeroIdentities = true,
            trimErrorBranches = true)
        val linePointCloud = calculateEmbeddings(lineDistanceMatrix)
        val lineDrift = calculateDrift(linePointCloud)

        val conflictDistanceMatrix = MatrixResult.fromDistanceRelation(distanceResult.conflictDistances,
            referenceRepository.getBranchesOfInterest(),
            isComplete = configuration.pc.symmetry,
            ensureSymmetry = true,
            zeroIdentities = true,
            trimErrorBranches = true)
        val conflictPointCloud = calculateEmbeddings(conflictDistanceMatrix)
        val conflictDrift = calculateDrift(conflictPointCloud)

        val fileDistanceMatrix = MatrixResult.fromDistanceRelation(distanceResult.fileDistances,
            referenceRepository.getBranchesOfInterest(),
            isComplete = configuration.pc.symmetry,
            ensureSymmetry = true,
            zeroIdentities = true,
            trimErrorBranches = true)
        val filePointCloud = calculateEmbeddings(fileDistanceMatrix)
        val fileDrift = calculateDrift(filePointCloud)

        val durationCompareMillis = System.currentTimeMillis() - startTimeCompare

        return DriftReport(
            reportTitle = configuration.fc.reportIdentifier ?: "Drift Report",
            analysisTimestamp = LocalDateTime.ofInstant(Instant.ofEpochMilli(startingTimestampMillis), ZoneId.systemDefault()).toString(),
            analysisDurationMillisCheckout = durationMillisCheckout,
            analysisDurationMillisCompare = durationCompareMillis,
            analysisParallelism = numberThreads,
            numberOfBranchesTotal = referenceRepository.getAllBranches().size,
            numberOfBranchesAnalyzed = referenceRepository.getBranchesOfInterest().size,
            numberOfFinalBranches = lineDistanceMatrix.sortedBranchList.size,
            sortedBranchList = referenceRepository.getBranchesOfInterest(),
            sortedFinalBranchList = lineDistanceMatrix.sortedBranchList,
            lineDrift = lineDrift.toDouble(),
            conflictDrift = conflictDrift.toDouble(),
            fileDrift = fileDrift.toDouble(),
            lineDistanceMatrix = lineDistanceMatrix,
            conflictDistanceMatrix = conflictDistanceMatrix,
            fileDistanceMatrix = fileDistanceMatrix,
            linePointCloud = linePointCloud,
            conflictPointCloud = conflictPointCloud,
            filePointCloud = filePointCloud
        )
    }

}