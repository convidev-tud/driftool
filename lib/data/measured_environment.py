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

import numpy as np
import json

class MeasuredEnvironment:

    def __init__(self) -> None:

        self.branches: list[str] = list()
        #TODO save latest commits to environment

        self.line_matrix: np.ndarray[float]
        self.diff_matrix: np.ndarray[float]

        self.embedding_lines = np.ndarray[float]
        self.embedding_differences = np.ndarray[float]

        self.sd = 0
        self.dd = 0


    def serialize(self) -> str:
        obj = {
            "sd": self.sd,
            "dd": self.dd,
            "branches": self.branches,
            "line_matrix": self.line_matrix.tolist(),
            "diff_matrix": self.diff_matrix.tolist(),
            "3d_embedding_lines": self.embedding_lines.tolist(),
            "3d_embedding_differences": self.embedding_differences.tolist()
            }
        return json.dumps(obj, indent=4)