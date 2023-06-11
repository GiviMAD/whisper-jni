set -xe

cc -I${JAVA_HOME}/include -I${JAVA_HOME}/include/darwin -O3 -DNDEBUG -std=c11 -fPIC -pthread -mf16c -mfma -mavx -mavx2 -c ./src/main/native/whisper/ggml.c -o ./src/main/native/ggml.o

g++ -c -std=c++11 -O3 -DNDEBUG -fPIC -pthread -I${JAVA_HOME}/include -I${JAVA_HOME}/include/darwin \
src/main/native/io_github_givimad_whisperjni_WhisperJNI.cpp -o src/main/native/io_github_givimad_whisperjni_WhisperJNI.o

g++ -dynamiclib -I src/main/native -o src/main/resources/macos-x86-64/libwhisperjni.dylib src/main/native/ggml.o src/main/native/io_github_givimad_whisperjni_WhisperJNI.o -lc

rm -rf src/main/native/*.o


