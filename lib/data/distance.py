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

class BranchDistance:

    peer_branch: str = None
    sd: str = None
    dd: str = None

    def __init__(self, peer_branch: str, sd: float, dd: float) -> None:
        self.peer_branch = peer_branch
        self.sd = str("%.2f" % sd)
        self.dd = str("%.2f" % dd)

    def serialize(self) -> str:
        obj = {
            "peer_branch": self.peer_branch,
            "sd": self.sd,
            "dd": self.dd,
            }
        return json.dumps(obj, indent=4)



class BranchEnvironment:

    branch: str = None
    distances: list[BranchDistance] = None

    def __init__(self, branch: str, distances: list[BranchDistance]) -> None:
        self.branch = branch
        self.distances = distances

    def serialize(self) -> str:
        obj = {
            "branch": self.branch,
            "distances": list(map(lambda p: p.serialize(), self.distances))
            }
        return json.dumps(obj, indent=4)