package io.driftool.shell

class ShellResult(
    val exitCode: Int,
    val output: String,
    val error: String
) {
    fun isSuccessful(): Boolean {
        return exitCode == 0
    }
}