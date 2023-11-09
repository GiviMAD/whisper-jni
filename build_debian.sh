#!/bin/bash
set -xe
build_lib() {
  INCLUDE_JAVA="-I${JAVA_HOME}/include -I${JAVA_HOME}/include/linux"
  cc -O3 -DNDEBUG -std=c11 -fPIC -pthread $CFLAGS -c ./src/main/native/whisper/ggml.c -o ./src/main/native/ggml.o
  g++ -c -std=c++11 -O3 -DNDEBUG -fPIC -pthread \
  -I src/main/native/whisper/ $CXXFLAGS \
  src/main/native/whisper/whisper.cpp -o src/main/native/whisper.o
  g++ -c -std=c++11 -O3 -DNDEBUG -fPIC -pthread $CXXFLAGS -I src/main/native -I src/main/native/whisper $INCLUDE_JAVA \
  src/main/native/io_github_givimad_whisperjni_WhisperJNI.cpp -o src/main/native/io_github_givimad_whisperjni_WhisperJNI.o
  g++ -shared $CXXFLAGS -fPIC -I src/main/native/whisper/ src/main/native/ggml.o src/main/native/whisper.o -o libwhisper.so
  g++ -shared $CXXFLAGS -fPIC -I src/main/native/whisper/ -Wl,-rpath='${ORIGIN}' src/main/native/io_github_givimad_whisperjni_WhisperJNI.o -L. -lwhisper -o libwhisperjni.so
  mv libwhisper.so src/main/resources/debian-$AARCH/libwhisper$LIB_VARIANT.so
  mv libwhisperjni.so src/main/resources/debian-$AARCH/libwhisperjni.so
  rm -rf src/main/native/*.o
}
AARCH=$(dpkg --print-architecture)
case $AARCH in
  amd64)
    LIB_VARIANT="+mf16c+mfma+mavx+mavx2" CFLAGS="-mf16c -mfma -mavx -mavx2" CXXFLAGS="-mf16c -mfma -mavx -mavx2" build_lib
    build_lib
    ;;
  arm64)
    LIB_VARIANT="+crc" CFLAGS="-march=armv8.1-a+crc" CXXFLAGS="-march=armv8.1-a+crc" build_lib
    LIB_VARIANT="+fp16" CFLAGS="-march=armv8.2-a+fp16" CXXFLAGS="-march=armv8.2-a+fp16" build_lib
    build_lib
    ;;
  armhf|armv7l)
    AARCH=armv7l
    CFLAGS="-mfpu=neon -mfp16-format=ieee -mno-unaligned-access -funsafe-math-optimizations" CXXFLAGS="" build_lib
    LIB_VARIANT="+crc" CFLAGS="-march=armv8-a+crc -mfpu=neon-fp-armv8 -mfp16-format=ieee -mno-unaligned-access -funsafe-math-optimizations" CXXFLAGS="" build_lib
    ;;
esac
