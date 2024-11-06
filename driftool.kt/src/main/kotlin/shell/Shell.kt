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

object Shell {

    private fun exec(command: Array<String>): ShellResult {
        val cmd = Runtime.getRuntime().exec(command)
        val inputStream = cmd.inputStream
        val errorStream = cmd.errorStream
        val result = cmd.waitFor()
        val output = inputStream.bufferedReader().readText()
        val error = errorStream.bufferedReader().readText()
        return ShellResult(result, output, error)
    }

    fun mkdir(directory: String): ShellResult {
        return exec(arrayOf("mkdir", directory))
    }

}