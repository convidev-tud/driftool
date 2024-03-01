# syntax=docker/dockerfile:1

FROM debian:trixie
USER root 

COPY . /driftool
WORKDIR /driftool
RUN rm -rf /driftool/volume
RUN apt-get -y update
RUN apt -y install sudo
RUN chmod 777 ./deb_setup.sh
RUN chmod 777 ./deb_run.sh
RUN ./deb_setup.sh

ENTRYPOINT ["sudo", "/driftool/deb_run.sh"]