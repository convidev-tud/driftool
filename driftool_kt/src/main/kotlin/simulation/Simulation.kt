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
import kotlin.math.pow
import kotlin.math.sqrt

abstract class Simulation {

    abstract fun run(): DriftReport

    companion object {

        const val TOLERANCE: Float = 0.001f

        fun calculateEmbeddings(matrix: MatrixResult): PointCloud {
            if (matrix.data.isEmpty()){
                Log.append("Matrix is empty, skipping embedding calculation")
                return PointCloud(mutableListOf(), matrix.sortedBranchList)
            }
            if (matrix.data.size == 1){
                Log.append("Matrix has only one point, skipping embedding calculation")
                Log.append("Setting point to 0,0,0")
                return PointCloud(mutableListOf(Triple(0.0f, 0.0f, 0.0f)), matrix.sortedBranchList)
            }
            if (matrix.data.flatten().count{ it > TOLERANCE} == 0){
                Log.append("All values are 0 within TOLERANCE of $TOLERANCE, skipping embedding calculation")
                Log.append("Setting all points to 0,0,0")
                val zeroPointCloud = PointCloud(mutableListOf(), matrix.sortedBranchList)
                for (i in 0 until matrix.data.size){
                    zeroPointCloud.addPoint(0.0f, 0.0f, 0.0f)
                }
                return zeroPointCloud
            }

            Log.append("Calculating embeddings")
            val directoryHandler = DataProvider.getDirectoryHandler()
            val matrixJson = matrix.toJsonString()

            val outFileName = directoryHandler.createTemporalFile()
            val inFileName = directoryHandler.createTemporalFile()

            val outFile = File(outFileName)
            outFile.writeText(matrixJson)

            val cmd = Shell.exec(
                arrayOf("python", "math/embedding.py", outFileName, matrix.data.size.toString(), inFileName),
                DataProvider.getSupportPath())
            if (cmd.exitCode != 0) {
                Log.append("Error calculating embeddings, could not call python script")
                throw Exception("Error calculating embeddings, could not call python script")
            }
            Log.append("Embeddings calculated!")
            Log.append(cmd.output)

            val inFile = File(inFileName)
            /*
            Example:
            1;2.3;3
            2;3.5;7
            6;0;0.5
             */
            val embeddingsCSV = inFile.readLines().filter { it.isNotBlank() }
            val embeddedPointCloud = PointCloud(mutableListOf(), matrix.sortedBranchList)
            assert(embeddingsCSV.size == matrix.data.size) { "Embeddings CSV does not match matrix size" }
            for (line in embeddingsCSV) {
                val parts = line.split(";")
                assert(parts.size == 3) { "Embeddings CSV line does not have 3 parts" }
                embeddedPointCloud.addPoint(parts[0].toFloat(), parts[1].toFloat(), parts[2].toFloat())
            }
            return embeddedPointCloud
        }

        /**
         * Calculate the drift of a point cloud.
         * Uses the average distance of each point to the median point.
         * This method used the marginal median, which is the median of each axis, i.e., an approximation of the geometric median.
         * The median point may not be in the point cloud but only virtual.
         *
         * @param pointCloud The point cloud to calculate the drift of.
         */
        fun calculateDrift(pointCloud: PointCloud): Float {

            if (pointCloud.points.isEmpty()){
                Log.append("Point cloud is empty, skipping drift calculation")
                return 0.0f
            }

            if(pointCloud.points.size == 1){
                Log.append("Point cloud has only one point, skipping drift calculation")
                return 0.0f
            }

            val points = pointCloud.points
            val xMedian = median(points.map { it.first })
            val yMedian = median(points.map { it.second })
            val zMedian = median(points.map { it.third })

            val l = points.size.toFloat()
            var d = 0.0f
            for (p in points) {
                d += sqrt((p.first - xMedian).pow(2) + (p.second - yMedian).pow(2) + (p.third - zMedian).pow(2))
            }
            return d / l
        }

        fun median(values: List<Float>): Float {
            val sorted = values.sorted()
            val size: Int = sorted.size
            return if (size % 2 == 0) {
                (sorted[(size / 2).toInt() - 1] + sorted[(size / 2).toInt()]) / 2
            } else {
                sorted[(size / 2).toInt()]
            }
        }

    }
}