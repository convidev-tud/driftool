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

from lib.data.pairwise_distance import PairwiseDistance, distance_avg
from lib.analysis.repository_handler import RepositoryHandler
from lib.data.measured_environment import MeasuredEnvironment
from lib.analysis.csv_data import read_branches_from_csv, read_distances_from_csv
from lib.data.sysconf import SysConf
from lib.analysis.async_exec import async_execute
from lib.data.config_file import ConfigFile

def calculate_median_distance_avg(embeddings: np.ndarray[float]) -> float:
    '''
    Input embeddings in the form [[x0, y0, z0], ..., [xi, yi, zi]]
    '''
    m = np.median(embeddings, axis=0)
    l = len(embeddings)
    d = 0

    for p in embeddings:
        d += math.sqrt((p[0] - m[0])**2 + (p[1] - m[1])**2 + (p[2] - m[2])**2)
    
    return d / l


def calculate_distances(repository_handler: RepositoryHandler) -> list[tuple[str, str, PairwiseDistance]]:
    '''
    Calculate the actual distance relation.
    The returned distance relation of type list[tuple[str, str, PairwiseDistance]] maps a pair
    of branches to their pairwise distance.
    Note: This function must be symmetrical. For example (A, B) -> 7 implies that (=>) (B, A) -> 7 as well.
    However, sometimes the actual calculation is not symmetrical because of exotic git behaviour.
    In this cases, the AVG of both directions is calculated and stored for both directions.

    sd  = statement drift = variance(conflicting_lines)
    '''
    branches = repository_handler.materialize_all_branches_in_reference()
    branch_product: list[tuple[str, str]] = itertools.product(branches, branches)
    branch_pairs: list[tuple[str, str]] = list()
    
    for pair in branch_product:
        if pair not in branch_pairs and not pair[0] == pair[1] and (pair[1], pair[0]) not in branch_pairs:
            branch_pairs.append(pair)
    
    print("Calculation distances for " + str(len(branch_pairs)*2) + " branch combinations...")

    distance_relation: list[tuple[str, str, PairwiseDistance]] = list()

    # Add self-distances (0 per definition)
    for branch in branches:
        distance_relation.append([branch, branch, PairwiseDistance()])

    progress = 0
    total = len(branch_pairs)
    repository_handler.create_working_tmp()
    
    for pair in branch_pairs:
        print(str(progress*2) + " out of " + str(total*2), end='\r')
     
        progress += 1
    
        distanceA = repository_handler.merge_and_count_conflicts(pair[0], pair[1])
        repository_handler.reset_working_tmp()
        distanceB = repository_handler.merge_and_count_conflicts(pair[1], pair[0])
        repository_handler.reset_working_tmp()
        
        distanceAVG = distance_avg(distanceA, distanceB)
        
        distance_relation.append([pair[0], pair[1], distanceAVG])
        distance_relation.append([pair[1], pair[0], distanceAVG])
        
    repository_handler.clear_working_tmp()
    repository_handler.clear_reference_tmp()

    return distance_relation


def calculate_partial_distance_relation(repository_handler: RepositoryHandler, 
                                        branch_combinations: list[tuple[str, str]]) -> list[tuple[str, str, PairwiseDistance]]:
    '''
    Calculate the a partial distance relation.
    The returned distance relation of type list[tuple[str, str, PairwiseDistance]] maps a pair
    of branches to their pairwise distance.
    Note: This function must be symmetrical. For example (A, B) -> 7 implies that (=>) (B, A) -> 7 as well.
    However, sometimes the actual calculation is not symmetrical because of exotic git behaviour.
    In this cases, the AVG of both directions is calculated and stored for both directions.

    This function is used in mutlithreading usecases where the branch combinations are pre-calculated.

    sd  = statement drift = variance(conflicting_lines)
    '''

    distance_relation: list[tuple[str, str, PairwiseDistance]] = list()

    repository_handler.create_working_tmp()
    
    for pair in branch_combinations:
     
        distanceA = repository_handler.merge_and_count_conflicts(pair[0], pair[1])
        repository_handler.reset_working_tmp()
        distanceB = repository_handler.merge_and_count_conflicts(pair[1], pair[0])
        repository_handler.reset_working_tmp()
        
        distanceAVG = distance_avg(distanceA, distanceB)
        
        distance_relation.append([pair[0], pair[1], distanceAVG])
        distance_relation.append([pair[1], pair[0], distanceAVG])
        
    repository_handler.clear_working_tmp()

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

    for e in distance_relation:
        xi = branches.index(e[0])
        yi = branches.index(e[1])
        me.line_matrix[xi, yi] = e[2].conflicting_lines

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


def analyze_with_config(config: ConfigFile, sysconf: SysConf) -> MeasuredEnvironment:
    '''
    The computation main method of the driftool application.
    Orchestrates the drift calculation step by step.
    1. Read and prepare the repository
    2. Calculate the distance relation
    3. Transfrom the relation into an Environment with distance matrices
    4. Calculate the median average -> the actual drift metric
    '''

    timeout = 0 if config.timeout is None else config.timeout
    repository_handler: RepositoryHandler = RepositoryHandler(config.input_repository, config.fetch_updates, config.file_ignore, config.file_whitelist, config.branch_ignore, timeout)
    repository_handler.create_reference_tmp()
    
    number_threads = sysconf.number_threads
    
    if number_threads < 2:
        distance_relation = calculate_distances(repository_handler)
    else:
        branches = repository_handler.materialize_all_branches_in_reference()
        # get all pairs
        # the chars '~' and ':' are forbidden in git branch names, so we can use them as seperators
        threads = list()
        for i in range(0, number_threads, 1):
            threads.append(list())
        
        thread_idx = 0
        for b1 in branches:
            for b2 in branches:
                # Only add one-direction combinations and no identity combinations
                if b1 == b2:
                    break
                threads[thread_idx].append(b1 + "~" + b2)
                thread_idx += 1
                thread_idx %= number_threads
                
        # start the threads and wait until all results are delivered
        distance_relation = async_execute(threads, repository_handler._reference_tmp_path)
        for branch in branches:
            distance_relation.append([branch, branch, PairwiseDistance()])
        repository_handler.clear_reference_tmp()
    
    environment = construct_environment(distance_relation, repository_handler.branches)
    environment.embedding_lines = multidimensional_scaling(environment.line_matrix, 3)

    environment.sd = calculate_median_distance_avg(environment.embedding_lines)
    print("statement drift (sd) = " + str(environment.sd))

    return environment


def analyze_with_config_csv(csv_input_file) -> MeasuredEnvironment:

    branches = read_branches_from_csv(csv_input_file)               
    distance_relation = read_distances_from_csv(csv_input_file)
    environment = construct_environment(distance_relation, branches)
    environment.embedding_lines = multidimensional_scaling(environment.line_matrix, 3)

    environment.sd = calculate_median_distance_avg(environment.embedding_lines)
    print("statement drift (sd) = " + str(environment.sd))

    return environment