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

import json

class ConfigFile:

    input_repository: str
    output_directory: str | None = None
    fetch_updates: bool
    print_plot: bool
    html: bool
    show_html: bool
    branch_ignore: list[str]
    file_ignore: list[str]
    open_socket: str | None = None

    def __init__(self, config_json_string: str) -> None:

        conf = json.loads(config_json_string)

        if "output_directory" in conf:
            self.output_directory = conf["output_directory"]
        if "open_socket" in conf:
            self.open_socket = conf["open_socket"]

        self.input_repository = conf["input_repository"]
        self.fetch_updates = conf["fetch_updates"]
        self.print_plot = conf["print_plot"]
        self.html = conf["html"]
        self.show_html = conf["show_html"]
        self.branch_ignore = conf["branch_ignore"]
        self.file_ignore = conf["file_ignore"]
       
