package io.driftool.data

data class GenericParameterConfiguration(val inputRootPath: String,
                                         val absoluteWorkingPath: String,
                                         val absoluteInputRepositoryPath: String,
                                         val absoluteConfigPath: String,
                                         val absoluteReportPath: String,
                                         val threads: Int,
                                         val mode: String) {

}