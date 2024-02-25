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

import sys
import webbrowser
import os

from datetime import datetime

from lib.data.measured_environment import MeasuredEnvironment
from lib.data.config_file import ConfigFile
from lib.analysis.analysis import analyze_with_config, analyze_with_config_csv
from lib.analysis.plot import visualise_embeddings
from lib.webview.render.renderer import render_html


if __name__ == '__main__':

    argv = sys.argv[1:]

    config_path: str | None = None

    for index, arg in enumerate(argv):
        if arg == '-h':
            print('see https://github.com/KKegel/driftool for further information')
            sys.exit()
        elif arg in ["-c", "--config", "config="]:
            config_path = argv[index + 1]
            print("config: " + config_path)


    if config_path is None:
        print("Missing driftool input config!")
        sys.exit(2)

    config_file = open(config_path, "r")
    config = ConfigFile(config_file.read())
    config_file.close()


    if config.report_title is None:
        config.report_title = ""

    print("input_repository: " + str(config.input_repository))
    print("output_directory: " + str(config.output_directory))
    print("fetch_updates: " + str(config.fetch_updates))
    print("print_plot: " + str(config.print_plot))
    print("html: " + str(config.html))
    print("show_html: " + str(config.show_html))
    print("branch_ignore: " + str(config.branch_ignore))
    print("file_ignore: " + str(config.file_ignore))
    print("file_whitelist: " + str(config.file_whitelist))
    print("csv_file: " + str(config.csv_file))
    print("simple_export: " + str(config.simple_export))

    if config.input_repository is None:
        print("Missing requirement: input directory")
        sys.exit(2)
    

    measured_envrionment: MeasuredEnvironment = analyze_with_config(config.input_repository, config.fetch_updates, config.file_ignore, config.file_whitelist, config.branch_ignore)

    identifier = ("driftool_results_" + str(datetime.now())).replace(":", "_").replace(".", "_").replace(" ", "_")

    if config.output_directory is not None:
        output_file = config.output_directory + identifier + ".json"
        output = open(output_file, "x")
        output.write(measured_envrionment.serialize())
        output.close()

        if config.simple_export:
            output_file_simple = config.output_directory + "d_"+config.report_title + ".txt"
            output_simple = open(output_file_simple, "w")
            output_simple.write(str(measured_envrionment.sd))
            output_simple.close()


    if config.print_plot:
       visualise_embeddings(measured_envrionment)

    if config.html and config.output_directory:
        html_content = render_html(measured_envrionment, config.report_title, config.branch_ignore, config.file_ignore)
        html_file = config.output_directory + identifier + ".html"
        html_output = open(html_file, "x")
        html_output.write(html_content)
        html_output.close()

        if config.show_html:
            webbrowser.open_new_tab('file:///' + os.getcwd() + "/" + html_file)