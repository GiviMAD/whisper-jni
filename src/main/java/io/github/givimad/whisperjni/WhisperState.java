package io.github.givimad.whisperjni;

public class WhisperState implements AutoCloseable {
    protected final WhisperContext context;
    protected final int ref;
    private final WhisperJNI whisper;

    protected WhisperState(WhisperJNI whisper, int ref, WhisperContext context) {
        this.whisper = whisper;
        this.context = context;
        this.ref = ref;
    }

    @Override
    public void close() throws Exception {
        whisper.freeState(ref);
    }
}
