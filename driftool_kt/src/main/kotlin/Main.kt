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
import io.driftool.shell.DirectoryHandler
import io.driftool.reporting.DriftReport
import io.driftool.simulation.MainThreadSimulation
import io.driftool.simulation.MultiThreadSimulation
import io.driftool.simulation.Simulation
import picocli.CommandLine
import java.io.File
import java.time.Instant
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

    @CommandLine.Parameters(index = "3", description = ["support path (String) absolute" +
            "The path to the directory from which the support scripts, e.g. math/ and web/ can be accessed"])
    var supportPath: String = ""

    @CommandLine.Option(
        names = ["-i", "--input_repository"],
        description = ["Path to the input repository. " +
                "The path must start in the input directory root. "])
    var inputRepository: String = ""

    @CommandLine.Option(
        names = ["-o", "--report_location"],
        description = ["Path to the report directory. " +
                "The path must start in the input directory root."])
    var reportPath: String = "./"

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

    @CommandLine.Option(
        names = ["-s", "--symmetry"],
        description = ["Defines whether the merges (git) are executed in both directions (symmetry = true) " +
                "or just in one direction (symmetry = false). " +
                "Default is false."])
    var symmetry: Int = 0

    @CommandLine.Option(
        names = ["-p", "--print_log"],
        description = ["Defines if the logger prints main thread events on the fly" +
                "Default is true."])
    var printLog: Int = 1

    @CommandLine.Option(
        names = ["-a", "--print_log_async"],
        description = ["Defines if the logger prints async thread events on the fly." +
                "Events are printed FIFO without taking care of their parent thread id." +
                "Default is false."])
    var printLogAsync: Int = 0

    override fun call(): Int {
        Log.setPrint(printLog == 1)
        Log.setAsyncPrinting(printLogAsync == 1)

        workingPath = DirectoryHandler.ensureDirectoryPathEnding(DirectoryHandler.refactorPathUnixStyle(workingPath))
        inputRootPath = DirectoryHandler.ensureDirectoryPathEnding(DirectoryHandler.refactorPathUnixStyle(inputRootPath))
        inputRepository = DirectoryHandler.ensureNoSlashBeginning(DirectoryHandler.refactorPathUnixStyle(inputRepository))
        configPath = DirectoryHandler.ensureNoSlashBeginning(DirectoryHandler.refactorPathUnixStyle(configPath))
        reportPath = DirectoryHandler.ensureNoSlashBeginning(DirectoryHandler.refactorPathUnixStyle(reportPath))
        supportPath = DirectoryHandler.ensureDirectoryPathEnding(DirectoryHandler.refactorPathUnixStyle(supportPath))

        val absoluteConfigPath = inputRootPath + configPath
        val absoluteInputRepositoryPath = inputRootPath + inputRepository
        val absoluteReportPath = inputRootPath + reportPath
        val parameterConfiguration = GenericParameterConfiguration(
            inputRootPath, workingPath, absoluteInputRepositoryPath, absoluteConfigPath,
            absoluteReportPath, supportPath, threads, mode, symmetry == 1)

        Log.append("Starting Driftool")
        Log.append("Configuration: ${parameterConfiguration.toString()}")
        runWithConfig(parameterConfiguration)

        return 0
    }
}

fun main(args: Array<String>) {
    try {
        exitProcess(CommandLine(Checksum()).execute(*args))
    } catch (ex: NotImplementedError) {
        Log.append("NotImplementedError: ${ex.message}")
        exitProcess(1)
    } catch (ex: Exception) {
        Log.append("Exception: ${ex.message}")
        exitProcess(1)
    } catch (ae: AssertionError) {
        Log.append("AssertionError: ${ae.message}")
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
    assert(parameterConfig.supportPath.isNotBlank()) { "supportPath must be set" }

    var reportIdentifier: String = ""

    try {
        val mode = Mode.valueOf(parameterConfig.mode)
        var jsonReport: Boolean = false
        var htmlReport: Boolean = false
        
        Log.append("Mode: $mode")
        Log.append("Setup DirectoryHandler")
        DataProvider.initDirectoryHandler(parameterConfig.absoluteWorkingPath)
        DataProvider.setSupportPath(parameterConfig.supportPath)

        val driftReport = when (mode) {
            Mode.git -> {
                val gitModeConfiguration = GitModeConfiguration(
                    ConfigurationReader(parameterConfig.absoluteConfigPath).parseGitModeConfig(),
                    parameterConfig
                )
                jsonReport = gitModeConfiguration.fc.jsonReport
                htmlReport = gitModeConfiguration.fc.htmlReport
                reportIdentifier = gitModeConfiguration.fc.reportIdentifier ?: "UNTITLED"
                runGitSimulation(gitModeConfiguration)
            }

            Mode.matrix -> {
                runMatrixSimulation()
            }

            else -> {
                throw IllegalArgumentException("Unknown mode: $mode")
            }
        }

        Log.append("Writing drift report")
        if (jsonReport) {
            val reportJson = driftReport.toJson()
            val jsonReportPath = DirectoryHandler.ensureDirectoryPathEnding(parameterConfig.absoluteReportPath) +
                    reportIdentifier + ".json"
            File(jsonReportPath).writeText(reportJson)
        }
        writeLog(parameterConfig.absoluteReportPath, reportIdentifier)

        return driftReport

    } catch (ex: Exception){
        Log.append("Exception: ${ex.message}")
        writeLog(parameterConfig.absoluteReportPath, reportIdentifier)
        exitProcess(1)
    } catch (ae: AssertionError){
        Log.append("AssertionError: ${ae.message}")
        writeLog(parameterConfig.absoluteReportPath, reportIdentifier)
        exitProcess(1)
    }
}

fun writeLog(reportPath: String, reportIdentifier: String) {
    val logFile = DirectoryHandler.ensureDirectoryPathEnding(reportPath) +
            reportIdentifier + "_" + Instant.now().toEpochMilli().toString() + "_log.txt"
    File(logFile).writer().use { out ->
        Log.listValuePairs().forEach { out.write("${it.first}: ${it.second}\n") }
    }
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