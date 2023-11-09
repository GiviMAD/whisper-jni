Set-PSDebug -Trace 1
$env:CLIENT_KEY="xxxxxxxxxxxxxxxxx";
$env:CFLAGS="-O3 -DNDEBUG -std=c11 -fPIC -pthread -D_XOPEN_SOURCE=600 -mf16c -mfma -mavx -mavx2";
$env:CXXFLAGS="-std=c++11 -O3 -DNDEBUG -fPIC -pthread -mf16c -mfma -mavx -mavx2";
# build ggml objects
gcc $env:CFLAGS -c .\src\main\native\whisper\ggml.c -o .\src\main\native\ggml.o
gcc $env:CFLAGS -c .\src\main\native\whisper\ggml-backend.c -o .\src\main\native\ggml-backend.o
gcc $env:CFLAGS -c .\src\main\native\whisper\ggml-quants.c -o .\src\main\native\ggml-quants.o
gcc $env:CFLAGS -c .\src\main\native\whisper\ggml-alloc.c -o .\src\main\native\ggml-alloc.o
# build whisper object
g++ -c $env:CXXFLAGS -I src\main\native\whisper -c .\src\main\native\whisper\whisper.cpp -o .\src\main\native\whisper.o
# build whisper jni wrapper object
g++ -c $env:CXXFLAGS -I $env:JAVA_HOME\include -I $env:JAVA_HOME\include\win32 -I src\main\native\whisper src\main\native\io_github_givimad_whisperjni_WhisperJNI.cpp -o src\main\native\io_github_givimad_whisperjni_WhisperJNI.o
# build tmp whisper dll to link non full jni wrapper version agains it
g++ -shared -static -I src\main\native -I src\main\native\whisper -o whisper.dll src\main\native\whisper.o src\main\native\ggml.o src\main\native\ggml-alloc.o src\main\native\ggml-quants.o src\main\native\ggml-backend.o 
# link full whisper jni shared object
g++ -shared -static -I src\main\native -I src\main\native\whisper -o src\main\resources\win-amd64\whisperjni_full.dll src\main\native\whisper.o src\main\native\ggml.o src\main\native\ggml-alloc.o src\main\native\ggml-quants.o src\main\native\ggml-backend.o src\main\native\io_github_givimad_whisperjni_WhisperJNI.o
# link whisper jni wrapper shared object, forcing whisper.dll depencency to be dynamic
g++ "-Wl,-Bdynamic,-lwhisper" "-Wl,-Bstatic" -shared -static -I src\main\native -I src\main\native\whisper -L. -o src\main\resources\win-amd64\whisperjni.dll src\main\native\io_github_givimad_whisperjni_WhisperJNI.o
# abort on error
if ($LastExitCode -ne 0) {
    Write-Error "Unable to build library"
    Exit 1
}
# clean
rm -fo src\main\native\*.o
rm -fo whisper.dll
Set-PSDebug -Trace 0