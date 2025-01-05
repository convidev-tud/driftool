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

import io.driftool.Log
import java.io.File
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Provides the set of required shell commands.
 * The commands are executed using the ProcessBuilder class.
 * Results are dependent on the underlying operating system.
 * This class is intended for usage on a Debian based system.
 */
object Shell {

    var withLogging = true

    /**
     * Execute a shell command.
     * @param command The command to execute.
     * @param workingDirectory The working directory for the command. If null, the current working directory is used.
     * @return The result of the command, see [ShellResult].
     */
    fun exec(command: Array<String>, workingDirectory: String?, threadIdx: Int? = null): ShellResult {
        val processBuilder = ProcessBuilder(command.toList())
        val commandString = command.joinToString(" ")
        if (workingDirectory != null) {
            processBuilder.directory(File(workingDirectory))
        }
        val process = processBuilder.start()
        val inputStream = process.inputStream
        val errorStream = process.errorStream
        val output = inputStream.bufferedReader().readText()
        val error = errorStream.bufferedReader().readText()

        if (!process.waitFor(60, TimeUnit.SECONDS)) {
            try {
                val inputStream = process.inputStream
                val errorStream = process.errorStream
                val output = inputStream.bufferedReader().readText()
                val error = errorStream.bufferedReader().readText()
                
                Log.appendAsync(threadIdx, "Executing Shell Command: $commandString")
                if(output.isNotEmpty()) Log.appendAsync(threadIdx, "Shell Output: $output")
                if(error.isNotEmpty()) Log.appendAsync(threadIdx, "Shell Error: $error")
            }catch(e: Exception){
                Log.appendAsync(threadIdx, "Executing Shell Command: $commandString")
                Log.appendAsync(threadIdx, "FAILED WITH TIMEOUT WITHOUT MESSAGE!")
            }
            
            process.destroy()
            throw RuntimeException("execution timed out: $this")
        }
        
        val result = process.exitValue()
        process.destroy()

        if (withLogging) {
            val commandString = command.joinToString(" ")
            Log.appendAsync(threadIdx, "Executing Shell Command: $commandString")
            Log.appendAsync(threadIdx, "Exiting Shell Command: $result")
            if(output.isNotEmpty()) Log.appendAsync(threadIdx, "Shell Output: $output")
            if(error.isNotEmpty()) Log.appendAsync(threadIdx, "Shell Error: $error")
        }
        return ShellResult(result, output, error)
    }

    fun execComplexCommand(command: String, workingDirectory: String, threadIdx: Int? = null): ShellResult {
        val randomKey = UUID.randomUUID().toString()
        val commandFileName = "command_$randomKey"
        val commandFile = File.createTempFile(commandFileName, ".sh", File(workingDirectory))
        commandFile.writeText("#!/bin/bash\n$command")
        commandFile.deleteOnExit()
        val result = exec(arrayOf("sh", commandFile.absolutePath), workingDirectory, threadIdx)
        commandFile.delete()
        return result
    }

    fun supportGitAdd(workingDirectory: String, supportPath: String, threadIdx: Int? = null): ShellResult {
        return exec(arrayOf("sh", "./git_add.sh", "$workingDirectory"), supportPath, threadIdx)
    }



    /**
     * Create a directory. The directory must not yet exist.
     * @param directory The directory to create.
     * @param workingDirectory The working directory for the command. If null, the current working directory is used.
     * @return The result of the command, see [ShellResult].
     */
    fun mkdir(directory: String, workingDirectory: String?, threadIdx: Int? = null): ShellResult {
        return exec(arrayOf("mkdir", "-p", directory), workingDirectory, threadIdx)
    }

    /**
     * Remove a directory and all its contents.
     * @param directory The directory to remove.
     * @param workingDirectory The working directory for the command. If null, the current working directory is used.
     * @return The result of the command, see [ShellResult].
     */
    fun rmrf(directory: String, workingDirectory: String?, threadIdx: Int? = null): ShellResult {
        return exec(arrayOf("rm", "-rf", directory), workingDirectory, threadIdx)
    }

    fun rm(file: String, workingDirectory: String? = null, threadIdx: Int? = null): ShellResult {
        return exec(arrayOf("rm", "-f", file), workingDirectory, threadIdx)
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
    fun cp(source: String, target: String, workingDirectory: String?, threadIdx: Int? = null): ShellResult {
        val from = DirectoryHandler.ensureDirectoryPathEnding(source) + "."
        val to = DirectoryHandler.ensureNoDirectoryPathEnding(target)
        return exec(arrayOf("cp", "-r", from, to), workingDirectory, threadIdx)
    }



}
