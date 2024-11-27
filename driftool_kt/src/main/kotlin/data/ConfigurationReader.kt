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

package io.driftool.data

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.io.File

/**
 * Reads the configuration file and validates the configuration.
 * The configuration file must be in YAML format.
 * This class works for all configuration files supported by the parse methods.
 * @param configPath path to the configuration file
 */
class ConfigurationReader(private val configPath: String) {

    /**
     * Parses the configuration file and returns the configuration as a GitModeConfiguration object.
     * Asserts that all required fields are set and not out of bounds.
     * @return GitModeConfiguration object
     */
    fun parseGitModeConfig(): GitModeConfigurationFile {
        val mapper = ObjectMapper(YAMLFactory()).registerKotlinModule()
        val text = File(configPath).readText()
        val configuration = mapper.readValue(text, GitModeConfigurationFile::class.java)

        assert(configuration.jsonReport || configuration.htmlReport) { "At least one report type must be set" }
        assert(configuration.timeoutDays >= 0) { "timeoutDays must be greater equal 0" }

        return configuration
    }



}