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

package io.driftool

import io.driftool.data.ConfigurationFile
import io.driftool.data.GitModeConfiguration
import io.driftool.reporting.DriftReport
import io.driftool.simulation.MainThreadSimulation
import io.driftool.simulation.MultiThreadSimulation
import io.driftool.simulation.Simulation
import picocli.CommandLine
import java.util.concurrent.Callable
import kotlin.system.exitProcess

@CommandLine.Command(name = "graph-gentool", mixinStandardHelpOptions = true, version = ["v.0.1"])
class Checksum : Callable<Int> {

    @CommandLine.Parameters(index = "0", description = ["output directory"])
    var configPath: String = ""

    @CommandLine.Option(
        names = ["-m", "--mode"],
        description = ["Sum of nodes and edges in the generated base model (INT)."]
    )
    var mode: String = "git"

    @CommandLine.Option(
        names = ["-t", "--threads"],
        description = ["Sum of nodes and edges in the generated base model (INT)."]
    )
    var threads: Int = 1

    override fun call(): Int {
        runWithConfig(configPath, mode, threads)
        return 0
    }
}

fun main(args: Array<String>) {
    exitProcess(CommandLine(Checksum()).execute(*args))
}

fun runWithConfig(configPath: String, mode: String, threads: Int) {

    assert(configPath.isNotBlank()) { "configPath must be set" }
    assert(mode.isNotBlank()) { "mode must be set" }
    assert(threads > 0) { "threads must be greater than 0" }
    assert(mode == "git" || mode == "matrix") { "mode must be either git or matrix" }

    var mode = Mode.valueOf(mode)
    var jsonReport: Boolean = false
    var htmlReport: Boolean = false
    var reportIdentifier: String = ""

    val driftReport = if (mode == Mode.git) {
        runGitSimulation(ConfigurationFile(configPath).parseGitModeConfig(), threads)
    } else if (mode == Mode.matrix) {
        runMatrixSimulation()
    } else {
        throw IllegalArgumentException("Unknown mode: $mode")
    }

}

fun runGitSimulation(configuration: GitModeConfiguration, threads: Int): DriftReport {
    val simulation: Simulation = if (threads > 1) {
        MultiThreadSimulation(configuration, threads)
    } else {
        MainThreadSimulation(configuration)
    }

    return simulation.run()
}

fun runMatrixSimulation(): DriftReport {
    // TODO
    return DriftReport()
}

fun main() {
    println("Hello World!")
}