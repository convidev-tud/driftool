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

/**
 * Represents the (partial) distance relation between branches.
 * The relation is represented as a set of triples, where each triple contains the names of two branches and the distance between them.
 * Each triple is aware of the direction of the distance, i.e. the order of the branches: From -> To, Distance
 * @param values The set of triples representing the distance relation
 */
data class DistanceRelation(val values: MutableSet<Triple<String, String, Float>>) {

    fun addValue(from: String, to: String, distance: Float): Boolean {
        return values.add(Triple(from, to, distance))
    }

    fun join(other: DistanceRelation): DistanceRelation {
        values.addAll(other.values)
        return this
    }

}