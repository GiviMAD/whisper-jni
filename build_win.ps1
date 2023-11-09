Set-PSDebug -Trace 1

gcc -O3 -DNDEBUG -std=c11 -fPIC -pthread -mf16c -mfma -mavx -mavx2 -c .\src\main\native\whisper\ggml.c -o .\src\main\native\ggml.o

g++ -c -std=c++11 -O3 -DNDEBUG -fPIC -pthread -I -mf16c -mfma -mavx -mavx2 -I src\main\native\whisper -c .\src\main\native\whisper\whisper.cpp -o .\src\main\native\whisper.o

g++ -c -std=c++11 -O3 -DNDEBUG -fPIC -pthread -I $env:JAVA_HOME\include -I $env:JAVA_HOME\include\win32 -I src\main\native\whisper src\main\native\io_github_givimad_whisperjni_WhisperJNI.cpp -o src\main\native\io_github_givimad_whisperjni_WhisperJNI.o

g++ -shared -static -I src\main\native -I src\main\native\whisper -o whisper.dll src\main\native\whisper.o src\main\native\ggml.o

g++ -shared -static -I src\main\native -I src\main\native\whisper -o src\main\resources\win-amd64\whisperjni_full.dll src\main\native\whisper.o src\main\native\ggml.o src\main\native\io_github_givimad_whisperjni_WhisperJNI.o

g++ "-Wl,-Bdynamic,-lwhisper" "-Wl,-Bstatic" -shared -static -I src\main\native -I src\main\native\whisper -L. -o src\main\resources\win-amd64\whisperjni.dll src\main\native\io_github_givimad_whisperjni_WhisperJNI.o

if ($LastExitCode -ne 0) {
    Write-Error "Unable to build library"
    Exit 1
}

rm -fo src\main\native\*.o
rm -fo whisper.dll
Set-PSDebug -Trace 0