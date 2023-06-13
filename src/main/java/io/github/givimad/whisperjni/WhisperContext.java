package io.github.givimad.whisperjni;

/**
 * The {@link WhisperContext} class represents a native whisper.cpp context.
 *
 * You need to dispose the native memory for its instances by calling {@link #close}
 * or {@link WhisperJNI#free(WhisperContext)}
 *
 * @author Miguel Álvarez Díez - Initial contribution
 */
public class WhisperContext extends WhisperJNI.WhisperJNIPointer {
    private final WhisperJNI whisper;

    /**
     * Internal context constructor
     * @param whisper library instance
     * @param ref native pointer identifier
     */
    protected WhisperContext(WhisperJNI whisper, int ref) {
        super(ref);
        this.whisper = whisper;
    }
    @Override
    public void close() {
        whisper.free(this);
    }
}
