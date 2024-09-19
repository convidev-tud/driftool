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
import subprocess
from driftool.data.config_file import ConfigFile
from driftool.main import exec
from driftool.data.sysconf import SysConf
from driftool.analysis.analysis import analyze_with_config, analyze_with_config_csv
    
class TestAnalysisIntegration(unittest.TestCase):
    
    def setUp(self):
        out1 = subprocess.run(["tests/resources/repositories/repo_a_setup.sh"], capture_output=True).stdout
    
    def tearDown(self):
        out1 = subprocess.run(["tests/resources/repositories/repo_a_destruct.sh"], capture_output=True).stdout

    def test_calculate_drift_no_file_manipulation(self):
        print("Testing calculate_drift with no file manipulation")
        config_path = "tests/resources/repositories/repo_a_config.yaml"
        config_content = open(config_path, "r").read()
        config = ConfigFile(config_content)
        sysconf = SysConf("number_threads: 1")
        me_repo = analyze_with_config(config, sysconf)
    
        csv_file = "tests/resources/repositories/repo_a_distances.csv"
        me_csv = analyze_with_config_csv(csv_file)
        
        print("drift: " + str(me_repo.sd))
        self.assertCountEqual(me_repo.branches, ['main', 'additive_feature', 'conflicting_feature_a', 'conflicting_feature_b'])
        self.assertAlmostEqual(me_repo.sd, me_csv.sd)
        
        
    def test_calculate_drift_blacklist(self):
        print("Testing calculate_drift with blacklist")
        config_path = "tests/resources/repositories/repo_a_config_blacklist.yaml"
        config_content = open(config_path, "r").read()
        config = ConfigFile(config_content)
        sysconf = SysConf("number_threads: 1")
        
        me_repo = analyze_with_config(config, sysconf)
        
        csv_file = "tests/resources/repositories/repo_a_distances_blacklist.csv"
        me_csv = analyze_with_config_csv(csv_file)
        
        print("drift: " + str(me_repo.sd))
        self.assertCountEqual(me_repo.branches, ['main', 'additive_feature', 'conflicting_feature_a', 'conflicting_feature_b'])
        self.assertAlmostEqual(me_repo.sd, me_csv.sd)
        
        
    def test_calculate_drift_branch_ignore(self):
        print("Testing calculate_drift with branch ignore")
        config_path = "tests/resources/repositories/repo_a_config_branch_ignore.yaml"
        config_content = open(config_path, "r").read()
        config = ConfigFile(config_content)
        sysconf = SysConf("number_threads: 1")
        me_repo = analyze_with_config(config, sysconf)
        print("drift: " + str(me_repo.sd))
        self.assertCountEqual(me_repo.branches, ['main', 'additive_feature', 'conflicting_feature_a'])
        self.assertAlmostEqual(me_repo.sd, 0)
    
    
    def test_calculate_drift_whitelist(self):
        print("Testing calculate_drift with whitelist")
        config_path = "tests/resources/repositories/repo_a_config_whitelist.yaml"
        config_content = open(config_path, "r").read()
        config = ConfigFile(config_content)
        sysconf = SysConf("number_threads: 1")
        
        me_repo = analyze_with_config(config, sysconf)
        
        csv_file = "tests/resources/repositories/repo_a_distances_whitelist.csv"
        me_csv = analyze_with_config_csv(csv_file)
        
        print("drift: " + str(me_repo.sd))
        self.assertCountEqual(me_repo.branches, ['main', 'additive_feature', 'conflicting_feature_a', 'conflicting_feature_b'])
        self.assertAlmostEqual(me_repo.sd, me_csv.sd)
    
    

    def test_full_integration_json_report(self):
        print("Testing full integration with json report")
        config_path = "tests/resources/repositories/repo_a_config.yaml"
        config = ConfigFile(open(config_path, "r").read())
        print("config repo path: " + config.input_repository)
        exec(["tests/resources/repositories/repo_a_config.yaml"])
        
        out_folder = config.output_directory
        folder_content = subprocess.run(["ls", out_folder], capture_output=True).stdout
        lines = folder_content.split()
        print("folder content: " + str(lines))
        self.assertEqual(len(lines), 3)
        

    def test_full_integration_simple_report(self):
        print("Testing full integration with simple report")
        config_path = "tests/resources/repositories/repo_a_config_simple.yaml"
        config = ConfigFile(open(config_path, "r").read())
        print("config repo path: " + config.input_repository)
        exec(["tests/resources/repositories/repo_a_config_simple.yaml"])
        
        out_folder = config.output_directory
        result_file = out_folder + "d_" + config.report_title + ".txt"
        result_content = open(result_file, "r").read()
        self.assertAlmostEqual(float(result_content), 1.75)

    
    
if __name__ == '__main__':
    unittest.main()