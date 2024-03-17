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

import asyncio

from driftool.data.pairwise_distance import PairwiseDistance


def async_execute(threads: list[list[str]], reference_dir: str) -> list[tuple[str, str, PairwiseDistance]]:
    
    std = asyncio.run(run_and_join(threads, reference_dir))
    distance_relation = list()
    
    print("All threads returned their results!")
    
    for stdout, stderr in std:
        if stderr:
            print(stderr.decode())
        if stdout:
            #print(stdout.decode())
            out: str = stdout.decode().split("\n")
            
            for line in out:
                if not "~" in line:
                    continue
                combination = line.split("~")
                distance = PairwiseDistance()
                distance.conflicting_lines = float(combination[2])
                distance_relation.append((combination[0], combination[1], distance))
            
    return distance_relation


async def run(combinations: str, reference_dir: str):
        print("Async thread started, please wait...")
        proc = await asyncio.create_subprocess_shell(
            "python driftool/thread.py " + combinations + " " + reference_dir,
            stdout=asyncio.subprocess.PIPE, stderr=asyncio.subprocess.PIPE)

        stdout, stderr = await proc.communicate()
        return (stdout, stderr)


async def run_and_join(threads: list[list[str]], reference_dir):
    arguments = list()
    for thread in threads:
        combinations = ""
        for idx, pair in enumerate(thread):
            if idx < len(thread)-1:
                combinations += (pair + ":")
            else:
                combinations += pair
        arguments.append(combinations)
         
    return await asyncio.gather(*[run(arg, reference_dir) for arg in arguments])
    

    