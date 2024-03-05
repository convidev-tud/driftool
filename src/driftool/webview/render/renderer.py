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

from mako.template import Template
from mako.runtime import Context
from io import StringIO
import json

from lib.data.measured_environment import MeasuredEnvironment
from lib.data.distance import BranchDistance, BranchEnvironment


def generate_branch_distance_map(me: MeasuredEnvironment) -> list[BranchEnvironment]:
    '''
    Transform the distance matrices of the MeasuredEnvironment into an easy to interpret relation.
    This is required to keep the template logic as small as possible.
    '''
    
    res: list[BranchEnvironment] = list()

    for base in range(len(me.branches)):
        
        base_branch = me.branches[base]
        distances: list[BranchDistance] = list()

        for peer in range(len(me.branches)):
            peer_branch = me.branches[peer]
            sd = me.line_matrix[base, peer]
            distances.append(BranchDistance(peer_branch, sd))

        res.append(BranchEnvironment(base_branch, distances))

    return res



def render_html(me: MeasuredEnvironment, report_title: str, branch_ignore: list[str], file_ignore: list[str]):
    '''
    Generate the result HTML report using the mako template engine.
    The prepared html file is located in the resources directory. 
    This template file contains placeholders for the data values.
    Data objects are preprocessed and passed to the tmplate engine
    '''
    mytemplate = Template(filename='resources/report.template.html')
    buf = StringIO()

    sd_embeddings = (me.embedding_lines * 10).astype(int)
    branch_wise_distances = generate_branch_distance_map(me)

    ctx = Context(buf,
                  title=report_title,
                  sd=str("%.2f" % me.sd), 
                  branch_ignore=branch_ignore,
                  file_ignore=file_ignore,
                  branch_array=me.branches, 
                  branch_array_json=json.dumps(me.branches),
                  number_branches=len(me.branches),
                  sd_embeddings=json.dumps(sd_embeddings.tolist()),
                  branch_distances=branch_wise_distances,
                  full_json_dump=me.serialize())
    mytemplate.render_context(ctx)
    result = buf.getvalue()

    return result