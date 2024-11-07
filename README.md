# driftool :: Git Branch Inconsistency Analysis


> This repository is part of a research project of the [*Software Technology Group*](https://tu-dresden.de/ing/informatik/smt/st?set_language=en) at *Dresden University of Technology*. Contact Karl Kegel (KKegel) for further information. 

The driftool calculates the drift analysis for git repositories. 
It automatically compares all branches, simulates merges and generates both a scalar drift metric, as well as a 3D view of the repository drift.
The pairwise distance is the number of lines of merge conflicts.
The base measure of the point-cloud is the *mean absolute deviation around a central point*.

The results of the drift computation indicate how well or poorly a repository is managed. 
High drift indicates large inconsistencies in between branches. 
The drift is an absolute metric that always has to be interpreted in the context of the repository size. 
A repository with dozens of collaborateurs and branches naturally has more drift as a project with 3 developers working on. 
However, the evolution of the drift over time gives useful insigts about the project health.

> :zap: Running the driftool without reading this README till the end may cause severe issues.

## Key Metrics

The driftool calculates the *Drift* measure **g** (gamma).
Depending on the calculation strategy, different *Drift Flavours* are possible.

* *Statement Drift* := a measure for the merge complexity based on the git merge conflict line count
* *Conflict Drift* := a measure for the merge complexity based on the git merge conflict occurence count

In general, higher numbers indicate a more complex repository management.

:bulb: Per default, a git merge is a symmetric operation, meaning A into B produces the same conflicts as B into A. However, through certain git operations (resets, force operations, messed-up history) happens that the merge is not symmetric. For example, A into B has 2 conflicing lines, although B into A has 4 conflicting lines. As this is extremely rare, we ignore this issue for performance reasons (2x speedup).

## Report Generation

> TODO


# Getting Started

#### Terms 

There are some important terms to keep in mind while reading the following instructions.

The **driftool repository** is this repository you are currently in (or after cloning it onto your system). The **root** of the dirftool repository is the place this README.md is located.

The **repository under analysis** is the repository you want to analyze with the driftool in git mode.

The **volume** or **input volume** is a folder called *volume* that contains the input data used by the driftool. This folder is mounted automatically by the driftool docker container.

The **volume path** is the absolute path to the volume folder on your local system. If you use the default setup, it is the absolute path to the volume folder of the **driftool repository**.

The **volume root** is the space when you enter the **volume**. If, for example, there is a file ``.../volume/configs/config.yaml`` then its **path within the volume** is ``configs/config.yaml``.

## Docker Setup

> The driftool runs as a docker container application. We recommend the docker setup for all user. It is - in theory - possible to run the driftool without docker on a linux system directly. If you wish to do so, please read the code documentation.

### Quick Setup

1. Have docker (or docker desktop) installed and ready.
2. Clone this repository.
3. Make ``run.sh`` and ``build.sh`` executable via ``chmod``
4. Execute ``./build.sh`` with sudo priviledges in the root directory of the driftool repository.
5. Move the repository under analysis and the repository config into the ``./volume`` directory of the driftool repository
6. Execute 
```./run.sh [repo path (in volume)]  [output path (in volume)] [config path (in volume)] [RAM alloc] [threads]``` 
with sudo priviledges.
7. The analysis results are written to the ``./volume/[output path]``

:zap: **Note that the analysis of a large repository (branch-number) can take from few seconds up to several hours! It grows quadratically with the number of branches. For example if 10 branches of your repository take 100 seconds, 30 branches would take 900 seconds.**

> For advanced users: you can exectue the run.sh wherever you want as long as the driftool image is available in your docker image list. In case of running the driftool outside the cloned repository, there must be a ``volume``  folder in the location where the driftool (through the run.sh) is executed.

### Build & Run Scripts

* ``build.sh`` is the buildscript of the docker image. Execute ``./build.sh`` to generate the docker image. If you want to configure the image creation, e.g., name, tag or location, you can modify the build command defined in the file. **You must execute this command only in the source root directory of this repository!**
* ``run.sh`` is the entry script to start the driftool container from the image. Beforehand, the ``build.sh`` must be executed once. If the ``build.sh`` was executed without modifications, the ``run.sh`` works out of the box. Otherwise, the script must be modified accordingly. The run script takes thre positional parameters (have a look into the run.sh file or the quick setup instructions above)  Output reports are placed in the ``./volume`` directory. The container is destroyed after each run.
Example: ``./run.sh volume/repository.yaml 64 12``: Analyzed the configured repository on 12 threads using max. 64 GB of RAM. **You must execute this command only in the source root directory of this repository!** (If you know what you are doing, you may execute the run script in any location where a matching volume directory is located as well.)
* (``dockerfile``) is the standard entry point for ``docker build`` and describes the composition of the docker image. If you are a driftool user, you do not have to execute or touch this file.
* (``deb_run.sh``) is the entrypoint script of the created container. It prepares the ramdisk and starts the driftool's main method. If you are a driftool user, you do not have execute or touch this file.


### Environment Settings

#### Number of Threads

You can configure the number of threads used to parallelize the analysis. The analysis gets faster the more threads are used, particularly for large repositories. If the configured number of threads is larger as your system can physically provide, the analysis slows down. **Take care of an appropriate RAM size**.

#### RAM Size

You can specify the amount of RAM to be used by the container as the second argument of the ``run.sh``. Note that this is not the actual RAM size. The docker container will at least use the amount of RAM it actually needs to run, even if the paramter is set to 0 (which is not recommended because it will lead to a runtime crash).

The configured additional RAM size must at least be the configured as: the number of threads times two multiplied with the repository size in GB. If your host system has less RAM, the number of threads must be reduced.

Alternatively, you can disable the additional RAM usage (which slows down the analysis at least by half). To do so, open the ``deb_run.sh`` and remove the lines marked by comments from the file and execute the build script again. You still need to provide the second argument while executing ``run.sh`` but it has no impact. 
However, if the container runs in the "no ramdisk" mode, the required space will be allocated on the default hard drive.

### Configuration

Starting the driftool requires a repository-specific config file. All arguments are mandatory.

### Config Parameters

TODO

```YAML
input_repository: STRING
output_directory: STRING
fetch_updates: BOOLEAN
report_title: STRING
print_plot: BOOLEAN
html: BOOLEAN
show_html: BOOLEAN
simple_export: BOOLEAN
csv_file: STRING
timeout: 90
branch_ignore:
    - STRING
blacklist: 
    - STRING
whitelist:
    - STRING
```

### Config Examples

The following example is provided as ``config.template.yaml`` as part of this repository.

```YAML
input_repository: /Users/.../my-repository
output_directory: ../
fetch_updates: false
report_title: My Report
print_plot: false
html: true
show_html: true
simple_export: false
branch_ignore:
    - "^release\\-"
    - "^v\\."
blacklist: 
    - "build\\/"
    - "dist\\/"
    - "gen\\/"
    - "\\.min\\.js"
    - "\\.lib\\.js"
    - "node\\-modules\\/"
    - "\\.pdf"
    - "javadoc\\/"
    - "\\.png"
whitelist: []
```

**Whitelist Example**

The following example only analyses Java files and HTML templates adn ignores all other files.

```YAML
whitelist:
    - "\\.java"
    - "\\.template\\.html"
```

## Matrix Only Mode

You can provide a pre-caluclated distance matrix for calculating drift value and drift plot.

The CSV file must look like this. The top horizontal row denotes the branch names. The same branch ordering applies to the vertical dimension. The body contains the pairwise distances. The matrix must be symmetric.

```CSV
A;B;C
0;1;7
1;0;1
7;1;0
```


# In-Depth Understanding of the Drift Metric and Driftool Plot

## Drift Metric

The driftool calculates the *Drift* measure **sd** (gamma).

* **sd** is the *Statement Drift* := a measure for the merge complexity based on the pair-wise git merge conflict count as a distance

In general, higher numbers indicate a more complex repository management. The most interesting value is the change of the drift metrics over time. Increases the drift rapidely, severe conflicts may have been introduced recently.

Using the above distance measures, the unit of drift is *Lines of Code (LoC)*. But we recommend to use drift as a unit-less measure and interpret its tendencies only.

### Calculation Details

1. We calculate the pairwise distances using a distance metric.
2. The pairwise distances are used to perform a multidimensional scaling of the set of branches to a 3D pointcloud.
3. We calculate the *mean absolute deviation around a central point* (mean of distances from the median) of the pointlcoud and their euclidean distances..

For better understanding and transparency, we use provide the core computation:
```python
def calculate_median_distance_avg(embeddings: np.ndarray[float]) -> float:
    '''
    Input embeddings in the form [[x0, y0, z0], ..., [xi, yi, zi]]
    '''
    m = np.median(embeddings, axis=0)
    l = len(embeddings)
    d = 0

    for p in embeddings:
        d += math.sqrt((p[0] - m[0])**2 + (p[1] - m[1])**2 + (p[2] - m[2])**2)
    
    return d / l
```

## Scatter Plot

As explained above in *Calculation Details*, we perform a multidimensional scaling (MDS) into 3D space.
Each point in the scatterplot visualizes one branch (the latest commit / HEAD of the branch)

The initial dimensionality of the distance (consistency) problem is unknown. 
The unit of the distance is LoC of merge conflicts. 
The MDS tries to retain the pairwise distances while embedding the points in the target dimensions. 
The embedding error is called stress. The MDS algorithm tries to optimize for minimal stress.

**Consequences**

* Distances between points visualize the amount of merge conflicts (inconsistencies).
* The dimensions of the plot have no relieable unit, they are result of the MDS embedding.
* Point embeddings are not exact but approximated!
* Two points with a different locations do not neccessarily have a pair-wise difference, but different differences to a third point.

💡 **We recommend the scatter plot to get a visual and explainable overview of the project. Outlier detection is possible. However, the main indicator is the evolution of the drift metric over time and not size and strucutre of the scatter plot.**

---

> https://en.wikipedia.org/wiki/Average_absolute_deviation

> https://en.wikipedia.org/wiki/Multidimensional_scaling
