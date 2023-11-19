set -xe

AARCH=${1:-$(uname -m)}
CFLAGS="-O3 -DNDEBUG -std=c11 -fPIC -D_XOPEN_SOURCE=600 -D_DARWIN_C_SOURCE -pthread"
CXXFLAGS="-O3 -DNDEBUG -std=c++11 -fPIC -D_XOPEN_SOURCE=600 -D_DARWIN_C_SOURCE -pthread"
case "$AARCH" in
  x86_64|amd64)
    AARCH=x86_64
    AARCH_NAME=amd64
    TARGET_VERSION=11.0
    CFLAGS="$CFLAGS -mf16c -mfma -mavx -mavx2 -DGGML_USE_ACCELERATE"
    LDFLAGS="-framework Accelerate"
    ;;
  arm64|aarch64)
    AARCH=arm64
    AARCH_NAME=arm64
    TARGET_VERSION=11.0
    CFLAGS="$CFLAGS -DGGML_USE_ACCELERATE"
    LDFLAGS="-framework Accelerate"
    ;;
  *)
    echo Unsupported arch $AARCH
    ;;
    
esac

INCLUDE_JAVA="-I $JAVA_HOME/include -I $JAVA_HOME/include/darwin"
TARGET=$AARCH-apple-macosx$TARGET_VERSION
# build ggml objects
cc --target="$TARGET" -arch "$AARCH" $CFLAGS -c ./src/main/native/whisper/ggml.c -o ./src/main/native/ggml.o
cc --target="$TARGET" -arch "$AARCH" $CFLAGS -c ./src/main/native/whisper/ggml-alloc.c -o ./src/main/native/ggml-alloc.o
cc --target="$TARGET" -arch "$AARCH" $CFLAGS -c ./src/main/native/whisper/ggml-backend.c -o ./src/main/native/ggml-backend.o
cc --target="$TARGET" -arch "$AARCH" $CFLAGS -c ./src/main/native/whisper/ggml-quants.c -o ./src/main/native/ggml-quants.o
# build whisper object
g++ -c -arch "$AARCH" \
-I src/main/native/whisper/ $CXXFLAGS --target="$TARGET" \
src/main/native/whisper/whisper.cpp -o src/main/native/whisper.o
# build whisper grammar parser object
g++ -c -arch "$AARCH" \
-I src/main/native/whisper/ $CXXFLAGS --target="$TARGET" \
src/main/native/whisper/examples/grammar-parser.cpp -o src/main/native/grammar-parser.o
# build whisper jni wrapper object
g++ -c -arch "$AARCH" $INCLUDE_JAVA \
-I src/main/native/ -I src/main/native/whisper/ -I src/main/native/whisper/examples/ $CXXFLAGS --target="$TARGET" \
src/main/native/io_github_givimad_whisperjni_WhisperJNI.cpp -o src/main/native/io_github_givimad_whisperjni_WhisperJNI.o
# link whisper shared object
g++ -arch "$AARCH" --target="$TARGET" -dynamiclib -I src/main/native/whisper/ -o libwhisper.dylib src/main/native/ggml.o src/main/native/ggml-alloc.o src/main/native/ggml-backend.o src/main/native/ggml-quants.o src/main/native/whisper.o -lc $LDFLAGS
# link whisper jni wrapper shared object
g++ -arch "$AARCH" --target="$TARGET" -dynamiclib -I src/main/native/ -I src/main/native/whisper/ -L. -lwhisper -o libwhisperjni.dylib src/main/native/grammar-parser.o src/main/native/io_github_givimad_whisperjni_WhisperJNI.o -lc $LDFLAGS
# force search for libwhisper.dylib on same dir
install_name_tool -change libwhisper.dylib @loader_path/libwhisper.dylib libwhisperjni.dylib
# clean
mv libwhisper.dylib src/main/resources/macos-$AARCH_NAME/libwhisper.dylib
mv libwhisperjni.dylib src/main/resources/macos-$AARCH_NAME/libwhisperjni.dylib
rm -rf src/main/native/*.o
