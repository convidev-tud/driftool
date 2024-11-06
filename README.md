# driftool :: Git Branch Inconsistency Analysis


> This repository is part of a research project of the [*Software Technology Group*](https://tu-dresden.de/ing/informatik/smt/st?set_language=en) at *Dresden University of Technology*. Contact Karl Kegel (KKegel) for further information. 

The driftool calculates the drift analysis for git repositories. 
It automatically compares all branches, simulates merges and generates both a scalar drift metric, as well as a 3D view of the repository drift.
The pairwise distance is the number of lines of merge conflicts.
The base measure of the point-cloud is the *mean absolute deviation around a central point*.

The results of driftool indicate how well or poorly a repository is managed. 
High drift indicates large inconsistencies in between branches. 
The drift is an absolute metric that always has to be interpreted in the context of the repository size. 
A repository with dozens of collaborateurs and branches naturally has more drift as a project with 3 developers working on. 
However, the evolution of the drift over time gives useful insigts about the project health.

> :zap: Running the driftool without reading this README till the end may cause severe issues.

## Key Metrics

The driftool calculates the *Drift* measure **sd** (gamma).

* **sd** is the *Statement Drift* := a measure for the merge complexity based on the pair-wise git merge conflict count as a distance

In general, higher numbers indicate a more complex repository management.

:bulb: Per default, a git merge is a symmetric operation, meaning A into B produces the same conflicts as B into A. However, through certain git operations (resets, force operations, messed-up history) it happens that the merge is not symmatric. For example, A into B has 2 conflicing lines, although B into A has 4 conflicting lines. To cope with that, the driftool performs each merge both ways and takes the AVG of conflicts.

## Report Generation

The driftool generates HTML reports for further human analysis. 
The following figure shows such an analysis. 
The right side list displays all analysed branches. 


The scatterplot uses a synthetic coordinate system resulting from multidimensional scaling of the pairwise distances. 
However, point distribution is helpful for repository analysis. 
Evenly scattered points indicate many unrelated but conflicting changes. 
Clusters indicate groups of very compatible branches. 
Outliers indicate standalone branches with lots of inconsistencies compared to the majority.

# Getting Started

To get started with the driftool, please read the [configuration instructions](./doc/Configuration.md) first.

You can run the driftool in a local setup or via docker. If your primary goal is to run a driftool analysis on your repository, we recommend the [driftool docker setup](./doc/DockerSetup.md). With that, you only need docker on your system and no additional dependencies. Notably, running driftool via our docker image might lead to significantly faster execution times compared to a local environment.

If you want to contribute to the driftool or do not want a docker installation, you can start the driftool directly on your system by following the [driftool developer setup](./doc/DeveloperSetup.md)

---

## Docker Setup

### TLDR / Quick Setup

:zap: These are the quick setup steps, for a more spohisticated configuration, continue reading the following sections. This does only work on Unix/Linux systems.

1. Have docker (or docker desktop) installed and ready
2. Clone this repository
3. Make ``run.sh`` and ``build.sh`` executable via ``chmod``
4. Execute ``./build.sh`` with sudo priviledges in the root directory of this repository
5. Move the repository to analyze and the repository config into the ``./volume`` directory of this repository (the volume folder will be mounted by the docker container).
6. Execute ``./run.sh volume/y.yaml x y`` with sudo priviledges where y is your config name, x is the number of GB RAM and y is the number of cores to run on.
7. The analysis results are written to ``./volume``

:zap: **Note that the analysis of a large repository (branch-number) can take from few seconds up to several hours! It grows quadratically with the number of branches. For example if 10 branches of your repository take 100 seconds, 30 branches would take 900 seconds.**

### Build & Run Scripts

* ``build.sh`` is the buildscript of the docker image. Execute ``./build.sh`` to generate the docker image. If you want to configure the image creation, e.g., name, tag or location, you can modify the build command defined in the file. **You must execute this command only in the source root directory of this repository!**
* ``run.sh`` is the entry script to start the driftool container from the image. Beforehand, the ``build.sh`` must be executed once. If the ``build.sh`` was executed without modifications, the ``run.sh`` works out of the box. Otherwise, the script must be modified accordingly. The run script takes thre positional parameters (have a look into the run.sh file or the quick setup instructions above)  Output reports are placed in the ``./volume`` directory. The container is destroyed after each run.
Example: ``./run.sh volume/repository.yaml 64 12``: Analyzed the configured repository on 12 threads using max. 64 GB of RAM. **You must execute this command only in the source root directory of this repository!** (If you know what you are doing, you may execute the run script in any location where a matching volume directory is located as well.)
* (``dockerfile``) is the standard entry point for ``docker build`` and describes the composition of the docker image. If you are a driftool user, you do not have to execute or touch this file.
* (``deb_run.sh``) is the entrypoint script of the created container. It prepares the ramdisk and starts the driftool's main method. If you are a driftool user, you do not have execute or touch this file.

### Repository and Config Location

The driftool repository contains a special folder called ``volume``. 
This folder is dynamically mounted to the docker container during its boot.
In consequence, You and the docker container can use this folder to exchange data.

**The volume directory must not contain data that must not be lost or is mission ciritical. Always clone or move a clean copy of the repository under analysis into the volume. Apart from repo and config, no other file structures should be located within the volume.**

> For advanced users: you can exectue the run.sh wherever you want as long as the driftool image is available in your docker image list. In case of running the driftool outside the cloned repository, there must be a ``volume``  folder in the location where the driftool (through the run.sh) is executed.

1. The ``your-repository.yaml`` file containins your repository configuration, for example input path, title, blacklist etc. This file must be located in the ``./volume`` folder. While executing ``run.sh``, this configuration is the first positional parameter and must be provided in the form ``volume/your-repository.yaml``.
The config must define the output path as ``volume/``. Otherwise, you have no access to the driftool reports.
1. The repository to analyze must be located in the ``./volume`` folder. Its relative path must be defined in the ``your-repository.yaml`` in the form ``volume/your-repository``.

### Environment Settings

#### Number of Threads

You can configure the number of threads used to parallelize the analysis. The analysis gets faster the more threads are used, particularly for large repositories. If the configured number of threads is larger as your system can physically provide, the analysis slows down. **Take care of an appropriate RAM size**.

#### RAM Size

You can specify the amount of RAM to be used by the container as the second argument of the ``run.sh``. Note that this is not the actual RAM size. The docker container will at least use the amount of RAM it actually needs to run, even if the paramter is set to 0 (which is not recommended because it will lead to a runtime crash).

The configured additional RAM size must at least be the configured as: the number of threads times two multiplied with the repository size in GB. If your host system has less RAM, the number of threads must be reduced.

Alternatively, you can disable the additional RAM usage (which slows down the analysis at least by half). To do so, open the ``deb_run.sh`` and remove the lines marked by comments from the file and execute the build script again. You still need to provide the second argument while executing ``run.sh`` but it has no impact. 
However, if the container runs in the "no ramdisk" mode, the required space will be allocated on the default hard drive.

---

## Configuration

### Config Parameters

* ``input_repository`` STRING absolute path to the input repository.
* ``output_directory`` STRING (optional) exports the analysis to a json file to the specified directory.
* ``fetch_updates``BOOLEAN pull each branch of the (local tmp) repository before analysis starts.
* ``print_plot`` BOOLEAN show a pyplot of the results after finishing the calculation.
* ``html`` BOOLEAN generate a static HTML page with interactive in-depth result analysis. Requires the ``output_directory`` to be set.
* ``show_html`` BOOLEAN automatically open the HTML analysis in the default browser after completing the calculation. Requires ``html`` to be enabled.
---
* ``branch_ignore`` STRING a list of regualar expressions (python style). Branch names matching at least one of the expressions are ignored. For example, ignore numbered release branches.
* ``file_ignore`` STRING files to be ignored during comparison. A list of regex expressions which are matched against the file path. Binaries or large autogenerated files should be ignored during analysis, e.g., ``\.pdf``, ``package-lock\.json`` or ``gradle\-wrapper\/``. This has no impact on directory structures.
* ``file_whitelist`` STRING files to be whitelisted during comparison. A list of regex expressions which are matched against the file path. This has no impact on directory structures.
* ``timeout`` INT (optional) specifies the number of days in which a branch must have received its last commit. Otherwise it will not be analyzed. A value <= 0 skips this check.
---
* ``report_title=`` STRING (optional) renders a custom headline string into the generated HTML report. Long titles may result in bad formatting of the HTML.
* ``csv_file`` STRING (optional) DEVEVLOPER FEATURE Set a CSV distance matrix as input. This bypasses the repository analysis. Check the doc comments in the code for more information.
* ``simple_export`` BOOLEAN if set, a simple report consisting of a single txt file with the drift number is produces in addition to the full report.
  
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
