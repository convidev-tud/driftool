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

from lib.data.pairwise_distance import PairwiseDistance, distanve_avg
from lib.analysis.repository_handler import RepositoryHandler
from lib.data.measured_environment import MeasuredEnvironment


def calculate_standard_deviation(embeddings: np.ndarray[float]) -> float:
    '''
    Input embeddings in the form [[x0, y0, z0], ..., [xi, yi, zi]]
    '''
    m = embeddings.mean(axis=0)
    l = len(embeddings)
    omega_squared = 0

    for p in embeddings:
        omega_squared += (((m[0] - p[0]) ** 2) + ((m[1] - p[1]) ** 2) + ((m[2] - p[2]) ** 2))
    
    omega_squared = omega_squared / l
    omega = math.sqrt(omega_squared)
    
    return omega


def calculate_distances(repository_handler: RepositoryHandler) -> list[tuple[str, str, PairwiseDistance]]:
    '''
    Calculate the actual distance relation.
    The returned distance relation of type list[tuple[str, str, PairwiseDistance]] maps a pair
    of branches to their pairwise distance.
    Note: This function must be symmetrical. For example (A, B) -> 7 implies that (=>) (B, A) -> 7 as well.
    However, sometimes the actual calculation is not symmetrical because of exotic git behaviour.
    In this cases, the AVG of both directions is calculated and stored for both directions.

    sd  = statement drift = variance(conflicting_lines)
    dd  = difference drift = variance(diff_lines)
    '''
    branches = repository_handler.materialize_all_branches_in_reference()
    branch_product: list[tuple[str, str]] = itertools.product(branches, branches)
    branch_pairs: list[tuple[str, str]] = list()
    
    for pair in branch_product:
        if pair not in branch_pairs and not pair[0] == pair[1] and (pair[1], pair[0]) not in branch_pairs:
            branch_pairs.append(pair)
    
    print("Calculation distances for " + str(len(branch_pairs)*2) + " branch combinations...")

    distance_relation: list[tuple[str, str, PairwiseDistance]] = list()

    for branch in branches:
        distance_relation.append([branch, branch, PairwiseDistance()])

    progress = 0
    total = len(branch_pairs)
    repository_handler.create_working_tmp()
    
    for pair in branch_pairs:
        print(str(progress*2) + " out of " + str(total*2), end='\r')
        #print("merge " + pair[1] + " into " + pair[0])
        progress += 1
        
        distanceA = repository_handler.merge_and_count_conflicts(pair[0], pair[1])
        distanceB = repository_handler.merge_and_count_conflicts(pair[0], pair[1])
        distanceAVG = distanve_avg(distanceA, distanceB)
        #print("----> " + str(distance.conflicting_lines))
        distance_relation.append([pair[0], pair[1], distanceAVG])
        distance_relation.append([pair[1], pair[0], distanceAVG])
        repository_handler.reset_working_tmp()
        
    repository_handler.clear_working_tmp()
    repository_handler.clear_reference_tmp()

    return distance_relation


def construct_environment(distance_relation: list[tuple[str, str, PairwiseDistance]], branches: list[str]) -> MeasuredEnvironment:
    '''
    Construct the MeasuredEnvironment from the distance relation.
    This basically transforms the distance relation into a distance matrix.
    '''

    me = MeasuredEnvironment()
    me.branches = branches
    
    d = len(branches)
    me.line_matrix = np.zeros(shape=(d, d))
    me.diff_matrix = np.zeros(shape=(d, d))

    for e in distance_relation:
        xi = branches.index(e[0])
        yi = branches.index(e[1])
        me.line_matrix[xi, yi] = e[2].conflicting_lines
        me.diff_matrix[xi, yi] = e[2].diff_lines

    return me


def multidimensional_scaling(distance_matrix: np.ndarray[float], dimensions: int = 3) -> np.ndarray[float]:
    '''
    Executes the MDS algorithm to reduce the distance matrix to a 3D point-cloud mathcing the distances as close
    as possible.
    https://en.wikipedia.org/wiki/Multidimensional_scaling

    Although this function supports different dimensionality, in the context of this software, exactly 3 dimensions should be used.
    '''
    mds = MDS(dissimilarity='precomputed', random_state=0, n_components=dimensions, normalized_stress=False)
    embeddings = mds.fit_transform(distance_matrix) 
    return embeddings


def analyze_with_config(input_dir: str, fetch_updates: bool, 
                        ignore_files: list[str], whitelist_files: list[str], ignore_branches: list[str]) -> MeasuredEnvironment:
    '''
    The secret main method of the driftool application.
    Orchestrates the drift calculation step by step.
    1. Read and prepare the repository
    2. Calculate the distance relation
    3. Transfrom the relation into an Environment with distance matrices
    4. Calculate the standard deviations -> the actual dirft metric
    '''

    repository_handler: RepositoryHandler = RepositoryHandler(input_dir, fetch_updates, ignore_files, whitelist_files, ignore_branches)
    repository_handler.create_reference_tmp()
    
    distance_relation = calculate_distances(repository_handler)

    #repository_handler.clear_reference_tmp()

    environment = construct_environment(distance_relation, repository_handler.branches)

    environment.embedding_lines = multidimensional_scaling(environment.line_matrix, 3)
    environment.embedding_differences = multidimensional_scaling(environment.diff_matrix, 3)

    drift_lines = calculate_standard_deviation(environment.embedding_lines)
    drift_diff = calculate_standard_deviation(environment.embedding_differences)

    environment.sd = drift_lines
    environment.dd = drift_diff

    print("statement drift (sd) = " + str(drift_lines))
    print("difference drift (dd) = " + str(drift_diff))

    return environment