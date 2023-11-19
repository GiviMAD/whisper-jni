package io.github.givimad.whisperjni;

/**
 * The {@link WhisperGrammar} class represents a native whisper.cpp parsed grammar.
 *
 * You need to dispose the native memory for its instances by calling {@link #close}
 *
 * @author Miguel Álvarez Díez - Initial contribution
 */
public class WhisperGrammar extends WhisperJNI.WhisperJNIPointer {
    private final WhisperJNI whisper;
    private final String grammarText;

    /**
     * Internal context constructor
     * @param whisper library instance
     * @param ref native pointer identifier
     */
    protected WhisperGrammar(WhisperJNI whisper, int ref, String text) {
        super(ref);
        this.whisper = whisper;
        this.grammarText = text;
    }
    @Override
    public void close() {
        whisper.free(this);
    }
}
