# syntax=docker/dockerfile:1

# FROM debian:bookworm or debian:trixie works as well
FROM ubuntu:22.04
USER root 

COPY . /driftool
WORKDIR /driftool
RUN rm -rf /driftool/volume
RUN apt-get -y update
RUN apt -y install sudo
RUN chmod 777 ./debrun.sh
RUN rm -rf ./volume
RUN rm -rf ./tmp
RUN mkdir -p ./volume

RUN apt-get -y update
RUN apt-get -y upgrade

RUN apt -y install unzip
RUN apt -y install openjdk-21-jdk
RUN apt -y install python3
RUN apt -y install python3-pip
RUN apt -y install python-is-python3

RUN pip install numpy
RUN pip install scikit-learn

WORKDIR /driftool/driftool_kt
RUN ./gradlew clean
RUN ./gradlew build
RUN mv build/distributions/driftool_kt-1.0-SNAPSHOT.zip /driftool

WORKDIR /driftool
RUN unzip driftool_kt-1.0-SNAPSHOT.zip

RUN apt -y install git

ENTRYPOINT ["sudo", "/driftool/debrun.sh"]