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
    '''
    A helper data class for report generation.

    Attributes:
        peer_branch (str): The name of the peer branch.
        sd (str): The distance between the branches as a string.

    Methods:
        __init__(peer_branch: str, sd: float) -> None: Initializes a new instance of the BranchDistance class.
        serialize() -> str: Serializes the BranchDistance object to a JSON string.
    '''

    peer_branch: str = None
    sd: str = None

    def __init__(self, peer_branch: str, sd: float) -> None:
        '''
        Initializes a new instance of the BranchDistance class.

        Args:
            peer_branch (str): The name of the peer branch.
            sd (float): The distance between the branches as a float.
        '''
        self.peer_branch = peer_branch
        self.sd = sd

    def serialize(self) -> str:
        '''
        Serializes the BranchDistance object to a JSON string.

        Returns:
            str: The serialized JSON string representation of the BranchDistance object.
        '''
        return json.dumps({"peer_branch": self.peer_branch, "sd": self.sd})
      

    def serialize(self) -> str:
        '''
        Serializes the BranchDistance object to a JSON string.

        Returns:
            str: The JSON string representation of the BranchDistance object.
        '''
        obj = {
            "peer_branch": self.peer_branch,
            "sd": self.sd,
            }
        return json.dumps(obj, indent=4)


import json

class BranchEnvironment:
    '''
    Helper data class for report generation.
    
    This class represents the environment of a branch, including the branch name and a list of distances.
    '''

    branch: str = None
    distances: list[BranchDistance] = None

    def __init__(self, branch: str, distances: list[BranchDistance]) -> None:
        '''
        Initializes a new instance of the BranchEnvironment class.

        Args:
            branch (str): The name of the branch.
            distances (list[BranchDistance]): A list of BranchDistance objects representing the distances.

        Returns:
            None
        '''
        self.branch = branch
        self.distances = distances

    def serialize(self) -> str:
        '''
        Serializes the BranchEnvironment object to a JSON string.

        Returns:
            str: The JSON string representation of the BranchEnvironment object.
        '''
        obj = {
            "branch": self.branch,
            "distances": list(map(lambda p: p.serialize(), self.distances))
            }
        return json.dumps(obj, indent=4)