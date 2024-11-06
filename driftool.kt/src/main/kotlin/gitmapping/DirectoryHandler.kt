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

package io.driftool.gitmapping

import java.util.UUID
import java.util.concurrent.ConcurrentLinkedQueue

class DirectoryHandler(val rootLocation: String) {

    val temporalDirectories: ConcurrentLinkedQueue<String> = ConcurrentLinkedQueue()

    fun createTemporalDirectory(): String {
        val directory = "$rootLocation/${getUniqueName()}"

        return ""
    }

    fun deleteAllTemporalDirectories() {
        temporalDirectories.forEach { deleteDirectory(it) }
    }

    fun deleteDirectory(directory: String) {
        //TODO
        unregisterTemporalDirectory(directory)
    }

    fun registerTemporalDirectory(directory: String) {
        temporalDirectories.add(directory)
    }

    fun unregisterTemporalDirectory(directory: String) {
        temporalDirectories.remove(directory)
    }


    companion object {

        fun getUniqueName(): String {
            return UUID.randomUUID().toString()
        }

    }

}