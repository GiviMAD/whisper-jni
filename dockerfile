FROM maven:3.9.3-eclipse-temurin-17
RUN apt update && apt install -y git build-essential
COPY ggml-tiny.bin .
COPY pom.xml .
COPY .git ./.git
COPY src ./src
RUN git submodule update --init
COPY build_debian.sh .
RUN ./build_debian.sh
ARG RUN_TESTS
RUN if [ $(echo $RUN_TESTS) ]; then mvn test && echo "Done"; fi