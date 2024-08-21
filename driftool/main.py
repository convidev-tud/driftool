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
import time
import subprocess

from datetime import datetime

from driftool.data.measured_environment import MeasuredEnvironment
from driftool.data.config_file import ConfigFile
from driftool.data.sysconf import SysConf
from driftool.analysis.analysis import analyze_with_config, analyze_with_config_csv
from driftool.analysis.plot import visualise_embeddings
from driftool.webview.render.renderer import render_html


def exec(argv):
    
    start_time = time.time()

    config_path: str = argv[0]
    sysconf_path: str = "./driftool.yaml"

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
    print("timeout: " + str(config.timeout))

    if config.input_repository is None:
        print("Missing requirement: input directory")
        sys.exit(2)
    
    # Prefer the driftool.yaml located in the volume dir to configure easliy on build docker images
    if os.path.isfile("./volume/driftool.yaml"):
        sysconf_path = "./volume/driftool.yaml"
        
    sysconf_file = open(sysconf_path, "r")
    sysconf = SysConf(sysconf_file.read())
    sysconf_file.close()

    print("Starting drift calculation on " + str(sysconf.number_threads) + " threads...")

    measured_envrionment: MeasuredEnvironment = MeasuredEnvironment()
    analysis_log: list[str] = list()
    analysis_log.append(">>>>>>>> LOGSTART")
    
    if config.csv_file is None:
        try:
            measured_envrionment = analyze_with_config(config, sysconf, analysis_log)
        except:
            print("An unexpected error occurred during the analysis.")
            print("Please check the log for more information.")
            analysis_log.append("-------- Force writing log to file due to unexpected termination.")
            logfile = open(os.path.join(config.output_directory, "log.txt"), "w")
            for line in analysis_log:
                if not line.endswith("\n"):
                    line += "\n"
                logfile.write(line)
            logfile.close()
            analysis_log = None
            sys.exit(2)
    else:
        if len(config.branch_ignore) > 0 or len(config.file_ignore) > 0 or len(config.file_whitelist) > 0 or config.fetch_updates:
            print("INVALID CONFIGURATION. CSV IMPORT FORBIDS REPOSITORY OPERATIONS.")
            sys.exit(2)
        measured_envrionment = analyze_with_config_csv(config.csv_file)

    identifier = ("driftool_results_" + str(datetime.now())).replace(":", "_").replace(".", "_").replace(" ", "_")
    if config.anonymous:
        identifier = "report"

    if config.output_directory is not None:
        
        output_file = config.output_directory + identifier + ".json"
        subprocess.run(["mkdir", "-p", config.output_directory], capture_output=True)
        output = open(output_file, "x")
        output.write(measured_envrionment.serialize())
        output.close()
        
        if analysis_log is not None:
            logfile = open(os.path.join(config.output_directory, "log.txt"), "w")
            print("Writing log to file...")
            for line in analysis_log:
                if not line.endswith("\n"):
                    line += "\n"
                logfile.write(line)
            logfile.close()

        if config.simple_export:
            output_file_simple = config.output_directory + "d_"+config.report_title + ".txt"
            output_simple = open(output_file_simple, "w")
            output_simple.write(str(measured_envrionment.sd))
            output_simple.close()

    end_time = time.time()
    print("Duration (s) = " + str(end_time - start_time))

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


if __name__ == '__main__':
    exec(argv = sys.argv[1:])