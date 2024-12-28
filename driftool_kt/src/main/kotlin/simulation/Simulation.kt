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

import io.driftool.reporting.DriftReport
import io.driftool.reporting.MatrixResult
import io.driftool.reporting.PointCloud

abstract class Simulation {

    abstract fun run(): DriftReport

    fun calculateEmbeddings(matrix: MatrixResult): PointCloud {
        throw NotImplementedError("Not yet implemented")
    }

    fun calculateDrift(pointCloud: PointCloud): Float {
        throw NotImplementedError("Not yet implemented")
    }

}