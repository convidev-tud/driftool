# Driftool Docker Setup

## TLDR / Quick Setup

:zap: These are the quick setup steps, for a more spohisticated configuration, continue reading the following sections. This does only work on Unix/Linux systems.

1. Have docker (or docker desktop) installed and ready
2. Clone this repository
3. Set the prefered number of threads in the ``driftool.yaml`` 1...n
4. Make ``run.sh`` and ``build.sh`` executable via ``chmod``
5. Execute ``./build.sh`` in the working directory (where ``main.py`` is located).
6. Move the repository to analyze and the repository config into the ``./volume`` directory.
7. Execute ``./run.sh volume/y.yaml x`` where y is your config name and x is the number of GB RAM.
8. The analysis results are written to ``./volume``

:zap: **Note that the analysis of a large repository (branch-number) can take from few seconds up to several hours! It grows quadratically with the number of branches. For example if 10 branches take 10 minutes, 20 branches would take 40 minutes.**

## Script Overview

This repository contains serveral files playing a role for the docker setup and execution.

* ``build.sh`` is the buildscript of the docker image. Execute ``./build.sh`` to generate the docker image. If you want to configure the image creation, e.g., name, tag or location, you can modify the build command defined in the file. **You must execute this command only in the source root directory of this repository!**
* ``run.sh`` is the entry script to start the driftool container from the image. Beforehand, the ``build.sh`` must be executed once. If the ``build.sh`` was executed without modifications, the ``run.sh`` works out of the box. Otherwise, the script must be modified accordingly. The run script takes two positional parameters. The first parameter is the name of the repository config and the second paramter is the RAM size which will be allocated by the container.  Output reports are placed in the ``./volume`` directory. The container is destroyed after each run.
Example: ``./run.sh volume/repository.yaml 4``. **You must execute this command only in the source root directory of this repository!**
* (``dockerfile``) is the standard entry point for ``docker build`` and describes the composition of the docker image. If you are a driftool user, you do not have to edit this file.
* (``deb_run.sh``) is the entrypoint script of the created container. It prepares the ramdisk and starts the driftool's main method. If you are a driftool user, you do not have to edit this file.

### Repository and Config Location

The driftool repository contains a special folder called ``volume``. 
This folder is dynamically mounted to the docker container during its boot.
In consequence, You and the docker container can use this folder to exchange data.

1. The ``driftool.yaml`` file specifies the number of threads used for the analysis. Per default, the ``./driftool.yaml`` located in the source root is used. After image creation, changes to this file have no impact. For overwriting the default config (for example for specific runs only) without re-building the image, create a ``driftool.yaml`` in the ``./volume`` folder. The container has access to this config and uses it with priority.
2. The ``your-repository.yaml`` file containins your repository configuration, for example path, title, blacklist etc. This file must be located in the ``./volume`` folder. While executing ``run.sh``, this configuration is the first positional parameter and must be provided in the form ``volume/your-repository.yaml``.
The config must define the output path as ``volume/``. Otherwise, you have no access to the driftool reports.
3. The repository to analyze must be located in the ``./volume`` folder. Its relative path must be defined in the ``your-repository.yaml`` in the form ``volume/your-repository``.

## Environment Configuration

### Number of Threads

You can configure the number of threads used to parallelize the analysis in the ``driftool.yaml``. The analysis gets faster the more threads are used, particularly for large repositories. If the configured number of threads is larger as your system can physically provide, the analysis slows down. **Take care of an appropriate RAM size**.

### RAM Size

You can specify the amount of RAM to be used by the container as the second argument of the ``run.sh``. Note that this is not the actual RAM size. The docker container will at least use the amount of RAM it actually needs to run, even if the paramter is set to 0 (which is not recommended because it will lead to a runtime crash). The value defines the **additional** RAM that is used to speed up the execution. Basically, a ramdisk is created inside the container.

The configured additional RAM size must at least be the configured as: the number of threads multiplied with the repository size in GB. If your host system has less RAM, the number of threads must be reduced.

Alternatively, you can disable the additional RAM usage (which slows down the analysis at least by half). To do so, open the ``deb_run.sh`` and remove the lines marked by comments from the file. You still need to provide the second argument while executing ``run.sh`` but it has no impact. 
However, if the container runs in the "no ramdisk" mode, the required space will be allocated on the default hard drive.
