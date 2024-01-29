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

class PairwiseDistance:
    '''
    The PairwiseDistance class groups all distance measures taken from a the repository.
    '''

    def __init__(self) -> None:

        self.conflicting_lines: int = 0
        self.diff_lines: int = 0


def distance_avg(one: PairwiseDistance, other: PairwiseDistance):
    res = PairwiseDistance()
    res.conflicting_lines = (one.conflicting_lines + other.conflicting_lines) * 0.5
    #res.diff_lines = (one.diff_lines + other.diff_lines) * 0.5
    return res
    
    