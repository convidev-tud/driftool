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

package io.driftool.simulation

import io.driftool.DataProvider
import io.driftool.Log
import io.driftool.reporting.DriftReport
import io.driftool.reporting.MatrixResult
import io.driftool.reporting.PointCloud
import io.driftool.shell.Shell
import java.io.File

abstract class Simulation {

    abstract fun run(): DriftReport

    companion object {

        fun calculateEmbeddings(matrix: MatrixResult): PointCloud {
            Log.append("Calculating embeddings")
            val directoryHandler = DataProvider.getDirectoryHandler()
            val matrixJson = matrix.toJsonString()

            val outFileName = directoryHandler.createTemporalFile()
            val inFileName = directoryHandler.createTemporalFile()

            val outFile = File(outFileName)
            outFile.writeText(matrixJson)

            val cmd = Shell.exec(
                arrayOf("python3", "math/embedding.py", inFileName, matrix.data.size.toString(), outFileName),
                DataProvider.getSupportPath())
            if (cmd.exitCode != 0) {
                Log.append("Error calculating embeddings, could not call python script")
                throw Exception("Error calculating embeddings, could not call python script")
            }

            val inFile = File(inFileName)
            /*
            Example:
            1;2.3;3
            2;3.5;7
            6;0;0.5
             */
            val embeddingsCSV = inFile.readLines().filter { it.isNotBlank() }
            val embeddedPointCloud = PointCloud(mutableListOf())
            assert(embeddingsCSV.size == matrix.data.size) { "Embeddings CSV does not match matrix size" }
            for (line in embeddingsCSV) {
                val parts = line.split(";")
                assert(parts.size == 3) { "Embeddings CSV line does not have 3 parts" }
                embeddedPointCloud.addPoint(parts[1].toFloat(), parts[2].toFloat(), parts[3].toFloat())
            }
            return embeddedPointCloud
        }

        fun calculateDrift(pointCloud: PointCloud): Float {
            //TODO -- easy
            throw NotImplementedError("Not yet implemented")
        }

    }
}