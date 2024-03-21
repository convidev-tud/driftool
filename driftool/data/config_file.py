#  Copyright 2023 Karl Kegel
#
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#  http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.

import yaml

class ConfigFile:
    """
    Represents a configuration file for the Driftool application.

    Attributes:
        input_repository (str): The input repository to analyze.
        output_directory (str | None): The output directory to store the analysis results. Defaults to None.
        fetch_updates (bool): Flag indicating whether to fetch updates from the repository.
        print_plot (bool): Flag indicating whether to print the analysis plot.
        html (bool): Flag indicating whether to generate an HTML report.
        show_html (bool): Flag indicating whether to open the HTML report in a web browser.
        branch_ignore (list[str]): List of branches to ignore during analysis.
        file_ignore (list[str]): List of files to ignore during analysis.
        file_whitelist (list[str]): List of files to include during analysis.
        report_title (str | None): The title of the analysis report. Defaults to None.
        simple_export (bool): Flag indicating whether to perform a simple export of the analysis results.
        csv_file (str | None): The path to the CSV file to export the analysis results. Defaults to None.
        timeout (int | None): The timeout duration for the analysis. Defaults to None.

    Methods:
        __init__(config_yaml_string: str) -> None: Initializes a ConfigFile instance with the provided YAML string.
    """

    def __init__(self, config_yaml_string: str) -> None:
        """
        Initializes a ConfigFile instance with the provided YAML string.

        Args:
            config_yaml_string (str): The YAML string representing the configuration.

        Raises:
            ValueError: If the YAML string is invalid or missing required fields.
        """

        conf: dict = yaml.safe_load(config_yaml_string)
       
        if "output_directory" in conf:
            self.output_directory = conf["output_directory"]
        else:
            self.output_directory = None
        if "report_title" in conf:
            self.report_title = conf["report_title"]
        else:
            self.report_title = None
        if "csv_file" in conf:
            self.csv_file = conf["csv_file"]
        else:
            self.csv_file = None
        if "simple_export" in conf:
            self.simple_export = conf["simple_export"]
        else:
            self.simple_export = None
        if "timeout" in conf:
            self.timeout = int(conf["timeout"])
        else:
            self.timeout = None

        self.input_repository = conf["input_repository"]
        self.fetch_updates = conf["fetch_updates"]
        self.print_plot = conf["print_plot"]
        self.html = conf["html"]
        self.show_html = conf["show_html"]
        self.branch_ignore = conf["branch_ignore"]
        self.file_ignore = conf["blacklist"]
        self.file_whitelist = conf["whitelist"]
        
       
