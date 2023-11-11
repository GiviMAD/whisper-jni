# WhisperJNI

A JNI wrapper for [whisper.cpp](https://github.com/ggerganov/whisper.cpp), allows transcribe speech to text in Java.

## Platform support 

This library aims to support the following platforms:

* Windows x86_64 (built with mingw, not required for execution)
* Linux GLIBC x86_64/arm64 (built with debian focal, GLIBC version 2.31)
* macOS x86_64/arm64 (built targeting v11.0)

The native binaries for those platforms are included in the distributed jar.

The library has been tested on all the supported platforms unless macOS arm64.

The included binaries have been built with some features enabled that I think are available in most modern devices,
please open an issue if you found it don't work on any of the supported platforms.

## Installation

The package is distributed through [Maven Central](https://central.sonatype.com/artifact/io.github.givimad/whisper-jni).

You can also find the package's jar attached to each [release](https://github.com/GiviMAD/whisper-jni/releases).

### Use external whisper shared library.

It's possible to use your own build of the whisper.cpp shared library with this project.

On `Linux/macOs` you need to provide the library path to the `loadLibrary` method.

```java
        ...
        // load platform binaries
        var loadOptions = new WhisperJNI.LoadOptions();
        // Log library load to stdout
        loadOptions.logger = System.out::println;
        // Provide path to libwhisper so/dylib file.
        loadOptions.whisperLib = Paths.get("/usr/local/lib/libwhisper.so");
        // register the library
        WhisperJNI.loadLibrary(loadOptions);
        ...
```

On `windows` it's automatically used if `whisper.dll` exists in some of the directories in the $env:PATH variable.

## Example

A basic example extracted from the tests.

```java
        ...
        WhisperJNI.loadLibrary(); // load platform binaries
        WhisperJNI.setLibraryLogger(null); // capture/disable whisper.cpp log
        var whisper = new WhisperJNI();
        float[] samples = readJFKFileSamples();
        var ctx = whisper.init(Path.of(System.getProperty("user.home"), 'ggml-tiny.bin'));
        var params = new WhisperFullParams();
        int result = whisper.full(ctx, params, samples, samples.length);
        if(result != 0) {
            throw new RuntimeException("Transcription failed with code " + result);
        }
        int numSegments = whisper.fullNSegments(ctx);
        assertEquals(1, numSegments);
        String text = whisper.fullGetSegmentText(ctx,0);
        assertEquals(" And so my fellow Americans ask not what your country can do for you ask what you can do for your country.", text);
        ctx.close();
        ...
```

## Building and testing the project.

You need Java and Cpp setup.

After cloning the project you need to init the whisper.cpp submodule by running:

```sh
git submodule update --init
```

Then you need to download the model used in the tests using the script 'download-test-model.sh' or 'download-test-model.ps1', the ggml-tiny model.

Run the appropriate build script for your platform (build_debian.sh, build_macos.sh or build_win.ps1), it will place the native library file on the resources directory.

Finally you can run the project tests to confirm it works:

```sh
mvn test
```

## Extending the native api

If you want to add any missing whisper.cpp functionality you need to:

* Add the native method description in src/main/java/io/github/givimad/whisperjni/WhisperJNI.java.
* Run the gen_header.sh script to regenerate the src/main/native/io_github_givimad_whisperjni_WhisperJNI.h header file. 
* Add the native method implementation in src/main/native/io_github_givimad_whisperjni_WhisperJNI.cpp.
* Add a new test for it at src/test/java/io/github/givimad/whisperjni/WhisperJNITest.java.

BR
