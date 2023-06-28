#!/bin/bash
set -xe
build_lib() {
  cc -I${JAVA_HOME}/include -I${JAVA_HOME}/include/linux -O3 -DNDEBUG -std=c11 -fPIC -pthread $CFLAGS -c ./src/main/native/whisper/ggml.c -o ./src/main/native/ggml.o

  g++ -c -std=c++11 -O3 -DNDEBUG -fPIC -pthread $CXXFLAGS -I${JAVA_HOME}/include -I${JAVA_HOME}/include/linux \
  src/main/native/io_github_givimad_whisperjni_WhisperJNI.cpp -o src/main/native/io_github_givimad_whisperjni_WhisperJNI.o

  g++ -shared -fPIC $CXXFLAGS -I src/main/native -o src/main/resources/debian-$AARCH/libwhisperjni$LIB_VARIANT.so src/main/native/ggml.o src/main/native/io_github_givimad_whisperjni_WhisperJNI.o -lc

  rm -rf src/main/native/*.o
}
AARCH=$(dpkg --print-architecture)
case $AARCH in
  amd64)
    build_lib
    LIB_VARIANT="+mf16c+mfma+mavx+mavx2" CFLAGS="-mf16c -mfma -mavx -mavx2" CXXFLAGS="-mf16c -mfma -mavx -mavx2" build_lib
    ;;
  arm64)
    build_lib
    LIB_VARIANT="+crc" CFLAGS="-march=armv8.1-a+crc" CXXFLAGS="-march=armv8.1-a+crc" build_lib
    LIB_VARIANT="+fp16" CFLAGS="-march=armv8.2-a+fp16" CXXFLAGS="-march=armv8.2-a+fp16" build_lib
    ;;
  armhf|armv7l)
    AARCH=armv7l
    CFLAGS="-mfpu=neon -mfp16-format=ieee -mno-unaligned-access -funsafe-math-optimizations" \
    CXXFLAGS="" \
    build_lib
    LIB_VARIANT="+crc" \
    CFLAGS="-march=armv8-a+crc -mfpu=neon-fp-armv8 -mfp16-format=ieee -mno-unaligned-access -funsafe-math-optimizations" \
    CXXFLAGS="" \
    build_lib
    ;;
esac
