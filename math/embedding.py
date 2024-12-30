'''
 * Copyright 2024 Karl Kegel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
'''
from sklearn.manifold import MDS
import numpy as np
import sys
import json

def multidimensional_scaling(distance_matrix: np.ndarray[float], dimensions: int = 3) -> np.ndarray[float]:
    '''
    Executes the MDS algorithm to reduce the distance matrix to a 3D point-cloud mathcing the distances as close
    as possible.
    https://en.wikipedia.org/wiki/Multidimensional_scaling
    '''
    mds = MDS(dissimilarity='precomputed', random_state=0, n_components=dimensions, normalized_stress=False)
    embeddings = mds.fit_transform(distance_matrix) 
    return embeddings


'''
"0": [0, 1, 2],
"1": [1, 0, 1],
"2": [2, 1, 0]
-->
1;2.3;3
2;3.5;7
6;0;0.5
'''

def main():
    first_argument = sys.argv[1]
    second_argument = sys.argv[2]
    third_argument = sys.argv[3]
    input_json_file = open(first_argument, 'r')
    json_dict = json.load(input_json_file)
    input_json_file.close()
    number_branches = int(second_argument)
    
    distance_matrix = np.zeros((number_branches, number_branches))
    
    for index in range(number_branches):
        line_values = json_dict[str(index)]
        for i in range(number_branches):
            distance_matrix[index][i] = line_values[i]
            
    embeddings = multidimensional_scaling(distance_matrix, 3)
    print(embeddings)
    
    output_json_file = open(third_argument, 'w')
    for point in embeddings:
        print(str(point[0]) + ';' + str(point[1]) + ';' + str(point[2]))
        output_json_file.write(str(point[0]) + ';' + str(point[1]) + ';' + str(point[2]) + '\n')
    output_json_file.close()
    
    sys.exit(0)


main()