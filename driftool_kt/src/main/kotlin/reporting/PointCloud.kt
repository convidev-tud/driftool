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

import kotlin.math.pow
import kotlin.math.sqrt

data class PointCloud(val points: MutableList<Triple<Float, Float, Float>>, val sortedBranchList: List<String>) {

    fun addPoint(x: Float, y: Float, z: Float){
        points.add(Triple(x, y, z))
    }

    fun reconstructDistances(): List<Triple<String, String, Float>>{
        val distances = mutableListOf<Triple<String, String, Float>>()
        for(i in points.indices){
            for(j in points.indices){
                val distance = sqrt(
                    (points[i].first - points[j].first.toDouble()).pow(2.0) +
                            (points[i].second - points[j].second.toDouble()).pow(2.0) +
                            (points[i].third - points[j].third.toDouble()).pow(2.0)
                ).toFloat()
                distances.add(Triple(sortedBranchList[i], sortedBranchList[j], distance))
            }
        }
        return distances
    }

}