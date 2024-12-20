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
import uuid

from driftool.data.pairwise_distance import PairwiseDistance


def async_execute(threads: list[list[str]], reference_dirs: list[str], log: list[str]) -> list[tuple[str, str, PairwiseDistance]]:
    
    log.append(">>> Starting async_execute")
    total_elements = sum(len(thread) for thread in threads)
    std = asyncio.run(run_and_join(threads, reference_dirs))
    assert len(std) == len(threads)
    print("All threads returned their results!")
    
    distance_relation = list()
    
    
    results_without_error = list()
    for stdout, stderr in std:
        if not stderr:
            print(stdout.decode())
            log.append(str(stdout.decode()))
            results_without_error.append(stdout.decode().strip())
        else:
            print(str(stderr))
            log.append(str(stderr.decode()))

    print("SUCCESSFUL THREADS: " + str(results_without_error))
    log.append("SUCCESSFUL THREADS: " + str(results_without_error))

    for stdout, stderr in std:
        if stderr:
            print(stderr.decode())
            log.append(stderr.decode())
            print("Aborting thread result analysis")
            raise Exception(stderr.decode())
        if stdout:
            
            print("STDOUT: ", stdout.decode())
            results_file = stdout.decode().split("\n")[0]
            
            file = open(results_file, "r")
            out: list[str] = file.read().split("\n")
            print("Reading results from in/")
            log.append("Reading results from in/")
            log.extend(out)
            for line in out:
                if line == "---LOG":
                    break
                if not "~" in line:
                    continue
                combination = line.split("~")
                distance = PairwiseDistance()
                distance.conflicting_lines = float(combination[2])
                distance_relation.append((combination[0], combination[1], distance))
            file.close()
    
    if not len(results_without_error) == len(threads):
        log.append("Not all Threads returned successful!")
        print("Aborting thread result analysis")
        raise Exception("Not all Threads returned successful!")
    if not len(distance_relation) == total_elements*2:
        log.append("Not all combinations were calculated!")
        print("Aborting thread result analysis")
        raise Exception("Not all combinations were calculated!")
    
    log.append(">>> Finished async_execute")
    return distance_relation


async def run(combinations: str, reference_dir: str) -> tuple[bytes, bytes]:
        print("Async thread started, please wait...")
        print("RUN >>> python driftool/thread.py " + combinations + " " + reference_dir)
        proc = await asyncio.create_subprocess_shell(
            "python driftool/thread.py " + combinations + " " + reference_dir,
            stdout=asyncio.subprocess.PIPE, stderr=asyncio.subprocess.PIPE)

        stdout, stderr = await proc.communicate()
        return (stdout, stderr)


async def run_and_join(threads: list[list[str]], reference_dirs: list[str]) -> list[tuple[bytes, bytes]]:
    arguments: list[tuple[str]] = list()

    print("Found tasks for " + str(len(threads)) + " threads")
    print("Writing tasks to out/")

    tidx = 0
    for thread in threads:
        combinations = ""
        for idx, pair in enumerate(thread):
            if idx < len(thread)-1:
                combinations += (pair + "\n")
            else:
                combinations += pair
        
        file_name = "./io/" + "out_" + str(tidx) + "_" + str(uuid.uuid4()) + ".txt"
        file = open(file_name, "x")
        file.write(combinations)
        file.close()
        arguments.append((file_name, reference_dirs[tidx]))
        tidx += 1
         
         
    async with asyncio.TaskGroup() as tg:
        tasks = [tg.create_task(run(arg[0], arg[1])) for arg in arguments]
    
    results = [task.result() for task in tasks]
         
    return results
    

    