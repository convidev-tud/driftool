# Driftool Docker Setup

## TLDR / Quick Setup

:zap: These are the quick setup steps, for a more spohisticated configuration, continue reading the following sections. This does only work on Unix/Linux systems.

1. Have docker (or docker desktop) installed and ready
2. Clone this repository
3. Set the prefered number of threads in the ``driftool.yaml`` 1...n
4. Make ``run.sh`` and ``build.sh`` executable via ``chmod``
5. Execute ``./build.sh`` in the working directory (where ``main.py`` is located).
6. Move the repository to analyze and the repository config into the ``./volume`` directory.
7. Execute ``./run.sh volume/y.yaml x`` where y is your config name and x is the number of GB RAM the container can use for speeding up the analysis
8. The analysis results are written to ``./volume``

