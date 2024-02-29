# syntax=docker/dockerfile:1

FROM debian:trixie
COPY . /driftool
WORKDIR /driftool
RUN rm -rf /driftool/volume
RUN chmod 777 ./setup_debian.sh
RUN ./setup_debian.sh

ENTRYPOINT ["/driftool/deb_run.sh"]