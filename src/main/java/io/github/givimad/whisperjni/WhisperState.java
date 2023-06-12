package io.github.givimad.whisperjni;


/**
 * The {@link WhisperState} represents a whisper_state, useful for thread safe mode sharing.
 *
 * @author Miguel Álvarez Díez - Initial contribution
 */
public class WhisperState extends WhisperJNI.WhisperJNIPointer {
    private final WhisperContext context;
    private final WhisperJNI whisper;

    /**
     * The internal constructor for {@link WhisperState}
     *
     * @param whisper whisper lib instance
     * @param ref native pointer reference identifier
     * @param context parent {@link WhisperContext}
     */
    protected WhisperState(WhisperJNI whisper, int ref, WhisperContext context) {
        super(ref);
        this.whisper = whisper;
        this.context = context;
    }
    @Override
    public void close() {
        if(isReleased()) {
            return;
        }
        super.close();
        whisper.freeState(ref);
    }
}
