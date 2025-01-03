#!/bin/bash
set -xe
build_lib() {
  TMP_DIR=src/main/resources/debian
  TARGET_DIR=src/main/resources/debian-$AARCH
  cmake -B build $CMAKE_ARGS -DCMAKE_C_FLAGS="$CMAKE_CFLAGS" -DCMAKE_INSTALL_PREFIX=$TMP_DIR
  cmake --build build --config Release
  cmake --install build
  cp $TMP_DIR/libggml.so $TARGET_DIR/libggml$LIB_VARIANT.so
  if [ "$ADD_WRAPPER" = true ]; then
    cp $TMP_DIR/libwhisper.so.1 $TARGET_DIR/libwhisper.so.1
    cp $TMP_DIR/libwhisper-jni.so $TARGET_DIR/libwhisper-jni.so
  fi  
  rm -rf $TMP_DIR
  rm -rf build
}
AARCH=$(dpkg --print-architecture)
case $AARCH in
  amd64)
    LIB_VARIANT="+mf16c+mfma+mavx+mavx2" CMAKE_ARGS="-DGGML_AVX=ON -DGGML_AVX2=ON -DGGML_FMA=ON -DGGML_F16C=ON" build_lib
    ADD_WRAPPER=true CMAKE_ARGS="-DGGML_AVX=OFF -DGGML_AVX2=OFF -DGGML_FMA=OFF -DGGML_F16C=OFF" build_lib
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
