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

def async_execute(threads: list[list[str]]):
    std = asyncio.run(run_and_join(threads))
    #TODO parse std and get the actual results
    #TODO write async run script

async def run_and_join(threads: list[list[str]]):

    async def run(pairs: list[str]):
        '''
        print(f'[{cmd!r} exited with {proc.returncode}]')
        if stdout:
        print(f'[stdout]\n{stdout.decode()}')
        if stderr:
         print(f'[stderr]\n{stderr.decode()}')
        '''
        proc = await asyncio.create_subprocess_shell(
            cmd,
            stdout=asyncio.subprocess.PIPE, stderr=asyncio.subprocess.PIPE)

        stdout, stderr = await proc.communicate()
        return (stdout, stderr)
       

    return await asyncio.gather(*[run(t) for t in threads])
    

    