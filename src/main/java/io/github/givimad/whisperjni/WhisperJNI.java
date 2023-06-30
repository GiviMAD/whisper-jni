package io.github.givimad.whisperjni;

import io.github.givimad.whisperjni.internal.NativeUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * The {@link WhisperJNI} class allows to use whisper.cpp thought JNI.
 *
 * @author Miguel Álvarez Díez - Initial contribution
 */
public class WhisperJNI {
    private static boolean libraryLoaded;

//region native api

    private native int init(String model);
    private native int initNoState(String model);
    private native int initState(int model);
    private native boolean isMultilingual(int model);
    private native int full(int context, WhisperFullParams params, float[] samples, int numSamples);
    private native int fullWithState(int context, int state, WhisperFullParams params, float[] samples, int numSamples);
    private native int fullNSegments(int context);
    private native int fullNSegmentsFromState(int state);
    private native long fullGetSegmentTimestamp0(int context, int index);
    private native long fullGetSegmentTimestamp1(int context, int index);
    private native String fullGetSegmentText(int context, int index);
    private native long fullGetSegmentTimestamp0FromState(int state, int index);
    private native long fullGetSegmentTimestamp1FromState(int state, int index);
    private native String fullGetSegmentTextFromState(int state, int index);
    private native void freeContext(int context);
    private native void freeState(int state);

//endregion

    /**
     * Creates a new whisper context.
     *
     * @param model {@link Path} to the whisper ggml model file.
     *
     * @return A new {@link WhisperContext}.
     * @throws IOException if model file is missing.
     */
    public WhisperContext init(Path model) throws IOException {
        var absModelPath = model.toAbsolutePath();
        assertModelExists(model, absModelPath);
        return new WhisperContext(this, init(absModelPath.toString()));
    }

    /**
     * Creates a new whisper context without state.
     *
     * @param model {@link Path} to the whisper ggml model file.
     *
     * @return A new {@link WhisperContext} without state.
     * @throws IOException if model file is missing.
     */
    public WhisperContext initNoState(Path model) throws IOException {
        var absModelPath = model.toAbsolutePath();
        assertModelExists(model, absModelPath);
        return new WhisperContext(this, initNoState(absModelPath.toString()));
    }

    /**
     * Creates a new whisper.cpp state for the provided context.
     *
     * @param context the {@link WhisperContext} of this state.
     *
     * @return A new {@link WhisperContext}.
     */
    public WhisperState initState(WhisperContext context) {
        WhisperJNIPointer.assertAvailable(context);
        return new WhisperState(this, initState(context.ref), context);
    }

    /**
     * Is multilingual.
     *
     * @param context the {@link WhisperContext} to check.
     *
     * @return true if model support multiple languages
     */
    public boolean isMultilingual(WhisperContext context) {
        WhisperJNIPointer.assertAvailable(context);
        return isMultilingual(context.ref);
    }

    /**
     * Run whisper.cpp full audio transcription.
     *
     * @param context the {@link WhisperContext} used to transcribe.
     * @param params a {@link WhisperFullParams} instance with the desired configuration.
     * @param samples the audio samples (f32 encoded samples with sample rate 16000).
     * @param numSamples the number of audio samples provided.
     * @return a result code, values other than 0 indicates problems.
     */
    public int full(WhisperContext context, WhisperFullParams params, float[] samples, int numSamples) {
        WhisperJNIPointer.assertAvailable(context);
        return full(context.ref, params, samples, numSamples);
    }

    /**
     * Run whisper.cpp full audio transcription.
     *
     * @param context the {@link WhisperContext} used to transcribe.
     * @param state the {@link WhisperState} used to transcribe.
     * @param params a {@link WhisperFullParams} instance with the desired configuration.
     * @param samples the audio samples (f32 encoded samples with sample rate 16000).
     * @param numSamples the number of audio samples provided.
     * @return a result code, values other than 0 indicates problems.
     */
    public int fullWithState(WhisperContext context, WhisperState state, WhisperFullParams params, float[] samples, int numSamples) {
        WhisperJNIPointer.assertAvailable(context);
        WhisperJNIPointer.assertAvailable(state);
        return fullWithState(context.ref, state.ref, params, samples, numSamples);
    }

    /**
     * Gets the available number of text segments.
     *
     * @param state the {@link WhisperState} used to transcribe
     *
     * @return available number of segments
     */
    public int fullNSegmentsFromState(WhisperState state) {
        WhisperJNIPointer.assertAvailable(state);
        return fullNSegmentsFromState(state.ref);
    }

    /**
     * Gets the available number of text segments.
     *
     * @param context the {@link WhisperContext} used to transcribe
     * @return available number of segments
     */
    public int fullNSegments(WhisperContext context){
        WhisperJNIPointer.assertAvailable(context);
        return fullNSegments(context.ref);
    }

    /**
     * Gets start timestamp of text segment by index.
     *
     * @param context a {@link WhisperContext} used to transcribe
     * @param index the segment index
     * @return start timestamp of segment text, like 800 means 8s
     */
    public long fullGetSegmentTimestamp0(WhisperContext context, int index) {
        WhisperJNIPointer.assertAvailable(context);
        return fullGetSegmentTimestamp0(context.ref, index);
    }

    /**
     * Gets end timestamp of text segment by index.
     *
     * @param context a {@link WhisperContext} used to transcribe
     * @param index the segment index
     * @return end timestamp of segment text
     */
    public long fullGetSegmentTimestamp1(WhisperContext context, int index) {
        WhisperJNIPointer.assertAvailable(context);
        return fullGetSegmentTimestamp1(context.ref, index);
    }

    /**
     * Gets text segment by index.
     *
     * @param context a {@link WhisperContext} used to transcribe
     * @param index the segment index
     * @return the segment text
     */
    public String fullGetSegmentText(WhisperContext context, int index) {
        WhisperJNIPointer.assertAvailable(context);
        return fullGetSegmentText(context.ref, index);
    }

    /**
     * Gets start timestamp of text segment by index.
     *
     * @param state a {@link WhisperState} used to transcribe
     * @param index the segment index
     * @return start timestamp of segment text
     */
    public long fullGetSegmentTimestamp0FromState(WhisperState state, int index) {
        WhisperJNIPointer.assertAvailable(state);
        return fullGetSegmentTimestamp0FromState(state.ref, index);
    }

    /**
     * Gets end timestamp of text segment by index.
     *
     * @param state a {@link WhisperState} used to transcribe
     * @param index the segment index
     * @return end timestamp of segment text
     */
    public long fullGetSegmentTimestamp1FromState(WhisperState state, int index) {
        WhisperJNIPointer.assertAvailable(state);
        return fullGetSegmentTimestamp1FromState(state.ref, index);
    }

    /**
     * Gets text segment by index.
     *
     * @param state a {@link WhisperState} used to transcribe
     * @param index the segment index
     * @return the segment text
     */
    public String fullGetSegmentTextFromState(WhisperState state, int index) {
        WhisperJNIPointer.assertAvailable(state);
        return fullGetSegmentTextFromState(state.ref, index);
    }

    /**
     * Release context memory in native implementation.
     *
     * @param context the {@link WhisperContext} to release
     */
    public void free(WhisperContext context) {
        if (context.isReleased()) {
            return;
        }
        context.release();
        freeContext(context.ref);
    }

    /**
     * Release state memory in native implementation.
     *
     * @param state the {@link WhisperState} to release
     */
    public void free(WhisperState state) {
        if (state.isReleased()) {
            return;
        }
        state.release();
        freeState(state.ref);
    }

    /**
     * Register the native library, should be called at first.
     * @throws IOException when unable to load the native library
     */
    public static void loadLibrary() throws IOException {
        if (libraryLoaded) {
            return;
        }
        String bundleLibraryPath = null;
        String osName = System.getProperty("os.name").toLowerCase();
        String osArch = System.getProperty("os.arch").toLowerCase();
        if (osName.contains("win")) {
            if(osArch.contains("amd64") || osArch.contains("x86_64")) {
                bundleLibraryPath = "/win-amd64/libwhisperjni.dll";
            }
        } else if (osName.contains("nix") || osName.contains("nux")
                || osName.contains("aix")) {
            String cpuInfo;
            try {
                cpuInfo = Files.readString(Path.of("/proc/cpuinfo"));
            } catch (IOException ignored) {
                cpuInfo = "";
            }
            if(osArch.contains("amd64") || osArch.contains("x86_64")) {
                if(cpuInfo.contains("avx2") && cpuInfo.contains("fma") && cpuInfo.contains("f16c") && cpuInfo.contains("avx")) {
                    bundleLibraryPath = "/debian-amd64/libwhisperjni+mf16c+mfma+mavx+mavx2.so";
                } else {
                    bundleLibraryPath = "/debian-amd64/libwhisperjni.so";
                }
            } else if(osArch.contains("aarch64") || osArch.contains("arm64")) {
                if(cpuInfo.contains("fphp")) {
                    bundleLibraryPath = "/debian-arm64/libwhisperjni+fp16.so";
                } else if(cpuInfo.contains("crc32")) {
                    bundleLibraryPath = "/debian-arm64/libwhisperjni+crc.so";
                } else {
                    bundleLibraryPath = "/debian-arm64/libwhisperjni.so";
                }
            } else if(osArch.contains("armv7") || osArch.contains("arm")) {
                if(cpuInfo.contains("crc32")) {
                    bundleLibraryPath = "/debian-armv7l/libwhisperjni+crc.so";
                } else {
                    bundleLibraryPath = "/debian-armv7l/libwhisperjni.so";
                }
            }
        } else if (osName.contains("mac") || osName.contains("darwin")) {
            if(osArch.contains("amd64") || osArch.contains("x86_64")) {
                bundleLibraryPath = "/macos-amd64/libwhisperjni.dylib";
            } else if(osArch.contains("aarch64") || osArch.contains("arm64")) {
                bundleLibraryPath ="/macos-arm64/libwhisperjni.dylib";
            }
        }
        if (bundleLibraryPath == null) {
            throw new java.io.IOException("WhisperJNI: Unsupported platform " + osName + " - " + osArch);
        }
        NativeUtils.loadLibraryFromJar(bundleLibraryPath);
        libraryLoaded = true;
    }

    /**
     * In order to avoid sharing pointers between the c++ and java, we use this
     * util base class which holds a random integer id generated in the whisper.cpp wrapper.
     *
     * @author Miguel Álvarez Díez - Initial contribution
     */
    protected static abstract class WhisperJNIPointer implements AutoCloseable {
        /**
         * Native pointer reference identifier.
         */
        protected final int ref;
        private boolean released;

        /**
         * Asserts the provided pointer is still available.
         *
         * @param pointer a {@link WhisperJNIPointer} instance representing a pointer.
         */
        protected static void assertAvailable(WhisperJNIPointer pointer) {
            if (pointer.isReleased()) {
                throw new RuntimeException("Unavailable pointer, object is closed");
            }
        }

        /**
         * Creates a new object used to represent a struct pointer on the native library.
         * @param ref a random integer id generated by the native wrapper
         */
        protected WhisperJNIPointer(int ref) {
            this.ref = ref;
        }

        /**
         * Return true if native memory is free
         * @return a boolean indicating if the native data was already released
         */
        protected boolean isReleased() {
            return released;
        }

        /**
         * Mark the point as released
         */
        protected void release() {
            released = true;
        }
    }

    private static void assertModelExists(Path model, Path path) throws IOException {
        if (!model.toFile().exists() || !model.toFile().isFile()) {
            throw new IOException("Missing model file: " + path);
        }
    }
}
