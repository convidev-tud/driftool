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
from driftool.data.config_file import ConfigFile

class TestConfigReading(unittest.TestCase):
    
    def test_config_full(self):
        print("Testing full config file")
        config_path = "tests/resources/configs/config-full-all.yaml"
        config_content = open(config_path, "r").read()
        config = ConfigFile(config_content)
        self.assertEqual(config.input_repository, "foo/bar")
        self.assertEqual(config.output_directory, "volume/")
        self.assertEqual(config.fetch_updates, True)
        self.assertEqual(config.print_plot, True)
        self.assertEqual(config.html, True)
        self.assertEqual(config.show_html, True)
        self.assertEqual(config.report_title, "My Report")
        self.assertEqual(config.csv_file, "volume/file.csv")
        self.assertEqual(config.simple_export, True)
        self.assertEqual(config.timeout, 20)
        self.assertSequenceEqual(config.branch_ignore, ["a\\-", "abc"])
        self.assertSequenceEqual(config.file_ignore, ["^c*", "d"])
        self.assertSequenceEqual(config.file_whitelist, ["\\.foo", "\\.bar"])
        
        
    def test_config_minimal(self):
        print("Testing minimal config file")
        config_path = "tests/resources/configs/config-minimal.yaml"
        config_content = open(config_path, "r").read()
        config = ConfigFile(config_content)
        
        self.assertIsNone(config.output_directory)
        self.assertIsNone(config.timeout)
        self.assertIsNone(config.report_title)
        self.assertIsNone(config.csv_file)
        self.assertIsNone(config.simple_export)
        
        self.assertEqual(config.input_repository, "foo/bar")
        self.assertEqual(config.fetch_updates, True)
        self.assertEqual(config.print_plot, True)
        self.assertEqual(config.html, True)
        self.assertEqual(config.show_html, True)
        
        self.assertSequenceEqual(config.branch_ignore, [])
        self.assertSequenceEqual(config.file_ignore, [])
        self.assertSequenceEqual(config.file_whitelist, [])
    
    
    def test_config_full_empty(self):
        print("Testing full but empty config file")
        config_path = "tests/resources/configs/config-full-none.yaml"
        config_content = open(config_path, "r").read()
        config = ConfigFile(config_content)
        self.assertEqual(config.fetch_updates, False)
        self.assertEqual(config.print_plot, False)
        self.assertEqual(config.html, False)
        self.assertEqual(config.show_html, False)
        self.assertEqual(config.simple_export, False)
        self.assertEqual(config.timeout, 0)
        
        self.assertSequenceEqual(config.branch_ignore, [])
        self.assertSequenceEqual(config.file_ignore, [])
        self.assertSequenceEqual(config.file_whitelist, [])
        
        self.assertIsNone(config.input_repository)
        self.assertIsNone(config.csv_file) 
        self.assertIsNone(config.report_title)
        self.assertIsNone(config.output_directory)
        
        
if __name__ == '__main__':
    unittest.main()