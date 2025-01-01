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

import io.driftool.DataProvider
import io.driftool.reporting.DriftReport
import io.driftool.reporting.MatrixResult
import io.driftool.reporting.PointCloud
import io.driftool.shell.Shell
import io.driftool.simulation.Simulation
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import kotlin.math.pow
import kotlin.math.sqrt

class DriftMethodTests {

    @Test
    fun medianTest() {
        val values = listOf(1.0f, 8.0f, 3.0f, -4.0f, 5.0f)
        val median = Simulation.median(values)
        assertEquals(3.0f, median)
    }

    @Test
    fun medianTestEvenNumber() {
        val values = listOf(1.0f, 8.0f, 3.0f, -4.0f, 5.0f, 9.5f)
        val median = Simulation.median(values)
        assertEquals(4.0f, median)
    }

    @Test
    fun driftCalculationEmptyPointCloud(){
        val drift = Simulation.calculateDrift(PointCloud(mutableListOf(), listOf()))
        assertEquals(0.0f, drift)
    }

    @Test
    fun driftCalculationOneElementPointCloud(){
        val drift = Simulation.calculateDrift(PointCloud(mutableListOf(Triple(1f,2f,3f)), listOf("a")))
        assertEquals(0.0f, drift)
    }

    @Test
    fun driftCalculationTwoElementsPointCloud(){
        val pointA = Triple(1f,2f,3f)
        val pointB = Triple(4f, 5f, 6f)
        val drift = Simulation.calculateDrift(PointCloud(mutableListOf(pointA, pointB), listOf("a", "b")))
        val median = Triple(2.5f, 3.5f, 4.5f)
        val distanceMedianA = sqrt((pointA.first - median.first).pow(2) + (pointA.second - median.second).pow(2) + (pointA.third - median.third).pow(2))
        val distanceMedianB = sqrt((pointB.first - median.first).pow(2) + (pointB.second - median.second).pow(2) + (pointB.third - median.third).pow(2))
        val distanceAVG = (distanceMedianA + distanceMedianB) / 2
        assertEquals(distanceAVG, drift, 0.0001f)
    }

    @Test
    fun driftCalculationThreeElementsPointCloud(){
        val pointA = Triple(1f,2f,3f)
        val pointB = Triple(4f, 5f, 6f)
        val pointC = Triple(7f, -8f, -9f)
        val drift = Simulation.calculateDrift(PointCloud(mutableListOf(pointA, pointB, pointC), listOf("a", "b")))
        val median = Triple(
            Simulation.median(listOf(pointA.first, pointB.first, pointC.first)),
            Simulation.median(listOf(pointA.second, pointB.second, pointC.second)),
            Simulation.median(listOf(pointA.third, pointB.third, pointC.third))
        )
        val distanceMedianA = sqrt((pointA.first - median.first).pow(2) + (pointA.second - median.second).pow(2) + (pointA.third - median.third).pow(2))
        val distanceMedianB = sqrt((pointB.first - median.first).pow(2) + (pointB.second - median.second).pow(2) + (pointB.third - median.third).pow(2))
        val distanceMedianC = sqrt((pointC.first - median.first).pow(2) + (pointC.second - median.second).pow(2) + (pointC.third - median.third).pow(2))
        val distanceAVG = (distanceMedianA + distanceMedianB + distanceMedianC) / 3
        assertEquals(distanceAVG, drift, 0.0001f)
    }

}