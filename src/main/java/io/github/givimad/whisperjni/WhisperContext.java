package io.github.givimad.whisperjni;

/**
 * The {@link WhisperContext} class represents a native whisper.cpp context.
 *
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
        if(isReleased()) {
            return;
        }
        super.close();
        whisper.freeContext(ref);
    }
}
