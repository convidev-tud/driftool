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

import getopt
import sys
import webbrowser
import os

from datetime import datetime

from lib.data.measured_environment import MeasuredEnvironment
from lib.data.config_file import ConfigFile
from lib.analysis.analysis import analyze_with_config
from lib.analysis.plot import visualise_embeddings
from lib.webview.render.renderer import render_html


if __name__ == '__main__':

    argv = sys.argv[1:]

    config_path: str | None = None

    input_dir: str = ""
    output_dir: str | None = None
    fetch_updates: bool = False
    print_plot: bool = False
    generate_html: bool = False
    show_html: bool = False
    ignore_files: list[str] = list()
    ignore_branches: list[str] = list()
    open_socket: str | None = None
    report_title: str | None = None

    try:
        opts, args = getopt.getopt(argv, "h:c:i:o:f:p:t:b:g:s:x:r", 
                                   ["config=", "input_repository=", "output_directory=", "fetch_updates=", 
                                    "print_plot", "html", "branch_ignore=", "file_ignore", "show_html", "open_socket=", "report_title="])
    except getopt.GetoptError:
        print('see https://github.com/KKegel/driftool for further information')
        sys.exit(2)
    for opt, arg in opts:
        if opt == '-h':
            print('see https://github.com/KKegel/driftool for further information')
            sys.exit()
        elif opt in ("-c", "--config"):
            config_path = arg
            break
            # Ingore other arguments as soon as a config is specified (they will be overwritten nonetheless)
        elif opt in ("-i", "--input_repository"):
            input_dir = arg
        elif opt in ("-o", "--output_directory"):
           output_dir = arg
        elif opt in ("-f", "--fetch_updates"):
            fetch_updates = arg == "true"
        elif opt in ("-p", "--print_plot"):
            print_plot = True
        elif opt in ("-t", "--html"):
            generate_html = True
        elif opt in ("-s", "--show_html"):
            show_html = True
        elif opt in ("-b", "--branch_ignore"):
            values = arg.split("::")
            for val in values:
                ignore_branches.append(val)
        elif opt in ("-g", "--file_ignore"):
            values = arg.split("::")
            for val in values:
                ignore_files.append(val)
        elif opt in ("-x", "--open_socket"):
            open_socket = True
            print("Socket mode not supported yet. Proceeding without socket connection!")
        elif opt in ("-r", "--report_title"):
            report_title = arg

    if config_path is not None:
        config_file = open(config_path, "r")
        config = ConfigFile(config_file.read())
        config_file.close()
        
        input_dir = config.input_repository
        output_dir = config.output_directory
        fetch_updates = config.fetch_updates
        print_plot = config.print_plot
        generate_html = config.html
        show_html = config.show_html
        ignore_branches = config.branch_ignore
        ignore_files = config.file_ignore
        open_socket = config.open_socket
        report_title = config.report_title


    if report_title is None:
        report_title = ""

    print("input_dir: " + str(input_dir))
    print("output_dir: " + str(output_dir))
    print("fetch_updates: " + str(fetch_updates))
    print("print_plot: " + str(print_plot))
    print("generate_html: " + str(generate_html))
    print("show_html: " + str(show_html))
    print("ignore_branches: " + str(ignore_branches))
    print("ignore_files: " + str(ignore_files))
    print("open_socket: " + str(open_socket))

    if input_dir is None:
        print("Missing requirement: input directory")
        sys.exit(2)

    measured_envrionment: MeasuredEnvironment = analyze_with_config(input_dir, fetch_updates, ignore_files, ignore_branches)

    identifier = ("driftool_results_" + str(datetime.now())).replace(":", "_").replace(".", "_").replace(" ", "_")

    if output_dir is not None:
        output_file = output_dir + identifier + ".json"
        output = open(output_file, "x")
        output.write(measured_envrionment.serialize())
        output.close()

    if generate_html and output_dir:
        html_content = render_html(measured_envrionment, report_title, ignore_branches, ignore_files)
        html_file = output_dir + identifier + ".html"
        html_output = open(html_file, "x")
        html_output.write(html_content)
        html_output.close()

        if show_html:
            webbrowser.open_new_tab('file:///' + os.getcwd() + "/" + html_file)
        
    if print_plot:
       visualise_embeddings(measured_envrionment)