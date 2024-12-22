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

import io.driftool.data.ConfigurationReader
import io.driftool.data.GenericParameterConfiguration
import io.driftool.data.GitModeConfiguration
import io.driftool.data.GitModeConfigurationFile
import io.driftool.gitmapping.DirectoryHandler
import io.driftool.reporting.DriftReport
import io.driftool.simulation.MainThreadSimulation
import io.driftool.simulation.MultiThreadSimulation
import io.driftool.simulation.Simulation
import picocli.CommandLine
import java.util.concurrent.Callable
import kotlin.system.exitProcess

@CommandLine.Command(name = "driftool", mixinStandardHelpOptions = true, version = ["v.2.0 (beta)"])
class Checksum : Callable<Int> {

    @CommandLine.Parameters(index = "0", description = ["absolute input directory path (String)." +
            "Path to the input directory in which the repository, configuration and report directories are located. " +
            "The path must be absolute."])
    var inputRootPath: String = ""

    @CommandLine.Parameters(index = "1", description = ["absolute working directory path (String)." +
            "Path to an empty directory used for temporary files during analysis. " +
            "This directory should be on a RAM-disc for performance reasons. " +
            "The path must be absolute."])
    var workingPath: String = ""

    @CommandLine.Parameters(index = "2", description = ["configuration path (String) from the input dir root." +
            "The type of the configuration file that needs to be provided depends on the mode."])
    var configPath: String = ""

    @CommandLine.Option(
        names = ["-i", "--input_repository"],
        description = ["Path to the input repository. " +
                "The path must start in the input directory root. "])
    var inputRepository: String = ""

    @CommandLine.Option(
        names = ["-o", "--report_location"],
        description = ["Path to the report directory. " +
                "The path must start in the input directory root."])
    var reportPath: String = "out/"

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
        workingPath = DirectoryHandler.ensureDirectoryPathEnding(DirectoryHandler.refactorPathUnixStyle(workingPath))
        inputRootPath = DirectoryHandler.ensureDirectoryPathEnding(DirectoryHandler.refactorPathUnixStyle(inputRootPath))
        inputRepository = DirectoryHandler.ensureNoSlashBeginning(DirectoryHandler.refactorPathUnixStyle(inputRepository))
        configPath = DirectoryHandler.ensureNoSlashBeginning(DirectoryHandler.refactorPathUnixStyle(configPath))
        reportPath = DirectoryHandler.ensureNoSlashBeginning(DirectoryHandler.refactorPathUnixStyle(reportPath))
        val absoluteConfigPath = inputRootPath + configPath
        val absoluteInputRepositoryPath = inputRootPath + inputRepository
        val absoluteReportPath = inputRootPath + reportPath
        val parameterConfiguration = GenericParameterConfiguration(
            inputRootPath, workingPath, absoluteInputRepositoryPath, absoluteConfigPath, absoluteReportPath, threads, mode)
        runWithConfig(parameterConfiguration)
        return 0
    }
}

fun main(args: Array<String>) {
    try {
        exitProcess(CommandLine(Checksum()).execute(*args))
    } catch (ex: NotImplementedError) {
        println("NotImplementedError: ${ex.message}")
        Log.append("NotImplementedError: ${ex.message}")
        exitProcess(1)
    }
}

fun runWithConfig(parameterConfig: GenericParameterConfiguration): DriftReport {

    Log.append(parameterConfig.toString())
    assert(parameterConfig.inputRootPath.isNotBlank()) { "inputRootPath must be set" }
    assert(parameterConfig.absoluteConfigPath.isNotBlank()) { "configPath must be set" }
    assert(parameterConfig.mode.isNotBlank()) { "mode must be set" }
    assert(parameterConfig.threads > 0) { "threads must be greater than 0" }
    assert(parameterConfig.mode == "git" || parameterConfig.mode == "matrix") { "mode must be either git or matrix" }

    val mode = Mode.valueOf(parameterConfig.mode)
    var jsonReport: Boolean = false
    var htmlReport: Boolean = false
    var reportIdentifier: String = ""

    Log.append("Mode: $mode")
    Log.append("Setup DirectoryHandler")
    DataProvider.initDirectoryHandler(parameterConfig.absoluteWorkingPath)
    DataProvider.setWorkingDirectory(parameterConfig.absoluteWorkingPath)

    val driftReport = when (mode) {
        Mode.git -> {
            val gitModeConfiguration = GitModeConfiguration(ConfigurationReader(parameterConfig.absoluteConfigPath).parseGitModeConfig(), parameterConfig)
            runGitSimulation(gitModeConfiguration)
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

fun runGitSimulation(configuration: GitModeConfiguration): DriftReport {
    Log.append("Run Git Simulation")
    val simulation: Simulation = if (configuration.pc.threads > 1) {
        MultiThreadSimulation(configuration)
    } else {
        MainThreadSimulation(configuration)
    }
    return simulation.run()
}

fun runMatrixSimulation(): DriftReport {
    // TODO
    throw NotImplementedError("Matrix simulation not implemented yet.")
}