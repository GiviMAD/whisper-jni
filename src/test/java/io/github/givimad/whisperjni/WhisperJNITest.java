package io.github.givimad.whisperjni;
import org.junit.Before;
import org.junit.Test;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Path;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

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
    public void testInit() throws IOException {
        var ctx = whisper.init(testModelPath);
        assertNotNull(ctx);
        ctx.close();
    }

    @Test
    public void testInitNoState() throws IOException {
        var ctx = whisper.initNoState(testModelPath);
        assertNotNull(ctx);
        ctx.close();
    }
    @Test
    public void testContextIsMultilingual() throws IOException {
        var ctx = whisper.initNoState(testModelPath);
        assertNotNull(ctx);
        assertTrue(whisper.isMultilingual(ctx));
        ctx.close();
    }
    @Test
    public void testNewState() throws IOException {
        try (var ctx = whisper.initNoState(testModelPath)) {
            assertNotNull(ctx);
            WhisperState state = whisper.initState(ctx);
            assertNotNull(state);
            state.close();
        }
    }
    @Test
    public void testSegmentIndexException() throws IOException {
        var ctx = whisper.init(testModelPath);
        Exception exception = assertThrows(IndexOutOfBoundsException.class, () -> {
            whisper.fullGetSegmentText(ctx, 1);
        });
        ctx.close();
        assertEquals("Index out of range", exception.getMessage());
    }
    @Test
    public void testPointerUnavailableException() throws UnsupportedAudioFileException, IOException {
        var ctx = whisper.init(testModelPath);
        float[] samples = readJFKFileSamples();
        var params = new WhisperFullParams();
        ctx.close();
        Exception exception = assertThrows(RuntimeException.class, () -> {
            whisper.full(ctx, params, samples, samples.length);
        });
        assertEquals("Unavailable pointer, object is closed", exception.getMessage());
    }
    @Test
    public void testFull() throws Exception {
        float[] samples = readJFKFileSamples();
        try (var ctx = whisper.init(testModelPath)) {
            var params = new WhisperFullParams();
            int result = whisper.full(ctx, params, samples, samples.length);
            if(result != 0) {
                throw new RuntimeException("Transcription failed with code " + result);
            }
            int numSegments = whisper.fullNSegments(ctx);
            assertEquals(1, numSegments);
            long startTime = whisper.fullGetSegmentTimestamp0(ctx,0);
            long endTime = whisper.fullGetSegmentTimestamp1(ctx,0);
            String text = whisper.fullGetSegmentText(ctx,0);
            assertEquals(0, startTime);
            assertEquals(1050, endTime);
            assertEquals(" And so my fellow Americans ask not what your country can do for you ask what you can do for your country.", text);
        }
    }

    @Test
    public void testFullBeanSearch() throws Exception {
        float[] samples = readJFKFileSamples();
        try (var ctx = whisper.init(testModelPath)) {
            var params = new WhisperFullParams(WhisperSamplingStrategy.BEAN_SEARCH);
            params.printTimestamps = false;
            int result = whisper.full(ctx, params, samples, samples.length);
            if(result != 0) {
                throw new RuntimeException("Transcription failed with code " + result);
            }
            int numSegments = whisper.fullNSegments(ctx);
            assertEquals(1, numSegments);
            String text = whisper.fullGetSegmentText(ctx,0);
            assertEquals(" And so, my fellow Americans, ask not what your country can do for you, ask what you can do for your country.", text);
        }
    }
    @Test
    public void testFullWithState() throws Exception {
        float[] samples = readJFKFileSamples();
        try (var ctx = whisper.initNoState(testModelPath)) {
            var params = new WhisperFullParams();
            try (var state = whisper.initState(ctx)) {
                int result = whisper.fullWithState(ctx, state, params, samples, samples.length);
                if(result != 0) {
                    throw new RuntimeException("Transcription failed with code " + result);
                }
                int numSegments = whisper.fullNSegmentsFromState(state);
                assertEquals(1, numSegments);
                long startTime = whisper.fullGetSegmentTimestamp0FromState(state,0);
                long endTime = whisper.fullGetSegmentTimestamp1FromState(state,0);
                String text = whisper.fullGetSegmentTextFromState(state,0);
                assertEquals(0, startTime);
                assertEquals(1050, endTime);
                assertEquals(" And so my fellow Americans ask not what your country can do for you ask what you can do for your country.", text);
            }
        }
    }

    @Test
    public void testFullWithStateBeanSearch() throws Exception {
        float[] samples = readJFKFileSamples();
        try (var ctx = whisper.initNoState(testModelPath)) {
            var params = new WhisperFullParams(WhisperSamplingStrategy.BEAN_SEARCH);
            params.printTimestamps = false;
            try (var state = whisper.initState(ctx)) {
                int result = whisper.fullWithState(ctx, state, params, samples, samples.length);
                if(result != 0) {
                    throw new RuntimeException("Transcription failed with code " + result);
                }
                int numSegments = whisper.fullNSegmentsFromState(state);
                assertEquals(1, numSegments);
                String text = whisper.fullGetSegmentTextFromState(state,0);
                assertEquals(" And so, my fellow Americans, ask not what your country can do for you, ask what you can do for your country.", text);
            }
        }
    }

    private float[] readJFKFileSamples() throws UnsupportedAudioFileException, IOException {
        // sample is a 16 bit int 16000hz little endian wav file
        AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(samplePath.toFile());
        // read all the available data to a little endian capture buffer
        ByteBuffer captureBuffer = ByteBuffer.allocate(audioInputStream.available());
        captureBuffer.order(ByteOrder.LITTLE_ENDIAN);
        int read = audioInputStream.read(captureBuffer.array());
        if (read == -1) {
            throw new IOException("Empty file");
        }
        // obtain the 16 int audio samples, short type in java
        var shortBuffer = captureBuffer.asShortBuffer();
        // transform the samples to f32 samples
        float[] samples = new float[captureBuffer.capacity() / 2];
        var i = 0;
        while (shortBuffer.hasRemaining()) {
            samples[i++] = Float.max(-1f, Float.min(((float) shortBuffer.get()) / (float) Short.MAX_VALUE, 1f));
        }
        return samples;
    }

}
