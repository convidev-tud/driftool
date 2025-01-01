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

import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random
import kotlin.time.TimeSource

object Log {

    private val log: MutableMap<String, String> = ConcurrentHashMap<String, String>()
    private val asyncLogs: MutableMap<Int, MutableMap<String, String>> = ConcurrentHashMap<Int, MutableMap<String, String>>()

    private var print: Boolean = true

    fun setPrint(value: Boolean){
        print = value
    }

    fun append(elem: String){
        if (print) println("LOG <<< $elem")
        val currentTimeStamp = Instant.now().toString()
        val randomPart = Random.nextInt().toString()
        log.put(currentTimeStamp + "_" + randomPart, elem)
    }

    fun appendAsync(threadIdx: Int?, elem: String){
        if (threadIdx == null) {
            append(elem)
            return
        }
        var map = asyncLogs[threadIdx]
        if (map == null){
            map = ConcurrentHashMap<String, String>()
            asyncLogs.put(threadIdx, map)
        }
        val currentTimeStamp = Instant.now().toString()
        val randomPart = Random.nextInt().toString()
        map.put(currentTimeStamp + "_" + randomPart, "[$threadIdx] $elem")
    }

    fun mergeAsyncLogs(){
        asyncLogs.forEach { (logKey, value) ->
            val sortedEntries = getSorted(value)
            for (entry in sortedEntries){
                if (print) println("LOG <<< ${entry.second}")
                log.put(entry.first, entry.second)
            }
        }
        asyncLogs.clear()
    }

    fun print(){
        log.forEach { (key, value) -> println("$key: $value") }
    }

    private fun getSorted(logMap: Map<String, String>): List<Pair<String, String>> {
        return logMap.toList().sortedBy { it ->
            it.first.split("_")[0]
        }
    }

    fun listValuePairs(): List<Pair<String, String>> = getSorted(log).map { it -> Pair(it.first.split("_")[0], it.second) }

    fun listValues(): List<String> = getSorted(log).map { it.second }



}