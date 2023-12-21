FROM maven:3.9.3-eclipse-temurin-17-focal
# use kitware repo to get upper cmake version; fixes armv7l build
RUN curl -s https://apt.kitware.com/kitware-archive.sh | bash -s
RUN apt update && apt install -y git build-essential cmake
COPY ggml-tiny.bin .
COPY pom.xml .
COPY CMakeLists.txt .
COPY .git ./.git
COPY src ./src
RUN git submodule update --init
COPY build_debian.sh .
RUN ./build_debian.sh
ARG RUN_TESTS
RUN if [ $(echo $RUN_TESTS) ]; then mvn test && echo "Done"; fi