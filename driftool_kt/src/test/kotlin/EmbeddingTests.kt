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
import io.driftool.reporting.MatrixResult
import io.driftool.shell.Shell
import io.driftool.simulation.Simulation
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class EmbeddingTests {

    companion object {

        @BeforeAll
        @JvmStatic
        fun setup(){
            val locationResult = Shell.exec(arrayOf("pwd"), "./")
            if (locationResult.exitCode != 0) {
                throw Exception("Could not determine current location")
            }
            val fullPath = locationResult.output.trim()
            val rootPath = fullPath.split("driftool_kt")[0]
            val supportPath = rootPath
            val workingDirectory = rootPath + "tmp_test"

            Shell.mkdir("tmp_test", rootPath)
            DataProvider.setSupportPath(supportPath)
            DataProvider.initDirectoryHandler(workingDirectory)
        }

        @AfterAll
        @JvmStatic
        fun clean(){
            Shell.exec(arrayOf("rm", "-rf", DataProvider.getDirectoryHandler().rootLocation), null)
        }

    }

    val zeroMatrixResult = MatrixResult(listOf(
        listOf(0.0f, 0.0f, 0.01f),
        listOf(0.0f, 0.0f, 0.0f),
        listOf(0.01f, 0.0f, 0.0f)),
        listOf("a", "b", "c"))

    val evenSizedTriangleMatrixResult = MatrixResult(listOf(
        listOf(0.0f, 2.0f, 2.0f),
        listOf(2.0f, 0.0f, 2.0f),
        listOf(2.0f, 2.0f, 0.0f)),
        listOf("a", "b", "c"))

    val rectangularTriangleMatrixResult = MatrixResult(listOf(
        listOf(0.0f, 4.0f, 3.0f),
        listOf(4.0f, 0.0f, 5.0f),
        listOf(3.0f, 5.0f, 0.0f)),
        listOf("a", "b", "c"))

    val twoElementsMatrixResult = MatrixResult(listOf(
        listOf(0.0f, 7.0f),
        listOf(7.0f, 0.0f)),
        listOf("a", "b"))

    @Test
    fun testCalculateEmbeddingsZeroPoints() {
        val embeddings = Simulation.calculateEmbeddings(zeroMatrixResult)
        println(embeddings.reconstructDistances().toString())
        assertEquals(embeddings.points.size, 3)

        assertEquals(embeddings.points[0].first, 0.0f, 0.1f)
        assertEquals(embeddings.points[0].second, 0.0f, 0.1f)
        assertEquals(embeddings.points[0].third, 0.0f, 0.1f)

        assertEquals(embeddings.points[1].first, 0.0f, 0.1f)
        assertEquals(embeddings.points[1].second, 0.0f, 0.1f)
        assertEquals(embeddings.points[1].third, 0.0f, 0.1f)

        assertEquals(embeddings.points[2].first, 0.0f, 0.1f)
        assertEquals(embeddings.points[2].second, 0.0f, 0.1f)
        assertEquals(embeddings.points[2].third, 0.0f, 0.1f)
    }

    @Test
    fun testCalculateEmbeddingsTwoElements() {
        val embeddings = Simulation.calculateEmbeddings(twoElementsMatrixResult)
        val reconstruction = embeddings.reconstructDistances()
        println(reconstruction.toString())
        assertEquals(embeddings.points.size, 2)
        assertEquals(reconstruction[0].third, 0.0f, 0.1f)
        assertEquals(reconstruction[1].third, 7.0f, 0.1f)
        assertEquals(reconstruction[2].third, 7.0f, 0.1f)
        assertEquals(reconstruction[3].third, 0.0f, 0.1f)
    }

    @Test
    fun testCalculateEvenSizedTriangle(){
        val embeddings = Simulation.calculateEmbeddings(evenSizedTriangleMatrixResult)
        val reconstruction = embeddings.reconstructDistances()
        assertEquals(embeddings.points.size, 3)

        assertEquals(reconstruction[0].third, 0.0f, 0.1f)
        assertEquals(reconstruction[1].third, 2.0f, 0.1f)
        assertEquals(reconstruction[2].third, 2.0f, 0.1f)

        assertEquals(reconstruction[3].third, 2.0f, 0.1f)
        assertEquals(reconstruction[4].third, 0.0f, 0.1f)
        assertEquals(reconstruction[5].third, 2.0f, 0.1f)

        assertEquals(reconstruction[6].third, 2.0f, 0.1f)
        assertEquals(reconstruction[7].third, 2.0f, 0.1f)
        assertEquals(reconstruction[8].third, 0.0f, 0.1f)
    }

    @Test
    fun testCalculateEmbeddingsRectangularTriangle() {
        val embeddings = Simulation.calculateEmbeddings(rectangularTriangleMatrixResult)
        val reconstruction = embeddings.reconstructDistances()
        assertEquals(embeddings.points.size, 3)

        assertEquals(reconstruction[0].third, 0.0f, 0.1f)
        assertEquals(reconstruction[1].third, 4.0f, 0.1f)
        assertEquals(reconstruction[2].third, 3.0f, 0.1f)

        assertEquals(reconstruction[3].third, 4.0f, 0.1f)
        assertEquals(reconstruction[4].third, 0.0f, 0.1f)
        assertEquals(reconstruction[5].third, 5.0f, 0.1f)

        assertEquals(reconstruction[6].third, 3.0f, 0.1f)
        assertEquals(reconstruction[7].third, 5.0f, 0.1f)
        assertEquals(reconstruction[8].third, 0.0f, 0.1f)
    }
}