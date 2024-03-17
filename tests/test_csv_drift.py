import unittest
import numpy as np
from driftool.analysis.analysis import analyze_with_config_csv
import unittest
import numpy as np
from driftool.analysis.analysis import analyze_with_config_csv

class TestCSVDrift(unittest.TestCase):
    
    def test_analyze_with_config_csv_two_elem(self):
        csv_file = "tests/resources/csv/input_example_two_elem.csv"
        me = analyze_with_config_csv(csv_file)
        self.assertEqual(me.branches, ['A', 'B'])
        self.assertEqual(me.sd, 0.5)
        
    def test_analyze_with_config_csv_three_elem(self):
        csv_file = "tests/resources/csv/input_example_valid.csv"
        me = analyze_with_config_csv(csv_file)
        self.assertSequenceEqual(me.branches, ['A', 'B', 'C'])
        p_a = me.embedding_lines[0]
        p_b = me.embedding_lines[1]
        p_c = me.embedding_lines[2]
        l_a = [p_a[0], p_b[0], p_c[0]]
        l_a.sort()
        median_a = l_a[1]
        l_b = [p_a[1], p_b[1], p_c[1]]
        l_b.sort()
        median_b = l_b[1]
        l_c = [p_a[2], p_b[2], p_c[2]]
        l_c.sort()
        median_c = l_c[1]
        median = [median_a, median_b, median_c]
        median_np = np.median(me.embedding_lines, axis=0).tolist()
        print(median)
        print(median_np)
        print(me.embedding_lines)
        d_median_pa = np.linalg.norm(p_a - median)
        d_median_pb = np.linalg.norm(p_b - median)
        d_median_pc = np.linalg.norm(p_c - median)
        avg = (d_median_pa + d_median_pb + d_median_pc) / 3
        print("avg: ", avg)
        self.assertEqual(me.sd, avg)
        
    @unittest.expectedFailure
    def test_analyze_with_config_csv_invalid_asym(self):
        print("Testing analyze_with_config_csv with invalid asym csv file")
        csv_file = "tests/resources/csv/input_example_asym.csv"
        me = analyze_with_config_csv(csv_file)
        self.assertSequenceEqual(me.branches, ['A', 'B', 'C'])
    
    # The MDS algorithm ignores non idempotent nodes
    def test_analyse_with_config_csv_invalid_non_idempotent(self):
        print("Testing analyze_with_config_csv with invalid non idempotent csv file")
        csv_file = "tests/resources/csv/input_example_non_idempotent.csv"
        me = analyze_with_config_csv(csv_file)
        self.assertSequenceEqual(me.branches, ['A', 'B', 'C'])
        self.assertEqual(me.sd, 0)
    
    def test_analyse_with_config_csv_zeros(self):
        print("Testing analyze_with_config_csv with only zeros")
        csv_file = "tests/resources/csv/input_example_zeros.csv"
        me = analyze_with_config_csv(csv_file)
        self.assertSequenceEqual(me.branches, ['A', 'B', 'C'])
        self.assertEqual(me.sd, 0)
        
    def test_analyse_with_config_csv_one_element(self):
        print("Testing analyze_with_config_csv with only one element")
        csv_file = "tests/resources/csv/input_example_one_elem.csv"
        me = analyze_with_config_csv(csv_file)
        self.assertSequenceEqual(me.branches, ['A'])
        self.assertEqual(me.sd, 0)
        
if __name__ == '__main__':
    unittest.main()
