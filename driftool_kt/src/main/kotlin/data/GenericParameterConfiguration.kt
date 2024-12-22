package io.driftool.data

data class GenericParameterConfiguration(val inputRootPath: String,
                                         val absoluteWorkingPath: String,
                                         val absoluteInputRepositoryPath: String,
                                         val absoluteConfigPath: String,
                                         val absoluteReportPath: String,
                                         val threads: Int,
                                         val mode: String) {

    override fun toString(): String {
        return "GenericParameterConfiguration(inputRootPath='$inputRootPath', absoluteWorkingPath='$absoluteWorkingPath', " +
                "absoluteInputRepositoryPath='$absoluteInputRepositoryPath', absoluteConfigPath='$absoluteConfigPath', " +
                "absoluteReportPath='$absoluteReportPath', threads=$threads, mode='$mode')"
    }
}