package io.github.givimad.whisperjni;

public class WhisperFullParams {
    public WhisperFullParams(WhisperSamplingStrategy strategy) {
        this.strategy = strategy.ordinal();
    }
    public WhisperFullParams() {
        this(WhisperSamplingStrategy.GREEDY);
    }
    /**
     * Max tokens to use from past text as prompt for the decoder
     */
    public final int strategy;
    /**
     * Overwrite the audio context size (0 = use default)
     */
    public int audioCtx;
    /**
     * Max tokens to use from past text as prompt for the decoder
     */
    public int nMaxTextCtx = 16384;
    /**
     * Start offset in ms
     */
    public int offsetMs;
    /**
     * Audio duration to process in ms
     */
    public int durationMs;
    /**
     * Translate
     */
    public boolean translate;
    /**
     * do not use past transcription (if any) as initial prompt for the decoder
     */
    public boolean noContext = true;
    /**
     * force single segment output (useful for streaming)
     */
    public boolean singleSegment;
    /**
     * Print special tokens
     */
    public boolean printSpecial;
    /**
     * Print progress information
     */
    public boolean printProgress = true;
    /**
     * Print results from within whisper.cpp (avoid it, use callback instead)
     */
    public boolean printRealtime;
    /**
     * Print timestamps for each text segment when printing realtime
     */
    public boolean printTimestamps = true;
}
