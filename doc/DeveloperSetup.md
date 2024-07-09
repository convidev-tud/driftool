# Developer Setup

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

## CLI

1. If installed, activate the venv and make sure all requirements are installed
2. Run ```python main.py config.yaml``` with the desired configuration.
3. After completion, the drift metrics are printed to stdout, if specified, report files and graph views are generated.

All processing steps are performed on a temporary local copy of the git repository. The orignal repository is not touched.

## Matrix Only

You can provide a pre-caluclated distance matrix for calculating drift value and drift plot.

The CSV file must look like this. The top horizontal row denotes the branch names. The same branch ordering applies to the vertical dimension. The body contains the pairwise distances. The matrix must be symmetric.

```CSV
A;B;C
0;1;7
1;0;1
7;1;0
```

## Docker

To develop and test the docker setup you need an additional docker installation on your system.
