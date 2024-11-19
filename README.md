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
```./run.sh [repo path (in volume)]  [output path (in volume)] [config path (in volume)] [RAM alloc] [threads] [mode]``` 
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


#### Development Setup

To execute the driftool from sources on you local file system, execute the following command in the directory where the ``build.gradle`` file is located. This assumes you have a current version of gradle (8+) and Java (JDK 21) installed on your system.

```
gradle run --args="absolute_input absolute_working_dir config.yaml -i repo -m git -t 4"
```

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

### ``run.sh`` Arguments

```./run.sh [repo path (in volume)]  [output path (in volume)] [config path (in volume)] [RAM alloc] [threads] [mode]``` 

**repo path** is the path to the repository under analysis. It must be located in the volume.

**output path** is the path where the analysis reports are saved. It must be located in the volume. 

**config path** is the path to the configuration .yaml file used for analysis. It must be located in the volume.

**RAM** the RAM size to allocate.

**threads** the number of threads for parallel analysis.

**mode** the analysis mode. Use "git" for default git repository analysis. If you already have a custom distance matrix, you can use mode="matrix". YOu find more information at the end of this document.

#### Example

Assume this directory structure on your system:

* ``/home/
  * driftool/
  * your-repo/
  * your-repo.yaml
  
1. Move copy the repository an config to the volume:

* ``/home/
  * driftool/volume/
    * your-repo/
    * your-repo.yaml
  * ... 
  
2. Execute the run.sh in the ``/home/driftool`` directory (cd into it).

Assume 12 threads and 64GB of free RAM:

```./run.sh "your-repo"  "./" "your-repo.yaml" 65 12 "git"``` 

3. After successful execution, you find the reports in the specified directory

* ``/home/
  * driftool/volume/
    * your-repo/
    * your-repo.yaml
    * report_your-repo.html
    * report_your-repo.json
  * ... 

### "git" Mode Configuration .yaml

* ``jsonReport: Boolean`` If true, a JSON report will be generated and saved in the report directory.
* ``htmlReport: Boolean`` If true, an HTML report will be generated and saved in the report directory.
* ``ignoreBranches: List<String>`` List of branches that should be ignored. This is useful for branches that are not relevant for the analysis. The branch list can contain Regex patterns for which are searched in the branch name. Important: We use regex search (not regex match) to find the pattern anywhere in the branch name, e.g. the pattern "feature" will match "feature/branch" and "branch/feature". If the list is empty, no branches will be ignored.
* ``fileWhiteList: List<String>`` List of files that should be analyzed exclusively. This is useful if only particular file types should be included in the analysis. The file whitelist is applied before the file blacklist. The file list can contain Regex patterns for which are searched in the file path. Important: We use regex search (not regex match) to find the pattern anywhere in the file path, e.g. the pattern "test" will match "src/test/file" and "file/test". If the list is empty, no files will be ignored.
* ``fileBlackList: List<String>``List of files that should be ignored. This is useful if particular file types should be excluded from the analysis. The file blacklist is applied after the file whitelist. The file list can contain Regex patterns for which are searched in the file path. Important: We use regex search (not regex match) to find the pattern anywhere in the file path, e.g. the pattern "test" will match "src/test/file" and "file/test". If the list is empty, no files will be ignored.
* ``timeoutDays: Int`` The number of days a branch had to be active within to be included in the analysis. For example, if the timeoutDays is set to 30, only branches that were active in the last 30 days will be included. If the timeoutDays is set to 0, all branches will be included. This is useful to exclude dead branches as they might invalidate the analysis.
* ``reportIdentifier: String`` The identifier (or title) for the report. If unset, a unique default identifier will be generated.

```YAML
jsonReport: STRING
htmlReport: STRING
reportIdentifier: STRING
timeout: INT
fileWhiteList:
    - STRING
fileBlackList: 
    - STRING
ignoreBranches:
    - STRING
```

### Config Examples

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

```YAML
jsonReport: true
htmlReport: true
reportIdentifier: My Report
timeout: 90
fileWhiteList: []
fileBlackList: 
    - "build\\/"
    - "dist\\/"
    - "gen\\/"
    - "\\.min\\.js"
    - "\\.lib\\.js"
    - "node\\-modules\\/"
    - "\\.pdf"
    - "javadoc\\/"
    - "\\.png"
ignoreBranches:
    - "^release\\-"
    - "^v\\.
```

**Whitelist Example**

The following partial example only analyses Java files and HTML templates adn ignores all other files.

```YAML
fileWhiteList:
    - "\\.java"
    - "\\.template\\.html"
```

## Matrix Only Mode

> Work in progress

You can provide a pre-caluclated distance matrix for calculating drift value and drift plot.

The CSV file must look like this. The top horizontal row denotes the branch names. The same branch ordering applies to the vertical dimension. The body contains the pairwise distances. The matrix must be symmetric.

```CSV
A;B;C
0;1;7
1;0;1
7;1;0
```