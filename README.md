# driftool :: Git Branch Inconsistency Analysis

**🚑 Under active development, features may change unexpectedly!**

---

The driftool calculates the drift analysis for git repositories. 
It automatically compares all branches, simulates merges and generates both a scalar drift metric, as well as a 3D view of the repository drift.
The pairwise distance is the number of lines of merge conflicts.
The base measure of the point-cloud is the *mean absolute deviation around a central point*.

The results of driftool indicate how well or poorly a repository is managed. 
High drift indicates large inconsistencies in between branches. 
The drift is an absolute metric that always has to be interpreted in the context of the repository size. 
A repository with dozens of collaborateurs and branches naturally has more drift as a project with 3 developers working on. 
However, the evolution of the drift over time gives useful insigts about the project health.

## Key Metrics

The driftool calculates the *Drift* measure **sd** (gamma).

* **sd** is the *Statement Drift* := a measure for the merge complexity based on the pair-wise git merge conflict count as a distance

In general, higher numbers indicate a more complex repository management.

:bulb: Per default, a git merge is a symmetric operation, meaning A into B produces the same conflicts as B into A. However, through certain git operations (resets, force operations, messed-up history) it happens that the merge is not symmatric. For example, A into B has 2 conflicing lines, although B into A has 4 conflicting lines. To cope with that, the driftool performs each merge both ways and takes the AVG of conflicts.

## Report Generation

The driftool generates HTML reports for further human analysis. 
The following figure shows such an analysis. 
The right side list displays all analysed branches. 

![Driftool results screenshot](./doc/screenshot_results_example.png)

The scatterplot uses a synthetic coordinate system resulting from multidimensional scaling of the pairwise distances. 
However, point distribution is helpful for repository analysis. 
Evenly scattered points indicate many unrelated but conflicting changes. 
Clusters indicate groups of very compatible branches. 
Outliers indicate standalone branches with lots of inconsistencies compared to the majority.

## Usage

The driftool is a python application. To run it, it requires:
- python 3.12.0 (other versions might work but are not tested)
- pip
- git accessible via PATH

It is preferred to use the existing ``requirements.txt`` to create a virtualenv.
Please see the official [virtualenv documentation](https://packaging.python.org/en/latest/guides/installing-using-pip-and-virtual-environments/#install-packages-in-a-virtual-environment-using-pip-and-venv).

> :bulb: Note that for large repositories, the drift calculation can take some time. 
> The number of comparisons is ``(number of branches)^2 - (number of branches)``. Each comparison round is linearly dependent on the repository size. We experienced times around 1s per comparison.

> :bulb: Driftool is not sufficiently tested on Windows. A known Windows issue is the possible inability to delete temporary files due to the git file lock. After using the driftool on Windows, please remove the ./tmp folder in the working directory manually.

For large (memory instensive) repositories, we recommend running the driftool from a RAM-Disk on a Linux system. This reduced the IO load significantly!

⚡**Disclaimer: The driftool creates a temporary local copy of the git repository. It executes NO GIT PUSH commands. However, the tool is in active development and errors might happen. We recommend not to use the driftool on a not backed-up repository.** (Disconnecting from the remote/network while executing the analysis is an equal measure).

### CLI

1. If installed, activate the venv and make sure all requirements are installed
2. Run ```python main.py config.yaml``` with the desired configuration.
3. After completion, the drift metrics are printed to stdout, if specified, report files and graph views are generated.

All processing steps are performed on a temporary local copy of the git repository. The orignal repository is not touched.

#### Arguments

* STRING path to a config yaml file. This is the repository analysis config.

The driftool system config. The ``driftool.yaml`` located in the default directory will be used.


**If run via docker, the config file and the repository must be located in the ``./volume`` directory. The path must be provided in local syntax ``./volume/config.yaml`` and ``./volume/repository``.**


#### Sysconf Paramters

* ``number_threads`` INT the number of threads + 1 the driftool will use. To run the analysis on 4 threads, set this to 4. A fifth thread serves as an orchestrator. Each thread > 1 will occupy additional disk space. This space is as large as the analyzed repository.
* ``max_ram_gb`` The maximum number of GB RAM used by the tool. This must be at least ``repository_size * number_threads + repository_size``. We recommend to add at least 2 GB more to ensure a stable analysis.

#### Config Parameters

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
---
* ``report_title=` STRING (optional) renders a custom headline string into the generated HTML report. Long titles may result in bad formatting of the HTML.
* ``csv_file`` STRING (optional) DEVEVLOPER FEATURE Set a CSV distance matrix as input. This bypasses the repository analysis. Check the doc comments in the code for more information.
* ``simple_export`` BOOLEAN if set, a simple report consisting of a single txt file with the drift number is produces in addition to the full report.

> :bulb: On Windows, use the driftool only via the Powershell and use Unix-style path encodings.

**Example**

```
python main.py ../my_config.yaml
```
#### Config

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
branch_ignore:
    - STRING
blacklist: 
    - STRING
whitelist:
    - STRING
```

**Example**

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

---

This repository is part of a research project of the [*Software Technology Group*](https://tu-dresden.de/ing/informatik/smt/st?set_language=en) at *Dresden University of Technology*.
Contact Karl Kegel (KKegel) for further information. 
