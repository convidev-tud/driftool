#  Copyright 2024 Karl Kegel
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

from data.pairwise_distance import PairwiseDistance
import csv 

def read_branches_from_csv(csv_input_file: str) -> list[str]:
    with open(csv_input_file) as file:
        csv_reader = csv.reader(file, delimiter=';')
        line_count = 0
        for row in csv_reader:
            if line_count == 0:
                return row


def read_distances_from_csv(csv_input_file: str) -> list[tuple[str, str, PairwiseDistance]]:
    
    branches = read_branches_from_csv(csv_input_file)
    relation: list[tuple[str, str, PairwiseDistance]] = list()
    
    with open(csv_input_file) as file:
        csv_reader = csv.reader(file, delimiter=';')
        
        line_count = 0
        row_index = 0
        
        for row in csv_reader:
            if line_count > 0:
                
                col_index = 0
                for col in row:
                    distance = PairwiseDistance()
                    distance.conflicting_lines = int(col)
                    entry = (branches[row_index], branches[col_index], distance)
                    relation.append(entry)

                    col_index += 1
                row_index += 1

            line_count += 1

    return relation