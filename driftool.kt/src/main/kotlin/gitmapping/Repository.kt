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

package io.driftool.gitmapping

import io.driftool.shell.Shell

class Repository(val location: String) {

    private val allBranches: MutableList<String> = mutableListOf()
    private val branchesOfInterest: MutableList<String> = mutableListOf()
    private val currentBranch: String? = null

    fun findAllBranches(): List<String> {
        throw NotImplementedError()
    }

    fun getBranchesOfInterest(): List<String> {
        throw NotImplementedError()
    }

    fun getCurrentBranch(): String {
        throw NotImplementedError()
    }

    fun getAllBranches(): List<String> {
        throw NotImplementedError()
    }

    fun findModificationDates(): Map<String, String> {
        throw NotImplementedError()
    }

    fun applyWhiteList(whiteList: List<String>) {
        throw NotImplementedError()
    }

    fun applyBlackList(blackList: List<String>) {
        throw NotImplementedError()
    }

    fun commitChanges(message: String) {
        throw NotImplementedError()
    }

    fun checkoutBranch(branch: String) {
        throw NotImplementedError()
    }

    fun resetHard() {
        throw NotImplementedError()
    }

    fun resetHardToHead() {
        throw NotImplementedError()
    }

    fun getHeadHash(): String {
        throw NotImplementedError()
    }

    fun cleanFDX() {
        throw NotImplementedError()
    }

    fun sanitize() {
        resetHard()
        cleanFDX()
    }

    fun mergeAndCountConflicts(branch: String): Int {
        throw NotImplementedError()
    }

    fun deleteRepository() {
        throw NotImplementedError()
    }

    companion object {

        /**
         * Creates a Repository object by copying a given repository folder to a new location.
         * The repository object of the new location is returned.
         * The target location must be an empty directory that is either already registered by a [DirectoryHandler] or
         * must be registered afterward.
         * Both the path and the location must be absolute paths!
         * @param path The path to the repository folder that should be copied.
         * @param location The target location where the repository folder should be copied to.
         * @return The repository object of the new location.
         */
        fun cloneFromPath(path: String, location: String): Repository {
            println("Cloning repository from path: $path")
            val cpResult = Shell.cp(
                DirectoryHandler.ensureDotEnding(DirectoryHandler.ensureDirectoryPathEnding(path)),
                DirectoryHandler.ensureNoDirectoryPathEnding(location), null)

            if (! cpResult.isSuccessful()){
                throw RuntimeException("Could not copy repository to new temporal location.")
            }
            return Repository(location)
        }

    }
}