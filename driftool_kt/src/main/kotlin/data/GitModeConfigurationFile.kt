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

package io.driftool.data

/**
 * The configuration for the Git mode.
 * All values must be set in the configuration file.
 */
data class GitModeConfigurationFile(

    /**
     * If true, a JSON report will be generated and saved in the report directory.
     */
    val jsonReport: Boolean,

    /**
     * If true, an HTML report will be generated and saved in the report directory.
     */
    val htmlReport: Boolean,

    /**
     * List of branches that should be ignored.
     * This is useful for branches that are not relevant for the analysis.
     * The branch list can contain Regex patterns for which are searched in the branch name.
     * Important: We use regex search (not regex match) to find the pattern anywhere in the branch name,
     * e.g. the pattern "feature" will match "feature/branch" and "branch/feature".
     * If the list is empty, no branches will be ignored.
     */
    val ignoreBranches: List<String>,

    /**
     * List of files that should be analyzed exclusively.
     * This is useful if only particular file types should be included in the analysis.
     * The file whitelist is applied before the file blacklist.
     * The file list can contain Regex patterns for which are searched in the file path.
     * Important: We use regex search (not regex match) to find the pattern anywhere in the file path,
     * e.g. the pattern "test" will match "src/test/file" and "file/test".
     * If the list is empty, no files will be ignored.
     */
    val fileWhiteList: List<String>,

    /**
     * List of files that should be ignored.
     * This is useful if particular file types should be excluded from the analysis.
     * The file blacklist is applied after the file whitelist.
     * The file list can contain Regex patterns for which are searched in the file path.
     * Important: We use regex search (not regex match) to find the pattern anywhere in the file path,
     * e.g. the pattern "test" will match "src/test/file" and "file/test".
     * If the list is empty, no files will be ignored.
     */
    val fileBlackList: List<String>,

    /**
     * The number of days a branch had to be active within to be included in the analysis.
     * For example, if the timeoutDays is set to 30, only branches that were active in the last 30 days will be included.
     * If the timeoutDays is set to 0, all branches will be included.
     * This is useful to exclude dead branches as they might invalidate the analysis
     */
    val timeoutDays: Int,

    /**
     * The identifier (or title) for the report.
     * If unset, a unique default identifier will be generated.
     */
    val reportIdentifier: String? = null
)
