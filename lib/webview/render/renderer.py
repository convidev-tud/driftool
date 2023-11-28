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

def render_html(me: MeasuredEnvironment):
    mytemplate = Template(filename='resources/report.template.html')
    buf = StringIO()
    
    #TODO make accuracy an argument

    sd_embeddings = (me.embedding_lines * 10).astype(int)
    dd_embeddings = (me.embedding_differences / 10).astype(int)

    ctx = Context(buf, 
                  sd=str("%.2f" % me.sd), 
                  dd=str("%.2f" % me.dd), 
                  branch_array=me.branches, 
                  number_branches=len(me.branches),
                  sd_embeddings=json.dumps(sd_embeddings.tolist()),
                  dd_embeddings=json.dumps(dd_embeddings.tolist()),
                  full_json_dump=me.serialize())
    mytemplate.render_context(ctx)
    result = buf.getvalue()
    #print(result)
    return result