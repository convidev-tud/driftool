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

from matplotlib import pyplot as plt
from driftool.data.measured_environment import MeasuredEnvironment

def visualise_embeddings(me: MeasuredEnvironment):
    '''
    Uses matplotlib to create a (debug) visualization of the drift.
    '''

    fig = plt.figure()

    x_sd = list(map(lambda t: t[0], me.embedding_lines))
    y_sd = list(map(lambda t: t[1], me.embedding_lines))
    z_sd = list(map(lambda t: t[2], me.embedding_lines))

    axsd = fig.add_subplot(1, 1, 1 , projection='3d')

    axsd.scatter(x_sd, y_sd, z_sd)
    axsd.set_title('drift = ' + str("%.2f" % me.sd))
    
    plt.show()
