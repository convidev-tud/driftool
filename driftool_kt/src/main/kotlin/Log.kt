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
    private var asyncPrinting: Boolean = true

    fun setPrint(value: Boolean){
        print = value
    }

    fun setAsyncPrinting(value: Boolean){
        asyncPrinting = value
    }

    fun append(elem: String){
        if (print) println("LOG <<< $elem")
        val currentTimeStamp = Instant.now().toEpochMilli().toString()
        val randomPart = Random.nextInt().toString()
        log.put(currentTimeStamp + "_" + randomPart, elem)
    }

    fun appendAsync(threadIdx: Int?, elem: String){
        if (threadIdx == null) {
            append(elem)
            return
        }
        if (asyncPrinting){
            println("LOG [$threadIdx] <<< $elem")
        }
        var map = asyncLogs[threadIdx]
        if (map == null){
            map = ConcurrentHashMap<String, String>()
            asyncLogs.put(threadIdx, map)
        }
        val currentTimeStamp = Instant.now().toEpochMilli().toString()
        val randomPart = Random.nextInt().toString()
        map.put(currentTimeStamp + "_" + randomPart + "_" + threadIdx.toString(), "[$threadIdx] $elem")
    }

    fun print(){
        log.forEach { (key, value) -> println("$key: $value") }
    }

    private fun getSorted(): List<Pair<String, String>> {
        val sortedLog: MutableList<Pair<String, String>> = mutableListOf<Pair<String, String>>()
        sortedLog.addAll(log.toList().sortedBy { it ->
            it.first.split("_")[0]
        })
        for((_, map) in asyncLogs){
            sortedLog.addAll(map.toList().sortedBy { it ->
                it.first.split("_")[0]
            })
        }
        return sortedLog
    }

    fun listValuePairs(): List<Pair<String, String>> = getSorted().map { it -> Pair(it.first.split("_")[0], it.second) }

    fun listValues(): List<String> = getSorted().map { it.second }



}