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