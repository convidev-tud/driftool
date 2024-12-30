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

import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random
import kotlin.time.TimeSource

object Log {

    private val log: Map<String, String> = ConcurrentHashMap<String, String>()

    private var print: Boolean = true

    fun setPrint(value: Boolean){
        print = value
    }

    fun append(elem: String){
        if (print) println("LOG <<< $elem")
        val currentTimeStamp = TimeSource.Monotonic.markNow().toString()
        val randomPart = Random.nextInt().toString()
        log.plus(Pair(currentTimeStamp + "_" + randomPart, elem))
    }

    fun print(){
        log.forEach { (key, value) -> println("$key: $value") }
    }

    private fun getSorted(): List<Pair<String, String>> = log.toList().sortedBy { it -> it.first.split("_")[0] }

    fun listValuePairs(): List<Pair<String, String>> = getSorted().map { it -> Pair(it.first.split("_")[0], it.second) }

    fun listValues(): List<String> = getSorted().map { it.second }

}