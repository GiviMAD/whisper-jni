#!/bin/bash
set -x
set -e

AARCH=$(dpkg --print-architecture)
case $AARCH in
  amd64)
    CFLAGS="$CFLAGS -mf16c -mfma -mavx -mavx2"
    CXXFLAGS=""
    ;;
  arm64)
    CFLAGS="$CFLAGS -mcpu=native"
	CXXFLAGS="$CXXFLAGS -mcpu=native"
    ;;
esac

cc -I${JAVA_HOME}/include -I${JAVA_HOME}/include/linux -O3 -DNDEBUG -std=c11 -fPIC -pthread $CFLAGS -c ./src/main/native/whisper/ggml.c -o ./src/main/native/ggml.o

g++ -c -std=c++11 -O3 -DNDEBUG -fPIC -pthread $CXXFLAGS -I${JAVA_HOME}/include -I${JAVA_HOME}/include/linux \
src/main/native/io_github_givimad_whisperjni_WhisperJNI.cpp -o src/main/native/io_github_givimad_whisperjni_WhisperJNI.o

g++ -shared -fPIC $CXXFLAGS -I src/main/native -o src/main/resources/debian-$AARCH/libwhisperjni.so src/main/native/ggml.o src/main/native/io_github_givimad_whisperjni_WhisperJNI.o -lc

rm -rf src/main/native/*.o
rm -rf src/main/java/io/github/givimad/whisperjni/*.class

