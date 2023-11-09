set -xe

AARCH=${1:-$(uname -m)}
case "$AARCH" in
  x86_64|amd64)
    AARCH=x86_64
    AARCH_NAME=amd64
    TARGET_VERSION=11.0
    CFLAGS="$CFLAGS -mf16c -mfma -mavx -mavx2 -DGGML_USE_ACCELERATE"
    CXXFLAGS=""
    LDFLAGS="-framework Accelerate"
    ;;
  arm64|aarch64)
    AARCH=arm64
    AARCH_NAME=arm64
    TARGET_VERSION=11.0
    CFLAGS="-DGGML_USE_ACCELERATE"
    CXXFLAGS=""
    LDFLAGS="-framework Accelerate"
    ;;
  *)
    echo Unsupported arch $AARCH
    ;;
    
esac

INCLUDE_JAVA="-I $JAVA_HOME/include -I $JAVA_HOME/include/darwin"
TARGET=$AARCH-apple-macosx$TARGET_VERSION

cc -O3 --target="$TARGET" -std=c11 -arch "$AARCH" \
$CFLAGS -DNDEBUG -fPIC -pthread -c ./src/main/native/whisper/ggml.c -o ./src/main/native/ggml.o

g++ -c -std=c++11 -arch "$AARCH" -O3 -DNDEBUG -fPIC -pthread $INCLUDE_JAVA \
-I src/main/native/whisper/ $CXXFLAGS --target="$TARGET" \
src/main/native/whisper/whisper.cpp -o src/main/native/whisper.o

g++ -c -std=c++11 -arch "$AARCH" -O3 -DNDEBUG -fPIC -pthread $INCLUDE_JAVA \
-I src/main/native/ -I src/main/native/whisper/ --target="$TARGET" \
src/main/native/io_github_givimad_whisperjni_WhisperJNI.cpp -o src/main/native/io_github_givimad_whisperjni_WhisperJNI.o
g++ -arch "$AARCH" --target="$TARGET" -dynamiclib -I src/main/native/whisper/ -o libwhisper.dylib src/main/native/ggml.o src/main/native/whisper.o -lc $LDFLAGS
g++ -arch "$AARCH" --target="$TARGET" -dynamiclib  -I src/main/native/ -I src/main/native/whisper/ -L. -lwhisper -o libwhisperjni.dylib src/main/native/io_github_givimad_whisperjni_WhisperJNI.o -lc $LDFLAGS
install_name_tool -change libwhisper.dylib @loader_path/libwhisper.dylib libwhisperjni.dylib
mv libwhisper.dylib src/main/resources/macos-$AARCH_NAME/libwhisper.dylib
mv libwhisperjni.dylib src/main/resources/macos-$AARCH_NAME/libwhisperjni.dylib
rm -rf src/main/native/*.o
