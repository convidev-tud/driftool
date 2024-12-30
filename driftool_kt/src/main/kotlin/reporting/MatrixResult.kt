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

package io.driftool.reporting

data class MatrixResult(val data: List<List<Float>>, val sortedBranchList: List<String>) {

    /**
     * Example:
     *
     * {
     * "0": [1, 2, 3],
     * "1": [0, 2, 3],
     * "2": [0, 1, 3]
     * }
     *
     * Returns a JSON representation of the matrix result
     * @return JSON representation of the matrix result
     */
    fun toJsonString(): String {
        val jsonString = StringBuilder()
        jsonString.append("{\n")
        for ((lineIndex, line) in data.withIndex()) {
            jsonString.append("\"$lineIndex\": [")
            for (i in line.indices) {
                jsonString.append(line[i])
                if (i < line.size - 1) {
                    jsonString.append(", ")
                }
            }
            if (lineIndex < data.size - 1) {
                jsonString.append("],\n")
            } else {
                jsonString.append("]\n")
            }
        }
        jsonString.append("}")
        return jsonString.toString()
    }

    companion object {
        fun fromDistanceRelation(relation: DistanceRelation): MatrixResult {
            throw NotImplementedError("Not yet implemented")
        }
    }

}