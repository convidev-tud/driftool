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
import io.driftool.gitmapping.Repository
import io.driftool.reporting.DistanceRelation
import io.driftool.reporting.DistanceResult
import io.driftool.reporting.DriftReport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.thread

class MultiThreadSimulation(val gitModeConfiguration: GitModeConfiguration) : GitSimulation(gitModeConfiguration) {

    val threadCount = gitModeConfiguration.pc.threads

    override fun run(): DriftReport {
        val startingTimestampMillis = System.currentTimeMillis()
        super.prepareReferenceRepository()
        //get all branch combinations
        val branchCombinations = super.getBranchCombinations(includeSymmetries = true, includeIdentities = false)
        //create thread job distribution
        val threadJobs = createThreadJobs(branchCombinations)

        val workingInstances = mutableListOf<Pair<List<Pair<String, String>>, MergeHandler>>()
        for((instanceIndex, threadJob) in threadJobs.withIndex()) {
            val workingRepository = super.createWorkingRepository()
            workingInstances.add(Pair(threadJob, MergeHandler(workingRepository, instanceIndex)))
        }

        val results = ConcurrentHashMap<MergeHandler, DistanceResult>()
        println("Starting threads")
        runBlocking {
            runAllThreads(workingInstances, results)
        }
        println("All threads finished")
        Log.mergeAsyncLogs()

        val joinedResult = DistanceResult(
            DistanceRelation(mutableSetOf()),
            DistanceRelation(mutableSetOf()),
            DistanceRelation(mutableSetOf()))
        for((_, result) in results) {
            joinedResult.join(result)
        }

        val endingTimestampMillis = System.currentTimeMillis()
        val durationMillis = endingTimestampMillis - startingTimestampMillis

        return super.makeReport(joinedResult, durationMillis, startingTimestampMillis, threadCount)
    }

    suspend fun runAllThreads(
        workingInstances: List<Pair<List<Pair<String, String>>, MergeHandler>>,
        results: ConcurrentHashMap<MergeHandler, DistanceResult>) = withContext(Dispatchers.Default) {
        workingInstances.forEach { (job, mergeHandler) ->
            launch {
                thread(start = true) {
                    try {
                        println("Starting thread ${mergeHandler.idx}")
                        val result = mergeHandler.executeMerges(job)
                        results.put(mergeHandler, result)
                        println("Thread ${mergeHandler.idx} finished")
                    } catch (e: Exception) {
                        println("Thread ${mergeHandler.idx} failed")
                        e.printStackTrace()
                    }
                }.join()
            }
        }
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