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

package io.driftool.shell

import java.io.File
import java.util.UUID

/**
 * Provides the set of required shell commands.
 * The commands are executed using the ProcessBuilder class.
 * Results are dependent on the underlying operating system.
 * This class is intended for usage on a Debian based system.
 */
object Shell {

    /**
     * Execute a shell command.
     * @param command The command to execute.
     * @param workingDirectory The working directory for the command. If null, the current working directory is used.
     * @return The result of the command, see [ShellResult].
     */
    fun exec(command: Array<String>, workingDirectory: String?): ShellResult {
        val processBuilder = ProcessBuilder(command.toList())
        if (workingDirectory != null) {
            processBuilder.directory(File(workingDirectory))
        }
        val process = processBuilder.start()
        val inputStream = process.inputStream
        val errorStream = process.errorStream
        process.waitFor()
        val output = inputStream.bufferedReader().readText()
        val error = errorStream.bufferedReader().readText()
        val result = process.exitValue()
        return ShellResult(result, output, error)
    }

    fun execComplexCommand(command: String, workingDirectory: String): ShellResult {
        val randomKey = UUID.randomUUID().toString()
        val commandFileName = "command_$randomKey"
        val commandFile = File.createTempFile(commandFileName, ".sh", File(workingDirectory))
        commandFile.writeText("#!/bin/bash\n$command")
        commandFile.deleteOnExit()
        val result = exec(arrayOf("sh", commandFile.absolutePath), workingDirectory)
        commandFile.delete()
        return result
    }



    /**
     * Create a directory. The directory must not yet exist.
     * @param directory The directory to create.
     * @param workingDirectory The working directory for the command. If null, the current working directory is used.
     * @return The result of the command, see [ShellResult].
     */
    fun mkdir(directory: String, workingDirectory: String?): ShellResult {
        return exec(arrayOf("mkdir", "-p", directory), workingDirectory)
    }

    /**
     * Remove a directory and all its contents.
     * @param directory The directory to remove.
     * @param workingDirectory The working directory for the command. If null, the current working directory is used.
     * @return The result of the command, see [ShellResult].
     */
    fun rmrf(directory: String, workingDirectory: String?): ShellResult {
        return exec(arrayOf("rm", "-rf", directory), workingDirectory)
    }

    /**
     * Copy a file or directory and its contents recursively.
     * This operation uses the cp command with the -r flag. No path modification is done.
     * Check the cp documentation and the companion functions in [DirectoryHandler].
     * @param source The source file or directory.
     * @param target The target file or directory.
     * @param workingDirectory The working directory for the command. If null, the current working directory is used.
     * @return The result of the command, see [ShellResult].
     */
    fun cp(source: String, target: String, workingDirectory: String?): ShellResult {
        return exec(arrayOf("cp", "-r", source, target), workingDirectory)
    }



}