package io.github.givimad.whisperjni;
import org.junit.Before;
import org.junit.Test;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import java.io.IOException;
import java.nio.file.Path;

import static junit.framework.TestCase.assertEquals;

public class WhisperJNITest {
    Path testModelPath = Path.of("ggml-tiny.bin");
    Path samplePath = Path.of("src/main/native/whisper/samples/jfk.wav");
    WhisperJNI whisper;

    @Before
    public void before() throws IOException {
        var modelFile = testModelPath.toFile();
        var sampleFile = samplePath.toFile();
        if(!modelFile.exists() || !modelFile.isFile()) {
            throw new RuntimeException("Missing model file: " + testModelPath.toAbsolutePath().toString());
        }
        if(!sampleFile.exists() || !sampleFile.isFile()) {
            throw new RuntimeException("Missing sample file");
        }
        WhisperJNI.loadLibrary();
        whisper = new WhisperJNI();
    }

    @Test
    public void testInit() throws Exception {
        var ctx = whisper.init(testModelPath);
        ctx.close();
    }

    @Test
    public void testInitNoState() throws Exception {
        var ctx = whisper.initNoState(testModelPath);
        ctx.close();
    }

    @Test
    public void testNewState() throws Exception {
        var ctx = whisper.initNoState(testModelPath);
        try {
            WhisperState state = whisper.initState(ctx);
            state.close();
        } finally {
            ctx.close();
        }
    }
    @Test
    public void testFull() throws Exception {
        float[] samples = readJFKFileSamples();
        var ctx = whisper.init(testModelPath);
        var params = new WhisperFullParams();
        int result = whisper.full(ctx, params, samples, samples.length);
        if(result != 0) {
            throw new RuntimeException("Transcription failed with code " + result);
        }
        int numSegments = whisper.fullNSegments(ctx);
        assertEquals(1, numSegments);
        String text = whisper.fullGetSegmentText(ctx,0);
        assertEquals(" And so my fellow Americans ask not what your country can do for you ask what you can do for your country.", text);
        ctx.close();
    }
    @Test
    public void testFullwithState() throws Exception {
        float[] samples = readJFKFileSamples();
        var ctx = whisper.initNoState(testModelPath);
        var params = new WhisperFullParams();
        var state = whisper.initState(ctx);
        int result = whisper.fullWithState(ctx, state, params, samples, samples.length);
        if(result != 0) {
            throw new RuntimeException("Transcription failed with code " + result);
        }
        int numSegments = whisper.fullNSegmentsFromState(state);
        assertEquals(1, numSegments);
        String text = whisper.fullGetSegmentTextFromState(state,0);
        assertEquals(" And so my fellow Americans ask not what your country can do for you ask what you can do for your country.", text);
        state.close();
        ctx.close();
    }

    private float[] readJFKFileSamples() throws UnsupportedAudioFileException, IOException {
        AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(samplePath.toFile());
        byte[] b = new byte[audioInputStream.available()];
        float[] samples = new float[b.length / 2];
        audioInputStream.read(b);
        for (int i = 0, j = 0; i < b.length; i += 2, j++) {
            int intSample = (int) (b[i + 1]) << 8 | (int) (b[i]) & 0xFF;
            samples[j] = intSample / 32767.0f;
        }
        return samples;
    }

}
