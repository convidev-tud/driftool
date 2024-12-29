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
            val workingDirectory = supportPath + "tmp_test"

            Shell.mkdir("tmp_test", rootPath)
            DataProvider.setSupportPath(supportPath)
            DataProvider.initDirectoryHandler(workingDirectory)
        }

    }

    val zeroMatrixResult = MatrixResult(listOf(
        listOf(0.0f, 0.0f, 0.0f),
        listOf(0.0f, 0.0f, 0.0f),
        listOf(0.0f, 0.0f, 0.0f)),
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

    @Test
    fun testCalculateEmbeddings() {
        val zeroEmbeddings = Simulation.calculateEmbeddings(zeroMatrixResult)
        assertEquals(zeroEmbeddings.points.size, 3)
        assertEquals(zeroEmbeddings.points[0], Triple(0.0f, 0.0f, 0.0f))
        assertEquals(zeroEmbeddings.points[1], Triple(0.0f, 0.0f, 0.0f))
        assertEquals(zeroEmbeddings.points[2], Triple(0.0f, 0.0f, 0.0f))
    }
}