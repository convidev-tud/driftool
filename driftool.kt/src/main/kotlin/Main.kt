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

@CommandLine.Command(name = "driftool", mixinStandardHelpOptions = true, version = ["v.2.0 (beta)"])
class Checksum : Callable<Int> {

    @CommandLine.Parameters(index = "0", description = ["configuration path (String)." +
            "The type of the configuration file that needs to be provided depends on the mode."])
    var configPath: String = ""

    @CommandLine.Parameters(index = "1", description = ["absolute working directory path (String)." +
            "Path to an empty directory used for temporary files during analysis. " +
            "This directory should be on a RAM-disc for performance reasons. " +
            "The path must be absolute."])
    var workingPath: String = ""

    @CommandLine.Option(
        names = ["-m", "--mode"],
        description = ["Determines the internal simulation mode. " +
                "The mode 'git' simulates a git repository end-to-end; " +
                "The mode 'matrix' calculates drift and generates reports for a pre-calculated matrix of distances."])
    var mode: String = "git"

    @CommandLine.Option(
        names = ["-t", "--threads"],
        description = ["Number of threads used for analysis. " +
                "More threads does not necessarily mean shorter runtime. Speedups are dependent on repository size, " +
                "the number of branches and hardware used. " +
                "The default is 1 and the analysis runs in the main thread. " +
                "IMPORTANT: The number of threads impacts the memory usage (tmp disk space) of the application. " +
                "x threads require x + 2x * [repo size] disk space. " +
                "If the application runs in a RAM-disc (as recommended), disc space refers to RAM-disc space."]
    )
    var threads: Int = 1

    override fun call(): Int {
        runWithConfig(configPath, workingPath, mode, threads)
        return 0
    }
}

fun main(args: Array<String>) {
    exitProcess(CommandLine(Checksum()).execute(*args))
}

fun runWithConfig(configPath: String, workingPath: String, modeArg: String, threads: Int): DriftReport {

    assert(configPath.isNotBlank()) { "configPath must be set" }
    assert(modeArg.isNotBlank()) { "mode must be set" }
    assert(threads > 0) { "threads must be greater than 0" }
    assert(modeArg == "git" || modeArg == "matrix") { "mode must be either git or matrix" }

    val mode = Mode.valueOf(modeArg)
    var jsonReport: Boolean = false
    var htmlReport: Boolean = false
    var reportIdentifier: String = ""

    DataProvider.initDirectoryHandler(workingPath)
    DataProvider.setWorkingDirectory(workingPath)

    val driftReport = when (mode) {
        Mode.git -> {
            runGitSimulation(ConfigurationFile(configPath).parseGitModeConfig(), threads)
        }
        Mode.matrix -> {
            runMatrixSimulation()
        }
        else -> {
            throw IllegalArgumentException("Unknown mode: $mode")
        }
    }

    //TODO: Write report to file

    return driftReport
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
    throw NotImplementedError("Matrix simulation not implemented yet.")
}