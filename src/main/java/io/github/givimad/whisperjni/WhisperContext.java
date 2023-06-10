package io.github.givimad.whisperjni;

public class WhisperContext implements AutoCloseable {
    protected final int ref;
    private final WhisperJNI whisper;

    protected WhisperContext(WhisperJNI whisper, int ref) {
        this.whisper = whisper;
        this.ref = ref;
    }

    @Override
    public void close() throws Exception {
        whisper.freeContext(ref);
    }
}
