# WhisperJNI

A JNI wrapper for [whisper.cpp](https://github.com/ggerganov/whisper.cpp), it allows basic usage of the library in Java.

# Platform support 

This library support the following systems:
* Windows x86_64
* Debian x86_64/arm64
* macOS x86_64/arm64

The native binaries for those platforms are included.

The library has been tested on in all the supported platforms unless macOS arm64.

Is have been build with some features enabled that should be available in modern devices,
please open an issue if you found it don't work on any of the supported platforms.

## Example

A basic example extracted from the tests.

```java
        var whisper = new WhisperJNI();
        whisper.loadLibrary();
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

If you want to implement any new api you need to:

* Add the native method description in src/main/java/io/github/givimad/whisperjni/WhisperJNI.java.
* Run the gen_header.sh script to regenerate the src/main/native/io_github_givimad_whisperjni_WhisperJNI.h header file. 
* Add the native method implementation in src/main/native/io_github_givimad_whisperjni_WhisperJNI.cpp.
* Add a new test for it at src/test/java/io/github/givimad/whisperjni/WhisperJNITest.java.

BR
