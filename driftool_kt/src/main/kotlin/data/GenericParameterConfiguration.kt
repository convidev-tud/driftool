/**
 * Copyright 2025 Karl Kegel
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

data class GenericParameterConfiguration(val inputRootPath: String,
                                         val absoluteWorkingPath: String,
                                         val absoluteInputRepositoryPath: String,
                                         val absoluteConfigPath: String,
                                         val absoluteReportPath: String,
                                         val supportPath: String,
                                         val threads: Int,
                                         val mode: String,
                                         val symmetry: Boolean) {

    override fun toString(): String {
        return "GenericParameterConfiguration(inputRootPath='$inputRootPath', absoluteWorkingPath='$absoluteWorkingPath', " +
                "absoluteInputRepositoryPath='$absoluteInputRepositoryPath', absoluteConfigPath='$absoluteConfigPath', " +
                "absoluteReportPath='$absoluteReportPath', supportPath:'$supportPath' threads=$threads, mode='$mode', symmetry=$symmetry)"
    }
}