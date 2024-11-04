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

import unittest
import numpy as np
import math
from driftool.analysis.analysis import calculate_median_distance_avg, construct_environment

class TestAnalysis(unittest.TestCase):

    def test_calculate_median_distance_avg(self):
        print("Testing calculate_median_distance_avg with 3 embeddings")
        embeddings = np.array([[1, 2, 3], [4, 5, 6], [7, 8, 9]])
        m1 = math.sqrt((1 - 4)**2 + (2 - 5)**2 + (3 - 6)**2)
        m2 = math.sqrt((4 - 4)**2 + (5 - 5)**2 + (6 - 6)**2)
        m3 = math.sqrt((7 - 4)**2 + (8 - 5)**2 + (9 - 6)**2)
        median_avg = (m1 + m2 + m3) / 3
        result = calculate_median_distance_avg(embeddings)
        self.assertAlmostEqual(result, median_avg)

    
    def test_calculate_median_distance_avg_4_embeddings(self):
        print("Testing calculate_median_distance_avg with 4 embeddings")
        embeddings_4 = np.array([[1, 2, 3], [4, 5, 6], [7, 8, 9], [10, 11, 12]])
        median_4 = [5.5, 6.5, 7.5]
        m4 = math.sqrt((1 - median_4[0])**2 + (2 - median_4[1])**2 + (3 - median_4[2])**2)
        m5 = math.sqrt((4 - median_4[0])**2 + (5 - median_4[1])**2 + (6 - median_4[2])**2)
        m6 = math.sqrt((7 - median_4[0])**2 + (8 - median_4[1])**2 + (9 - median_4[2])**2)
        m7 = math.sqrt((10 - median_4[0])**2 + (11 - median_4[1])**2 + (12 - median_4[2])**2)
        median_avg_4 = (m4 + m5 + m6 + m7) / 4
        result_4 = calculate_median_distance_avg(embeddings_4)
        self.assertAlmostEqual(result_4, median_avg_4)


    def test_construct_environment(self):
        print("Testing construct_environment")
        p1 = 3
        p2 = 2
        distance_relation = [('A', 'B', p1), ('B', 'C', p2)]
        branches = ['A', 'B', 'C']
        expected_line_matrix = np.array([[0, 3, 0], [0, 0, 2], [0, 0, 0]])

        result = construct_environment(distance_relation, branches)

        self.assertEqual(result.branches, branches)
        np.testing.assert_array_equal(result.line_matrix, expected_line_matrix)

if __name__ == '__main__':
    unittest.main()