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

import sys
import uuid

from driftool.analysis.analysis import calculate_partial_distance_relation
from driftool.analysis.repository_handler import RepositoryHandler

argv = sys.argv[1:]

encoded_pairs = argv[0]
reference_path = argv[1]

if encoded_pairs is None or encoded_pairs == "":
    sys.exit(1)

repository_handler = RepositoryHandler("", False, list(), list(), list(), 0)
repository_handler.set_bypass_arguments(reference_path)

print("Reading job from out/")
file = open(encoded_pairs, "r")
encoded_pairs_content = file.read()
pairs = encoded_pairs_content.split(":")
file.close()

branch_combinations = list()

for pair in pairs:
    decoding = pair.split("~")
    branch_combinations.append((decoding[0], decoding[1]))

partial_distances = calculate_partial_distance_relation(repository_handler, branch_combinations)

file_name = "./io/" + "in_" + str(uuid.uuid4()) + ".txt"
with open(file_name, "x") as file:
    lines = []
    for result in partial_distances:
        lines.append(result[0] + "~" + result[1] + "~" + str(result[2].conflicting_lines) + "\n")
    file.writelines(lines)
    print("reading from " + file_name)

sys.exit(0)