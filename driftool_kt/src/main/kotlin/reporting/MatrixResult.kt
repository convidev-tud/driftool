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

        /**
         * Creates a matrix result from a distance relation.
         * If the relation is complete, the matrix may be asymmetric.
         * In this case, setting ensureSymmetry to true will calculate the average of the two values (directions).
         * If the relation is marked as incomplete, missing values are set to 0. This may lead to asymmetric matrices.
         * In this case, if ensureSymmetry is set to true, the matrix will be made symmetric by assuming the same
         * distance value in both directions.
         *
         * @param relation The distance relation
         * @param sortedBranchList The list of branches sorted by name
         * @param isComplete Whether the relation is complete, i.e. contains all possible branch pairs.
         * Not complete means missing symmetric values.
         * @param ensureSymmetry Whether to ensure symmetry of the matrix by calculating averages
         * @param zeroIdentities Whether to set the diagonal to 0
         * @param trimErrorBranches Whether to remove branches that result in at least one -1 result. The branch introducing
         * the most errors is reduced first.
         */
        fun fromDistanceRelation(relation: DistanceRelation,
                                 sortedBranchList: List<String>,
                                 isComplete: Boolean,
                                 ensureSymmetry: Boolean,
                                 zeroIdentities: Boolean,
                                 trimErrorBranches: Boolean): MatrixResult {
            if(zeroIdentities){
                for(branch in sortedBranchList){
                    relation.addValue(branch, branch, 0f)
                }
            }

            var trimmedDistanceRelation = relation
            var reducedSortedBranchList = sortedBranchList
            if(trimErrorBranches){
                while (containsNegativeValues(trimmedDistanceRelation)){
                    trimmedDistanceRelation = trimNegativeValues(trimmedDistanceRelation, reducedSortedBranchList)
                    reducedSortedBranchList = getSortedBranchList(trimmedDistanceRelation)
                }
            }
            val analysedRelation = trimmedDistanceRelation

            val matrix = MutableList(reducedSortedBranchList.size) { MutableList(reducedSortedBranchList.size) { 0f } }

            for((fromIndex, fromBranch) in reducedSortedBranchList.withIndex()){
                for((toIndex, toBranch) in reducedSortedBranchList.withIndex()){
                    val distance: Triple<String, String, Float>? = analysedRelation.values.find { it.first == fromBranch && it.second == toBranch }
                    val distanceReverse: Triple<String, String, Float>? = analysedRelation.values.find { it.first == toBranch && it.second == fromBranch }
                    if(isComplete){
                        if(distance != null){
                            if(ensureSymmetry){
                                if(distanceReverse != null){
                                    matrix[fromIndex][toIndex] = (distance.third + distanceReverse.third) / 2
                                } else {
                                    throw IllegalArgumentException("Complete matrix is missing value for $toBranch -> $fromBranch")
                                }
                            } else {
                                matrix[fromIndex][toIndex] = distance.third
                            }
                        } else {
                            throw IllegalArgumentException("Complete matrix is missing value for $fromBranch -> $toBranch")
                        }
                    } else {
                        if(distance != null || distanceReverse != null){
                            if(distance != null && distanceReverse != null && fromBranch != toBranch){
                                throw IllegalArgumentException("Incomplete matrix cannot have both directions!")
                            }
                            if(ensureSymmetry){
                                matrix[fromIndex][toIndex] = distance?.third ?: distanceReverse?.third ?: throw IllegalArgumentException("Unexpected matrix matching error")
                            } else{
                                matrix[fromIndex][toIndex] = distance?.third ?: 0f
                            }
                        } else {
                            matrix[fromIndex][toIndex] = 0f
                        }
                    }
                }
            }
            return MatrixResult(matrix, reducedSortedBranchList)
        }

        /**
         * @see fromDistanceRelation
         */
        fun fromPartialDistanceRelations(partialRelations: List<DistanceRelation>,
                                         sortedBranchList: List<String>,
                                         isComplete: Boolean,
                                         ensureSymmetry: Boolean,
                                         zeroIdentities: Boolean,
                                         trimErrorBranches: Boolean): MatrixResult {
            val joinedRelation = DistanceRelation(mutableSetOf())
            partialRelations.forEach { joinedRelation.join(it) }
            return fromDistanceRelation(joinedRelation, sortedBranchList, isComplete, ensureSymmetry, zeroIdentities, trimErrorBranches)
        }

        fun getSortedBranchList(relation: DistanceRelation): List<String> {
            val branches = mutableSetOf<String>()
            for((from, to, _) in relation.values){
                branches.add(from)
                branches.add(to)
            }
            return branches.sorted()
        }

        fun containsNegativeValues(relation: DistanceRelation): Boolean {
            for((_, _, distance) in relation.values){
                if(distance < -0.5){
                    return true
                }
            }
            return false
        }

        fun trimNegativeValues(relation: DistanceRelation, sortedBranchList: List<String>): DistanceRelation {
            if (!containsNegativeValues(relation)){
                return relation
            }
            val negativeCount = mutableMapOf<String, Int>()
            for(branch in sortedBranchList){
                negativeCount[branch] = 0
            }
            for((from, to, distance) in relation.values){
                if(distance < -0.5){
                    negativeCount[from] = negativeCount[from]!! + 1
                    negativeCount[to] = negativeCount[to]!! + 1
                }
            }
            val mostNegativeBranch = negativeCount.maxByOrNull { it.value }!!.key
            val newValues = relation.values.filter { it.first != mostNegativeBranch && it.second != mostNegativeBranch }
            return DistanceRelation(newValues.toMutableSet())
        }
    }

}