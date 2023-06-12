package io.github.givimad.whisperjni;

import io.github.givimad.whisperjni.internal.NativeUtils;

import java.io.IOException;
import java.nio.file.Path;

/**
 * The {@link WhisperJNI} class allows to use whisper.cpp thought JNI.
 *
 * @author Miguel Álvarez Díez - Initial contribution
 */
public class WhisperJNI {
    private static boolean libraryLoaded;

    private native int init(String model);
    private native int initNoState(String model);
    private native int initState(int model);

    /**
     * Release context memory by id
     * @param context the pointer id
     */
    protected native void freeContext(int context);
    /**
     * Release context memory by id
     * @param state the pointer id
     */
    protected native void freeState(int state);
    private native int full(int context, WhisperFullParams params, float[] samples, int numSamples);
    private native int fullWithState(int context, int state, WhisperFullParams params, float[] samples, int numSamples);

    /**
     * Creates a new whisper context.
     *
     * @param model {@link Path} to the whisper ggml model file.
     *
     * @return A new {@link WhisperContext}.
     */
    public WhisperContext init(Path model) {
        return new WhisperContext(this, init(model.toAbsolutePath().toString()));
    }

    /**
     * Creates a new whisper context without state.
     *
     * @param model {@link Path} to the whisper ggml model file.
     *
     * @return A new {@link WhisperContext} without state.
     */
    public WhisperContext initNoState(Path model) {
        return new WhisperContext(this, initNoState(model.toAbsolutePath().toString()));
    }
    /**
     * Creates a new whisper.cpp state for the provided context.
     *
     * @param context {@link WhisperContext} to the whisper ggml model file.
     *
     * @return A new {@link WhisperContext}.
     */
    public WhisperState initState(WhisperContext context) {
        return new WhisperState(this, initState(context.ref), context);
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
        return fullWithState(context.ref, state.ref, params, samples, numSamples);
    }
    private native int fullNSegmentsFromState(int state);

    /**
     * Gets the available number of text segments.
     *
     * @param state the {@link WhisperState} used to transcribe
     *
     * @return available number of segments
     */
    public int fullNSegmentsFromState(WhisperState state) {
        return fullNSegmentsFromState(state.ref);
    }
    private native int fullNSegments(int context);

    /**
     * Gets the available number of text segments.
     *
     * @param context the {@link WhisperContext} used to transcribe
     * @return available number of segments
     */
    public int fullNSegments(WhisperContext context){
        return fullNSegments(context.ref);
    }
    private native String fullGetSegmentText(int context, int index);

    /**
     * Gets text segment by index.
     *
     * @param context a {@link WhisperContext} used to transcribe
     * @param index the segment index
     * @return the segment text
     */
    public String fullGetSegmentText(WhisperContext context, int index) {
        return fullGetSegmentText(context.ref, index);
    }
    private native String fullGetSegmentTextFromState(int state, int index);

    /**
     * Gets text segment by index.
     *
     * @param state a {@link WhisperState} used to transcribe
     * @param index the segment index
     * @return the segment text
     */
    public String fullGetSegmentTextFromState(WhisperState state, int index) {
        return fullGetSegmentTextFromState(state.ref, index);
    }

    /**
     * Release context memory in native implementation.
     *
     * @param context the {@link WhisperContext} to release
     */
    public void free(WhisperContext context) {
        context.close();
    }

    /**
     * Release state memory in native implementation.
     *
     * @param state the {@link WhisperState} to release
     */
    public void free(WhisperState state) {
        state.close();
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
            if(osArch.contains("amd64") || osArch.contains("x86_64")) {
                bundleLibraryPath = "/debian-amd64/libwhisperjni.so";
            } else if(osArch.contains("aarch64") || osArch.contains("arm64")) {
                bundleLibraryPath = "/debian-arm64/libwhisperjni.so";
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
     * In order to avoid sharing pointers between the java and the native implementation we use this
     * util base class and a random integer id generated by the native wrapper.
     */
    protected static abstract class WhisperJNIPointer implements AutoCloseable {
        /**
         * Native pointer reference identifier.
         */
        protected final int ref;
        private boolean released = false;

        /**
         * Creates a new object used to represent a struct pointer on the native library.
         * @param ref a random integer id generated by the native wrapper
         */
        protected WhisperJNIPointer(int ref) {
            this.ref = ref;
        }

        /**
         * Return true after native memory was free
         * @return a boolean indicating if the native data was already released
         */
        protected boolean isReleased() {
            return released;
        }
        public void close() {
            released = true;
        }

    }
}
