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

import itertools
import numpy as np
from sklearn.manifold import MDS
import math

from lib.data.pairwise_distance import PairwiseDistance
from lib.analysis.repository_handler import RepositoryHandler
from lib.data.measured_environment import MeasuredEnvironment


def calculate_standard_deviation(embeddings: np.ndarray[float]) -> float:
    return math.sqrt(np.var(embeddings))


def calculate_distances(repository_handler: RepositoryHandler) -> list[tuple[str, str, PairwiseDistance]]:
    '''
    What should the metric / metrics be?

    sd  = statement drift = variance(conflicting_lines)
    fd  = file drift = variance(conflicting_files)
    bd  = branch drift = variance(conflicting_branches)
    ldd  = difference drift = variance(branch_diff_lines)
    '''
    branches = repository_handler.materialize_all_branches_in_reference()
    branch_product: list[tuple[str, str]] = itertools.product(branches, branches)
    branch_pairs: list[tuple[str, str]] = list()
    
    for pair in branch_product:
        if pair not in branch_pairs and (pair[1], pair[0]) not in branch_pairs and not pair[0] == pair[1]:
            branch_pairs.append(pair)
    
    print("Calculation distances for " + str(len(branch_pairs)) + " branch combinations...")

    distance_relation: list[tuple[str, str, PairwiseDistance]] = list()

    for branch in branches:
        distance_relation.append([branch, branch, PairwiseDistance()])

    progress = 0
    total = len(branch_pairs)
    for pair in branch_pairs:
        print(str(progress) + " out of " + str(total), end='\r')
        progress += 1
        repository_handler.create_working_tmp()
        distance = repository_handler.merge_and_count_conflicts(pair[0], pair[1])
        distance_relation.append([pair[0], pair[1], distance])
        distance_relation.append([pair[1], pair[0], distance])
        repository_handler.clear_working_tmp()

    return distance_relation


def construct_environment(distance_relation: list[tuple[str, str, PairwiseDistance]], branches: list[str]) -> MeasuredEnvironment:
    
    me = MeasuredEnvironment()
    me.branches = branches
    
    d = len(branches)
    me.line_matrix = np.zeros(shape=(d, d))
    me.conflict_matrix = np.zeros(shape=(d, d))
    me.file_matrix = np.zeros(shape=(d, d))
    me.branch_matrix = np.zeros(shape=(d, d))
    me.diff_matrix = np.zeros(shape=(d, d))

    for e in distance_relation:
        xi = branches.index(e[0])
        yi = branches.index(e[1])
        me.line_matrix[xi, yi] = e[2].conflicting_lines
        me.file_matrix[xi, yi] = e[2].conflicting_files
        me.diff_matrix[xi, yi] = e[2].diff_lines
        me.conflict_matrix[xi, yi] = e[2].conficts

        if e[2].conflicting_lines > 0:
            me.branch_matrix[xi, yi] = 1

    return me


def multidimensional_scaling(distance_matrix: np.ndarray[float], dimensions: int = 3) -> np.ndarray[float]:
    mds = MDS(dissimilarity='precomputed', random_state=0, n_components=dimensions, normalized_stress=False)
    embeddings = mds.fit_transform(distance_matrix) 
    return embeddings


def analyze_with_config(input_dir: str, fetch_updates: bool, 
                        ignore_files: list[str], ignore_branches: list[str]) -> MeasuredEnvironment:

    repository_handler: RepositoryHandler = RepositoryHandler(input_dir, fetch_updates, ignore_files, ignore_branches)
    repository_handler.create_reference_tmp()
    
    distance_relation = calculate_distances(repository_handler)

    repository_handler.clear_reference_tmp()

    environment = construct_environment(distance_relation, repository_handler.branches)

    environment.embedding_lines = multidimensional_scaling(environment.line_matrix, 3)
    environment.embedding_conflicts = multidimensional_scaling(environment.conflict_matrix, 3)
    environment.embedding_files = multidimensional_scaling(environment.file_matrix, 3)
    environment.embedding_branches = multidimensional_scaling(environment.branch_matrix, 3)
    environment.embedding_differences = multidimensional_scaling(environment.diff_matrix, 3)

    drift_lines = calculate_standard_deviation(environment.embedding_lines)
    drift_conflicts = calculate_standard_deviation(environment.embedding_conflicts)
    drift_files = calculate_standard_deviation(environment.embedding_files)
    drift_branches = calculate_standard_deviation(environment.embedding_branches)
    drift_diff = calculate_standard_deviation(environment.embedding_differences)

    environment.sd = drift_lines
    environment.cd = drift_conflicts
    environment.fd = drift_files
    environment.bd = drift_branches
    environment.dd = drift_diff

    print("statement drift (sd) = " + str(drift_lines))
    print("difference drift (dd) = " + str(drift_diff))

    return environment