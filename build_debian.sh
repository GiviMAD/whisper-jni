#!/bin/bash
set -xe
build_lib() {
  TMP_DIR=src/main/resources/debian
  TARGET_DIR=src/main/resources/debian-$AARCH
  cmake -B build $CMAKE_ARGS -DCMAKE_C_FLAGS="$CMAKE_CFLAGS" -DCMAKE_INSTALL_PREFIX=$TMP_DIR
  cmake --build build --config Release
  cmake --install build
  mv $TMP_DIR/libwhisper.so $TARGET_DIR/libwhisper$LIB_VARIANT.so
  if [ "$ADD_WRAPPER" = true ]; then
    mv $TMP_DIR/libwhisper-jni.so $TARGET_DIR/libwhisper-jni.so
  fi
  rm -rf $TMP_DIR
  rm -rf build
}
AARCH=$(dpkg --print-architecture)
case $AARCH in
  amd64)
    LIB_VARIANT="+mf16c+mfma+mavx+mavx2" CMAKE_ARGS="-DWHISPER_NO_AVX=OFF -DWHISPER_NO_AVX2=OFF -DWHISPER_NO_FMA=OFF -DWHISPER_NO_F16C=OFF" build_lib
    ADD_WRAPPER=true CMAKE_ARGS="-DWHISPER_NO_AVX=ON -DWHISPER_NO_AVX2=ON -DWHISPER_NO_FMA=ON -DWHISPER_NO_F16C=ON" build_lib
    ;;
  arm64)
    LIB_VARIANT="+fp16" CMAKE_CFLAGS="-march=armv8.2-a+fp16" build_lib
    ADD_WRAPPER=true LIB_VARIANT="+crc" CMAKE_CFLAGS="-march=armv8.1-a+crc" build_lib
    ;;
  armhf|armv7l)
    AARCH=armv7l
    LIB_VARIANT="+crc" CMAKE_CFLAGS="-march=armv8-a+crc -mfpu=neon-fp-armv8 -mfp16-format=ieee -mno-unaligned-access" build_lib
    ADD_WRAPPER=true CMAKE_CFLAGS="-mfpu=neon -mfp16-format=ieee -mno-unaligned-access" build_lib
    ;;
esac
