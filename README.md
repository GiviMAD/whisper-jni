# WhisperJNI

A JNI wrapper for [whisper.cpp](https://github.com/ggerganov/whisper.cpp).

Example:
```
        var whisper = new WhisperJNI();
        whisper.loadLibrary();
        float[] samples = readJFKFileSamples();
        var ctx = whisper.init(Path.of(System.getProperty("user.home"), 'ggml-tiny.bin'));
        int result = whisper.full(ctx, samples, samples.length);
        if(result != 0) {
            throw new RuntimeException("Transcription failed with code " + result);
        }
        int numSegments = whisper.fullNSegments(ctx);
        assertEquals(1, numSegments);
        String text = whisper.fullGetSegmentText(ctx,0);
        assertEquals(" And so my fellow Americans ask not what your country can do for you ask what you can do for your country.", text);
        ctx.close();
```
Example with state:
```
        var whisper = new WhisperJNI();
        whisper.loadLibrary();
        float[] samples = readJFKFileSamples();
        var ctx = whisper.initNoState(Path.of(System.getProperty("user.home"), 'ggml-tiny.bin'));

        var state = whisper.initState(ctx);
        int result = whisper.fullWithState(ctx, state, samples, samples.length);
        if(result != 0) {
            throw new RuntimeException("Transcription failed with code " + result);
        }
        int numSegments = whisper.fullNSegmentsFromState(state);
        assertEquals(1, numSegments);
        String text = whisper.fullGetSegmentTextFromState(state,0);
        assertEquals(" And so my fellow Americans ask not what your country can do for you ask what you can do for your country.", text);
        state.close();
        ctx.close();
```