FROM openjdk:17-buster
RUN apt update && apt install -y build-essential
RUN wget https://dlcdn.apache.org/maven/maven-3/3.9.2/binaries/apache-maven-3.9.2-bin.tar.gz -q -P /tmp && \
    tar xf /tmp/apache-maven-*.tar.gz -C /opt && \
    ln -s /opt/apache-maven-3.9.2 /opt/maven
ENV M2_HOME="/opt/maven"
ENV MAVEN_HOME="/opt/maven"
ENV PATH="$M2_HOME/bin:$PATH"
COPY .git ./.git
COPY src ./src
COPY pom.xml .
COPY build_debian.sh .
COPY ggml-tiny.bin .
RUN git submodule update --init
RUN ./build_debian.sh
ARG RUN_TESTS
RUN if [ $(echo $RUN_TESTS) ]; then mvn test && echo "Done"; fi