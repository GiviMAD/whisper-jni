package io.github.givimad.whisperjni;

import io.github.givimad.whisperjni.internal.LibraryUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * The {@link WhisperJNI} class allows to use whisper.cpp thought the JNI.
 *
 * @author Miguel Álvarez Díez - Initial contribution
 */
public class WhisperJNI {
    private static boolean libraryLoaded;
    private static LibraryLogger libraryLogger;

    //region native api
    private native int init(String model, WhisperContextParams params);

    private native int initNoState(String model, WhisperContextParams params);

    private native int initState(int model);

    private native int loadGrammar(String text);

    private native void initOpenVINOEncoder(int model, String device);

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

    private native void freeGrammar(int grammar);

    private native String printSystemInfo();

    private native static void setLogger(boolean enabled);

//endregion

    /**
     * Creates a new whisper context.
     *
     * @param model {@link Path} to the whisper ggml model file.
     * @return A new {@link WhisperContext}.
     * @throws IOException if model file is missing.
     */
    public WhisperContext init(Path model) throws IOException {
        return init(model, null);
    }

    /**
     * Creates a new whisper context.
     *
     * @param model  {@link Path} to the whisper ggml model file.
     * @param params {@link WhisperContextParams} params for context initialization.
     * @return A new {@link WhisperContext}.
     * @throws IOException if model file is missing.
     */
    public WhisperContext init(Path model, WhisperContextParams params) throws IOException {
        assertModelExists(model);
        if(params == null) {
            params = new WhisperContextParams();
        }
        int ref = init(model.toAbsolutePath().toString(), params);
        if(ref == -1) {
            return null;
        }
        return new WhisperContext(this, ref);
    }

    /**
     * Creates a new whisper context without state.
     *
     * @param model {@link Path} to the whisper ggml model file.
     * @return A new {@link WhisperContext} without state.
     * @throws IOException if model file is missing.
     */
    public WhisperContext initNoState(Path model) throws IOException {
        return initNoState(model, null);
    }

    /**
     * Creates a new whisper context without state.
     *
     * @param model  {@link Path} to the whisper ggml model file.
     * @param params {@link WhisperContextParams} params for context initialization.
     * @return A new {@link WhisperContext} without state.
     * @throws IOException if model file is missing.
     */
    public WhisperContext initNoState(Path model, WhisperContextParams params) throws IOException {
        assertModelExists(model);
        if(params == null) {
            params = new WhisperContextParams();
        }
        int ref = initNoState(model.toAbsolutePath().toString(), params);
        if(ref == -1) {
            return null;
        }
        return new WhisperContext(this, ref);
    }

    /**
     * Creates a new whisper.cpp state for the provided context.
     *
     * @param context the {@link WhisperContext} of this state.
     * @return A new {@link WhisperContext}.
     */
    public WhisperState initState(WhisperContext context) {
        WhisperJNIPointer.assertAvailable(context);
        int ref = initState(context.ref);
        if(ref == -1) {
            return null;
        }
        return new WhisperState(this, ref, context);
    }

    public WhisperGrammar parseGrammar(Path grammarPath) throws IOException {
        if(!Files.exists(grammarPath) || Files.isDirectory(grammarPath)){
            throw new FileNotFoundException("Grammar file not found");
        }
        return parseGrammar(Files.readString(grammarPath));
    }

    public WhisperGrammar parseGrammar(String text) throws IOException {
        if(text.isBlank()) {
            throw new IOException("Grammar text is blank");
        }
        int ref = loadGrammar(text);
        if(ref == -1) {
            return null;
        }
        return new WhisperGrammar(this, ref, text);
    }

    /**
     * Initializes OpenVino encoder.
     *
     * @param context a {@link WhisperContext} instance.
     * @param device the device name.
     */
    public void initOpenVINO(WhisperContext context, String device) {
        WhisperJNIPointer.assertAvailable(context);
        initOpenVINOEncoder(context.ref, device);
    }

    /**
     * Is multilingual.
     *
     * @param context the {@link WhisperContext} to check.
     * @return true if model support multiple languages
     */
    public boolean isMultilingual(WhisperContext context) {
        WhisperJNIPointer.assertAvailable(context);
        return isMultilingual(context.ref);
    }

    /**
     * Run whisper.cpp full audio transcription.
     *
     * @param context    the {@link WhisperContext} used to transcribe.
     * @param params     a {@link WhisperFullParams} instance with the desired configuration.
     * @param samples    the audio samples (f32 encoded samples with sample rate 16000).
     * @param numSamples the number of audio samples provided.
     * @return a result code, values other than 0 indicates problems.
     */
    public int full(WhisperContext context, WhisperFullParams params, float[] samples, int numSamples) {
        WhisperJNIPointer.assertAvailable(context);
        if(params.grammar != null) {
            WhisperJNIPointer.assertAvailable(params.grammar);
        }
        return full(context.ref, params, samples, numSamples);
    }

    /**
     * Run whisper.cpp full audio transcription.
     *
     * @param context    the {@link WhisperContext} used to transcribe.
     * @param state      the {@link WhisperState} used to transcribe.
     * @param params     a {@link WhisperFullParams} instance with the desired configuration.
     * @param samples    the audio samples (f32 encoded samples with sample rate 16000).
     * @param numSamples the number of audio samples provided.
     * @return a result code, values other than 0 indicates problems.
     */
    public int fullWithState(WhisperContext context, WhisperState state, WhisperFullParams params, float[] samples, int numSamples) {
        WhisperJNIPointer.assertAvailable(context);
        WhisperJNIPointer.assertAvailable(state);
        if(params.grammar != null) {
            WhisperJNIPointer.assertAvailable(params.grammar);
        }
        return fullWithState(context.ref, state.ref, params, samples, numSamples);
    }

    /**
     * Gets the available number of text segments.
     *
     * @param state the {@link WhisperState} used to transcribe
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
    public int fullNSegments(WhisperContext context) {
        WhisperJNIPointer.assertAvailable(context);
        return fullNSegments(context.ref);
    }

    /**
     * Gets start timestamp of text segment by index.
     *
     * @param context a {@link WhisperContext} used to transcribe
     * @param index   the segment index
     * @return start timestamp of segment text, 800 -> 8s
     */
    public long fullGetSegmentTimestamp0(WhisperContext context, int index) {
        WhisperJNIPointer.assertAvailable(context);
        return fullGetSegmentTimestamp0(context.ref, index);
    }

    /**
     * Gets end timestamp of text segment by index.
     *
     * @param context a {@link WhisperContext} used to transcribe
     * @param index   the segment index
     * @return end timestamp of segment text, 1050 -> 10.5s
     */
    public long fullGetSegmentTimestamp1(WhisperContext context, int index) {
        WhisperJNIPointer.assertAvailable(context);
        return fullGetSegmentTimestamp1(context.ref, index);
    }

    /**
     * Gets text segment by index.
     *
     * @param context a {@link WhisperContext} used to transcribe
     * @param index   the segment index
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
     * @return start timestamp of segment text, 1050 -> 10.5s
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
     * @return end timestamp of segment text, 1050 -> 10.5s
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
        freeContext(context.ref);
        context.release();
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
        freeState(state.ref);
        state.release();
    }

    /**
     * Release grammar memory in native implementation.
     *
     * @param grammar the {@link WhisperGrammar} to release
     */
    public void free(WhisperGrammar grammar) {
        if (grammar.isReleased()) {
            return;
        }
        freeGrammar(grammar.ref);
        grammar.release();
    }

    /**
     * Get whisper.cpp system info stream, to check enabled features in whisper.
     *
     * @return the whisper.cpp system info stream.
     */
    public String getSystemInfo() {
        return printSystemInfo();
    }

    /**
     * Register the native library, should be called at first.
     *
     * @throws IOException when unable to load the native library
     */
    public static void loadLibrary() throws IOException {
        loadLibrary(null);
    }

    /**
     * Register the native library, should be called at first.
     *
     * @param options instance of {@link LoadOptions} to customize library load.
     * @throws IOException when unable to load the native library.
     */
    public static void loadLibrary(LoadOptions options) throws IOException {
        if (libraryLoaded) {
            return;
        }
        if (options == null) {
            options = new LoadOptions();
        }
        if(options.logger == null) {
            options.logger = (String ignored) -> { };
        }
        LibraryUtils.loadLibrary(options);
        libraryLoaded = true;
    }

    /**
     * Proxy whisper.cpp logger.
     * Should be called after {@link #loadLibrary()}.
     *
     * @param logger whisper.cpp log consumer, or null to disable the library default log to stderr.
     */
    public static void setLibraryLogger(LibraryLogger logger) {
        libraryLogger = logger;
        setLogger(libraryLogger != null);
    }

    /**
     * The class {@link WhisperJNI.LibraryLogger} allows to proxy the whisper.cpp logger.
     *
     * @author Miguel Álvarez Díez - Initial contribution
     */
    public interface LibraryLogger {
        void log(String text);
    }

    /**
     * The class {@link LoadOptions} allows to customize the load of the required shared libraries.
     *
     * @author Miguel Álvarez Díez - Initial contribution
     */
    public static class LoadOptions {
        /**
         * Logs the library registration process (platform detection and library extraction).
         */
        public LibraryLogger logger;
        /**
         * Path to whisper jni library (so/dll/dylib).
         * Takes prevalence over the bundled binary.
         */
        public Path whisperJNILib;
        /**
         * Path to whisper library (so/dylib).
         * Takes prevalence over the bundled binary.
         * Only works on Linux and macOS.
         * On windows the library search for the whisper.dll in the $env:PATH directories.
         */
        public Path whisperLib;
    }

    /**
     * Called from the cpp side of the library to proxy the whisper.cpp logs.
     *
     * @param text whisper.cpp log line.
     */
    protected static void log(String text) {
        if (libraryLogger != null) {
            libraryLogger.log(text);
        }
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
         *
         * @param ref a random integer id generated by the native wrapper
         */
        protected WhisperJNIPointer(int ref) {
            this.ref = ref;
        }

        /**
         * Return true if native memory is free
         *
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

    private static void assertModelExists(Path model) throws IOException {
        if (!Files.exists(model) || Files.isDirectory(model)) {
            throw new IOException("Missing model file: " + model);
        }
    }
}
