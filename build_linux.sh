set -x
set -e
javac -h src/main/native src/main/java/io/github/givimad/WhisperContext.java src/main/java/io/github/givimad/WhisperState.java src/main/java/io/github/givimad/WhisperCppJni.java

cc -I${JAVA_HOME}/include -I${JAVA_HOME}/include/linux -O3 -DNDEBUG -std=c11 -fPIC -pthread -mf16c -mfma -mavx -mavx2 -c ./src/main/native/whisper/ggml.c -o ./src/main/native/ggml.o

g++ -c -std=c++11 -O3 -DNDEBUG -fPIC -pthread -I${JAVA_HOME}/include -I${JAVA_HOME}/include/linux \
src/main/native/io_github_givimad_WhisperCppJni.cpp -o src/main/native/io_github_givimad_WhisperCppJni.o

g++ -shared -fPIC -I src/main/native -o src/main/resources/macos-x86-64/libwhisperjni.dylib src/main/native/ggml.o src/main/native/io_github_givimad_WhisperCppJni.o -lc

rm -rf src/main/native/*.o
rm -rf src/main/java/io/github/givimad/whisperjni/*.class

