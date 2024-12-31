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

import io.driftool.Log
import io.driftool.reporting.Distance
import io.driftool.shell.DirectoryHandler
import io.driftool.shell.Shell
import java.nio.file.Path
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.concurrent.thread
import kotlin.io.path.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.isSymbolicLink
import kotlin.io.path.listDirectoryEntries

class Repository(val location: String) {

    private val allBranches: MutableList<String> = mutableListOf()
    private val branchesOfInterest: MutableList<String> = mutableListOf()
    private var currentBranch: String? = null
    var defaultBranch: String? = null

    fun findAllBranches(threadIdx: Int? = null): List<String> {
        val listBranchResult = Shell.exec(arrayOf("git", "branch", "--all"), location)

        if (! listBranchResult.isSuccessful()){
            throw RuntimeException("Could not list branches: git branch --all failed in given location")
        }
        val stdout = listBranchResult.output
        val allBranches: MutableList<String> = mutableListOf()

        for (line in stdout.split("\n")){
            val cleanedLine = line
                .replace("remotes/origin/", "")
                .replace("*", "")
                .replace(" ", "")
            if(cleanedLine.isNotEmpty() && !cleanedLine.contains("HEAD->") && cleanedLine.isNotBlank() && !allBranches.contains(cleanedLine)){
                allBranches.add(cleanedLine)
                Log.appendAsync(threadIdx, "added branch : $cleanedLine")
            }
        }
        this.allBranches.clear()
        this.allBranches.addAll(allBranches)
        allBranches.sort()
        return allBranches
    }

    /**
     * Returns a list of branches that are not ignored by the repository.
     * This applies the list of ignored branches.
     * This also applies the timeout days.
     * The branches of interest are returned and stored in the [branchesOfInterest] list.
     * @param timeoutDays The number of days without activity after that a branch is considered to be out of interest.
     * @param ignoreBranchesPatterns A list of regular expressions that define branches that should be ignored.
     * @return A list of branches that are not ignored
     */
    fun findBranchesOfInterest(timeoutDays: Int, ignoreBranchesPatterns: List<String>, threadIdx: Int? = null): List<String> {
        Log.appendAsync(threadIdx, ">>> Start getBranchesOfInterest")

        val lastCommitDatePerBranch: Map<String, Instant> = findModificationDates(threadIdx)
        val todayAtNoonUTC = Instant.now().atZone(ZoneOffset.UTC).withHour(12).withMinute(0).withSecond(0).withNano(0).toInstant()
        val todayAtNoonUTCMinusTimeout = todayAtNoonUTC.minusSeconds(timeoutDays.toLong() * 24 * 60 * 60)

        //Check if a branch is ignored because of the regex or the commit-date timeout
        //checkout every analyzed branch locally

        for (branch in allBranches){
            var ignore = false
            for (expr in ignoreBranchesPatterns){
                val match = expr.toRegex().find(branch)
                if (match != null){
                    ignore = true
                    break
                }
            }
            if (branch in lastCommitDatePerBranch){
                if (timeoutDays > 0 && lastCommitDatePerBranch[branch]!!.isBefore(todayAtNoonUTCMinusTimeout) ){
                    ignore = true
                }
            } else {
                Log.appendAsync(threadIdx, "PARSING PROBLEM: Branch $branch not found in last_commits")
                ignore = true
            }
            if (!ignore){
                Log.appendAsync(threadIdx, "Branch $branch is not ignored")
                branchesOfInterest.add(branch)
            }else{
                Log.appendAsync(threadIdx, "Branch $branch is ignored")
            }
        }

        branchesOfInterest.sort()
        return branchesOfInterest
    }

    fun overrideBranchesOfInterest(branches: List<String>){
        branchesOfInterest.clear()
        branchesOfInterest.addAll(branches)
        branchesOfInterest.sort()
    }

    fun getBranchesOfInterest(): List<String> {
        branchesOfInterest.sort()
        return branchesOfInterest
    }

    fun getCurrentBranch(): String {
        return currentBranch ?: throw RuntimeException("Current branch is not set")
    }

    fun getAllBranches(): List<String> {
        allBranches.sort()
        return allBranches
    }

    fun findModificationDates(threadIdx: Int? = null): Map<String, Instant> {
        /*
        Get the last commit timestamps for each branch:
        2024-11-20~main
        2024-11-20~origin
        2024-11-20~origin/feature/a
        2024-11-20~origin/feature/b
        2024-11-20~origin/feature/c
        2024-11-20~origin/main
        */
        val lastCommitDates: MutableMap<String, Instant> = mutableMapOf()
        val gitDateStringsShellResult = Shell.execComplexCommand("git branch -a --format=\"%(committerdate:short)~%(refname:short)\" | grep -v HEAD", location, threadIdx)

        if(!gitDateStringsShellResult.isSuccessful()){
            throw RuntimeException("Could not get last commit timestamps for each branch")
        }
        val gitDateStrings = gitDateStringsShellResult.output.split("\n")

        for(dateString in gitDateStrings){
            if(! dateString.contains("~")){
                continue
            }
            val split = dateString.split("~")
            // Setting the timeout time to always 12:00 avoids timout problems depending on the analysis time.
            // Consequently, all analysis runs of the same day will lead to the same results.
            val commitDate: Instant = LocalDate.parse(split[0]).atStartOfDay(ZoneOffset.UTC).withHour(12).withMinute(0).withSecond(0).withNano(0).toInstant()

            var branch = split[1]
            if(branch.startsWith("origin/")){
                branch = branch.replace("origin/", "")
            }
            lastCommitDates.put(branch, commitDate)
            Log.appendAsync(threadIdx, "RAW: $dateString")
            Log.appendAsync(threadIdx, "Branch $branch last commit: $commitDate")
        }

        return lastCommitDates
    }

    fun applyWhiteList(whiteList: List<String>, rootLocation: String? = null, threadIdx: Int? = null) {
        Log.appendAsync(threadIdx, "Applying whitelist to branch $currentBranch in location $location")
        val workingLocation = rootLocation ?: location
        applyPathList(whiteList, workingLocation, true, threadIdx)
    }

    fun applyBlackList(blackList: List<String>, rootLocation: String? = null, threadIdx: Int? = null) {
        Log.appendAsync(threadIdx, "Applying blacklist to branch $currentBranch in location $location")
        val workingLocation = rootLocation ?: location
        applyPathList(blackList, workingLocation, false, threadIdx)
    }

    private fun applyPathList(list: List<String>, rootLocation: String, keepMatches: Boolean, threadIdx: Int? = null){
        val gitPattern = Regex("\\.git")
        val patterns = list.map { it.toRegex() }
        val locationSuffix = rootLocation.removePrefix(location)
        Log.appendAsync(threadIdx, "Applying list to locationSuffix: $locationSuffix")
        val allFilesInLocation: List<Path> = Path(rootLocation).listDirectoryEntries()

        for(elem in allFilesInLocation){
            Log.appendAsync(threadIdx, "Checking file: ${elem.fileName} /// $elem")
            if(gitPattern.find(elem.toString()) != null){
                //Skip everything related to the git repository itself (.git/.gitignore/.gitkeep/...
                Log.appendAsync(threadIdx, "--skip (git related)")
                continue
            }
            if(elem.isSymbolicLink()){
                Log.appendAsync(threadIdx, "Symbolic link found: ${elem.fileName} -> deleting")
                delete(elem, threadIdx)
                Log.appendAsync(threadIdx, "--delete")
                continue
            }
            if(elem.isDirectory()){
                Log.appendAsync(threadIdx, "--traverse")
                if(keepMatches){
                    applyPathList(list, elem.toString(), true, threadIdx)
                }else{
                    applyPathList(list, elem.toString(), false, threadIdx)
                }
                continue
            }

            val elemSuffix = elem.toString().removePrefix(location)
            Log.appendAsync(threadIdx, "--check file:  + $elemSuffix")
            val matchList: MutableList<Boolean> = mutableListOf()
            for(pattern in patterns){
                val isMatch = pattern.find(elemSuffix) != null
                matchList.add(isMatch)
                if(isMatch && !keepMatches){
                    break
                }
            }
            //case applying the blacklist and file is in the blacklist
            //if there is at least one match, then remove t
            if(matchList.contains(true) && !keepMatches) {
                Log.appendAsync(threadIdx, "--delete")
                delete(elem, threadIdx)
                continue
            }
            //case applying the whitelist and file is not in the whitelist
            //delete the file of no match was found, i.e., no pattern to keep the file was specified
            if(matchList.count { it } == 0  && keepMatches){
                Log.appendAsync(threadIdx, "--delete")
                delete(elem, threadIdx)
                continue
            }
        }
    }

    private fun delete(elem: Path, threadIdx: Int? = null){
        val result = Shell.rm(elem.toString(), null, threadIdx)
        if(!result.isSuccessful()){
            Log.appendAsync(threadIdx, "Could not delete file: ${elem.fileName}")
            Log.appendAsync(threadIdx, result.error)
        }
    }

    fun commitChanges(message: String, threadIdx: Int? = null) {
        Log.appendAsync(threadIdx, "Committing changes to branch $currentBranch with message: $message")
        Log.appendAsync(threadIdx, "Adding all files")

        val addResult = Shell.exec(arrayOf("git", "add", "--all"), location, threadIdx)
        if (! addResult.isSuccessful()){
            throw RuntimeException("Could not add all files to git")
        }
        val commitResult = Shell.exec(arrayOf("git", "commit", "-m", message), location, threadIdx)
        //if (! commitResult.isSuccessful()){
        //    throw RuntimeException("Could not commit changes to git")
        //}
    }

    fun initializeCurrentBranch(threadIdx: Int? = null) {
        Log.appendAsync(threadIdx, "Initializing current branch")
        val branchShellResult = Shell.exec(arrayOf("git", "branch", "--show-current"), location, threadIdx)
        if (! branchShellResult.isSuccessful()){
            throw RuntimeException("Could not get current branch")
        }
        currentBranch = branchShellResult.output.trim()
    }

    fun checkoutBranch(branch: String, threadIdx: Int? = null) {
        Log.appendAsync(threadIdx, "Checking out from $currentBranch into $branch")
        val result = Shell.exec(arrayOf("git", "checkout", branch), location, threadIdx)
        //if (! result.isSuccessful()){
        //    throw RuntimeException("Could not checkout from $currentBranch into $branch")
        //}
        currentBranch = branch
    }

    fun resetHard(threadIdx: Int? = null) {
        Log.appendAsync(threadIdx, "Resetting current branch $currentBranch")
        val result = Shell.exec(arrayOf("git", "reset", "--hard"), location, threadIdx)
        if (! result.isSuccessful()){
            throw RuntimeException("Could not reset current branch $currentBranch")
        }
    }

    fun cleanFDX(threadIdx: Int? = null) {
        Log.appendAsync(threadIdx, "Cleaning (-fdx) current branch $currentBranch")
        val result = Shell.exec(arrayOf("git", "clean", "-fdx"), location, threadIdx)
        if (! result.isSuccessful()){
            throw RuntimeException("Could not clean current branch $currentBranch")
        }
    }

    fun sanitize(threadIdx: Int? = null) {
        Log.appendAsync(threadIdx, "Sanitizing current branch $currentBranch")
        resetHard()
        cleanFDX()
    }

    fun mergeAndCountConflicts(baseBranch: String, incomingBranch: String, threadIdx: Int? = null): Distance {
        var numberConflictFiles: Int = 0
        var numberConflicts: Int = 0
        var numberConflictLines: Int = 0
        sanitize(threadIdx)
        checkoutBranch(baseBranch, threadIdx)
        //TODO: count files in the branch for reference
        val mergeResult = Shell.exec(arrayOf("git", "merge", incomingBranch), location, threadIdx)

        if (! mergeResult.isSuccessful()){
            Log.appendAsync(threadIdx, "Could not merge $incomingBranch into $baseBranch")
            Log.appendAsync(threadIdx, "Error:" + mergeResult.error)
            throw RuntimeException("Could not merge $incomingBranch into $baseBranch")
        }
        Log.appendAsync(threadIdx, "STDOUT: " + mergeResult.output)

        val stdoutLines = mergeResult.output.split("\n")
        val conflictIndicatingLines = mutableListOf<String>()

        for (line in stdoutLines){
            if (line.contains("Merge conflict in")){
                numberConflictFiles++
                conflictIndicatingLines.add(line)
                Log.appendAsync(threadIdx, "Conflict in file: $line")
            }
        }

        if(conflictIndicatingLines.isEmpty()){
            Log.appendAsync(threadIdx, "No conflicts found")
            return Distance(lineDistance = numberConflictLines, conflictDistance = numberConflicts, fileDistance = numberConflictFiles)
        }

        val conflictingFilePaths = conflictIndicatingLines.map { it.split("Merge conflict in ")[1].replace("\n", "").trim() }

        for(conflictingFilePath in conflictingFilePaths){
            var localNumberConflicts = 0
            var localNumberConflictLines = 0

            Log.appendAsync(threadIdx, "Investigating conflicting file: $conflictingFilePath")
            var fileLines: List<String> = listOf()
            try {
                val openedFile = Path("$location/$conflictingFilePath").toFile()
                fileLines = openedFile.readLines(charset = Charsets.UTF_8)
            } catch (e: Exception){
                Log.appendAsync(threadIdx, "Error: cannot open conflicting file: $conflictingFilePath")
                Log.appendAsync(threadIdx, e.toString())
                Log.appendAsync(threadIdx, "--> Proceed without action")
                continue
            }

            var insideConflict = false
            var conflictStartIndex = 0

            for((lineIdx, line) in fileLines.withIndex()){
                if(line.trim().startsWith("<<<<<<<") && !insideConflict){
                    localNumberConflicts++
                    insideConflict = true
                    conflictStartIndex = lineIdx
                }
                if(line.trim().startsWith(">>>>>>>") && insideConflict){
                    insideConflict = false
                    localNumberConflictLines += (lineIdx - conflictStartIndex)
                }
            }
            numberConflicts += localNumberConflicts
            numberConflictLines += localNumberConflictLines
            Log.appendAsync(threadIdx, "File $conflictingFilePath has $localNumberConflicts conflicts and $localNumberConflictLines conflicting lines")
        }
        Log.appendAsync(threadIdx, "Total: $numberConflictFiles files with $numberConflicts conflicts and $numberConflictLines conflicting lines")
        return Distance(lineDistance = numberConflictLines, conflictDistance = numberConflicts, fileDistance = numberConflictFiles)
    }

    fun deleteRepository(threadIdx: Int? = null) {
        Log.append("Deleting repository at $location")
        val result = Shell.rmrf(location, null, threadIdx)
        if (! result.isSuccessful()){
            throw RuntimeException("Could not delete repository at $location")
        }
    }

    companion object {

        /**
         * Creates a Repository object by copying a given repository folder to a new location.
         * The repository object of the new location is returned.
         * The target location must be an empty directory that is either already registered by a [io.driftool.shell.DirectoryHandler] or
         * must be registered afterward.
         * Both the path and the location must be absolute paths!
         * @param absoluteRepositoryPath The path to the repository folder that should be copied.
         * @param location The target location where the repository folder should be copied to.
         * @return The repository object of the new location.
         */
        fun cloneFromPath(absoluteRepositoryPath: String, location: String): Repository {
            Log.append("Cloning repository from path: $absoluteRepositoryPath")
            Log.append("Cloning repository to location: $location")
            val cpResult = Shell.cp(
                DirectoryHandler.Companion.ensureDirectoryPathEnding(absoluteRepositoryPath),
                DirectoryHandler.Companion.ensureNoDirectoryPathEnding(location), null)

            Log.append("Exciting Shell Command: " + cpResult.exitCode)
            Log.append("Shell Output: " + cpResult.output)
            Log.append("Shell Error: " + cpResult.error)

            if (! cpResult.isSuccessful()){
                Log.append(cpResult.error)
                throw RuntimeException("Could not copy repository to new temporal location.")
            }
            return Repository(location)
        }

    }
}