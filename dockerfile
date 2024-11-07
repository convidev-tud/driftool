# syntax=docker/dockerfile:1

# FROM debian:bookworm or debian:trixie works as well
FROM debian:trixie
USER root 

COPY . /driftool
WORKDIR /driftool
RUN rm -rf /driftool/volume
RUN apt-get -y update
RUN apt -y install sudo
RUN chmod 777 ./deb_run.sh
RUN rm -rf ./volume
RUN rm -rf ./tmp
RUN mkdir -p ./volume

RUN apt-get -y update
RUN apt-get -y upgrade

# TODO: migrate python to java/kotlin stuff

RUN apt -y install python3
RUN apt -y install python3-venv
RUN apt -y install python-is-python3
RUN apt -y install python3-pip

RUN apt -y install git

RUN git config --global user.name "driftool"
RUN git config --global user.email "analysis@driftool.io"

RUN python3 -m venv env
RUN . env/bin/activate && python3 -m pip install -r requirements.txt

ENTRYPOINT ["sudo", "/driftool/debrun.sh"]