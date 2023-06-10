#!/bin/bash
set -x
set -e

javac -h src/main/native src/main/java/io/github/givimad/whisperjni/NativeUtils.java src/main/java/io/github/givimad/whisperjni/WhisperContext.java src/main/java/io/github/givimad/whisperjni/WhisperState.java src/main/java/io/github/givimad/whisperjni/WhisperJNI.java

cc -I${JAVA_HOME}/include -I${JAVA_HOME}/include/linux -O3 -DNDEBUG -std=c11 -fPIC -pthread -mf16c -mfma -mavx -mavx2 -c ./src/main/native/whisper/ggml.c -o ./src/main/native/ggml.o

g++ -c -std=c++11 -O3 -DNDEBUG -fPIC -pthread -I${JAVA_HOME}/include -I${JAVA_HOME}/include/linux \
src/main/native/io_github_givimad_whisperjni_WhisperJNI.cpp -o src/main/native/io_github_givimad_whisperjni_WhisperJNI.o

g++ -shared -fPIC -I src/main/native -o src/main/resources/debian-$(dpkg --print-architecture)/libwhisperjni.so src/main/native/ggml.o src/main/native/io_github_givimad_whisperjni_WhisperJNI.o -lc

rm -rf src/main/native/*.o
rm -rf src/main/java/io/github/givimad/whisperjni/*.class

