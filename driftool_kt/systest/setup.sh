#!/bin/bash

cd ..
cd ..
cd volume

# clone conflict-example repo if it does not exist
if [ ! -d "conflict-example" ]; then    
    echo "conflict-example does not exist -> CLONE"
    git clone https://github.com/convidev-tud/conflict-example.git
fi

# check if example_config.yaml exists
if [ ! -f "example_config.yaml" ]; then
    echo "example_config.yaml does not exist -> INIT"
    # create example_config.yaml and add the content to it
    echo $'jsonReport: true\nhtmlReport: true\nreportIdentifier: Example Report\ntimeoutDays: 0\nfileWhiteList: []\nfileBlackList: []\nignoreBranches: []'> example_config.yaml
fi

# check if example_config_blacklist.yaml exists
if [ ! -f "example_config_blacklist.yaml" ]; then
    echo "example_config_blacklist.yaml does not exist -> INIT"
    # create example_config.yaml and add the content to it
    echo $'jsonReport: true\nhtmlReport: true\nreportIdentifier: Example Report\ntimeoutDays: 0\nfileWhiteList: []\nfileBlackList: ["data\", "\\\\.sh"]\nignoreBranches: []'> example_config_blacklist.yaml
fi

if [ ! -f "example_config_whitelist.yaml" ]; then
    echo "example_config_whitelist.yaml does not exist -> INIT"
    # create example_config.yaml and add the content to it
    echo $'jsonReport: true\nhtmlReport: true\nreportIdentifier: Example Report\ntimeoutDays: 0\nfileWhiteList: ["\\\\.txt"]\nfileBlackList: []\nignoreBranches: []'> example_config_whitelist.yaml
fi

if [ ! -f "example_config_combined_list.yaml" ]; then
    echo "example_config_combined_list.yaml does not exist -> INIT"
    # create example_config.yaml and add the content to it
    echo $'jsonReport: true\nhtmlReport: true\nreportIdentifier: Example Report\ntimeoutDays: 0\nfileWhiteList: ["\\\\.txt", "folder"]\nfileBlackList: ["main"]\nignoreBranches: []'> example_config_combined_list.yaml
fi