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

import io.driftool.gitmapping.DirectoryHandler

object DataProvider {

    private var directoryHandler: DirectoryHandler? = null
    private var workingDirectory: String? = null

    fun initDirectoryHandler(rootLocation: String) {
        directoryHandler = DirectoryHandler(rootLocation)
    }

    fun setWorkingDirectory(workingDirectory: String) {
        this.workingDirectory = workingDirectory
    }

    fun getDirectoryHandler(): DirectoryHandler {
        assert(directoryHandler != null) { "DirectoryHandler not initialized" }
        return directoryHandler!!
    }

    fun getWorkingDirectory(): String {
        assert(workingDirectory != null) { "Working directory not set" }
        return workingDirectory!!
    }
}