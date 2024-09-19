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
        self.embedding_lines = np.ndarray[float]
        self.sd = 0


    def serialize(self) -> str:
        matrix: list[list[float]] = self.line_matrix.tolist()
        embedding: list[list[float]] = self.embedding_lines.tolist()
        obj = {
            "sd": self.sd,
            "branches": self.branches,
            "line_matrix": matrix,
            "3d_embedding_lines": embedding
            }
        return json.dumps(obj, indent=4)