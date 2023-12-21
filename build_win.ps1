# build full version
cmake -B build -DCMAKE_INSTALL_PREFIX=src/main/resources/win-amd64 -DBUILD_SHARED_LIBS=0
cmake --build build --config Release
cmake --install build
rm -r -fo build
mv .\src\main\resources\win-amd64\whisper-jni.dll .\src\main\resources\win-amd64\whisper-jni_full.dll
# build wrapper for external dll version
cmake -B build -DCMAKE_INSTALL_PREFIX=src/main/resources/win-amd64
cmake --build build --config Release
cmake --install build
rm -r -fo build
rm -r -fo src/main/resources/win-amd64/*.lib
rm -r -fo src/main/resources/win-amd64/whisper.dll
