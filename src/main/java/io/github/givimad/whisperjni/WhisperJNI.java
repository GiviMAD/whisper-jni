package io.github.givimad.whisperjni;

import java.io.IOException;
import java.nio.file.Path;

public class WhisperJNI {
    private native int init(String model);

    public WhisperContext init(Path model) {
        return new WhisperContext(this, init(model.toAbsolutePath().toString()));
    }
    private native int initNoState(String model);
    public WhisperContext initNoState(Path model) {
        return new WhisperContext(this, initNoState(model.toAbsolutePath().toString()));
    }
    protected native void freeContext(int ref);
    private native int initState(int model);
    public WhisperState initState(WhisperContext context) {
        return new WhisperState(this, initState(context.ref), context);
    }
    protected native void freeState(int state);

    private native int full(int context, WhisperFullParams params, float[] samples, int numSamples);

    public int full(WhisperContext context, WhisperFullParams params, float[] samples, int numSamples) {
        return full(context.ref, params, samples, numSamples);
    }

    private native int fullWithState(int context, int state, WhisperFullParams params, float[] samples, int numSamples);
    public int fullWithState(WhisperContext context, WhisperState state, WhisperFullParams params, float[] samples, int numSamples) {
        return fullWithState(context.ref, state.ref, params, samples, numSamples);
    }
    private native int fullNSegmentsFromState(int state);
    public int fullNSegmentsFromState(WhisperState state) {
        return fullNSegmentsFromState(state.ref);
    }
    private native int fullNSegments(int context);
    public int fullNSegments(WhisperContext context){
        return fullNSegments(context.ref);
    }
    private native String fullGetSegmentText(int context, int index);
    public String fullGetSegmentText(WhisperContext context, int index) {
        return fullGetSegmentText(context.ref, index);
    }
    private native String fullGetSegmentTextFromState(int state, int index);
    public String fullGetSegmentTextFromState(WhisperState state, int index) {
        return fullGetSegmentTextFromState(state.ref, index);
    }
    public static void loadLibrary() throws IOException {
        String bundleLibraryPath = null;
        String osName = System.getProperty("os.name").toLowerCase();
        String osArch = System.getProperty("os.arch").toLowerCase();
        if (osName.contains("win")) {
            if(osArch.contains("amd64") || osArch.contains("x86_64")) {
                bundleLibraryPath = "/win/libwhisperjni.dll";
            }
        } else if (osName.contains("nix") || osName.contains("nux")
                || osName.contains("aix")) {
            if(osArch.contains("amd64") || osArch.contains("x86_64")) {
                bundleLibraryPath = "/debian-amd64/libwhisperjni.so";
            } else if(osArch.contains("aarch64") || osArch.contains("arm64")) {
                bundleLibraryPath = "/debian-aarch64/libwhisperjni.so";
            } else if(osArch.contains("armv7") || osArch.contains("arm")) {
                bundleLibraryPath = "/debian-armv7l/libwhisperjni.so";
            }
        } else if (osName.contains("mac") || osName.contains("darwin")) {
            if(osArch.contains("amd64") || osArch.contains("x86_64")) {
                bundleLibraryPath = "/macos-x86-64/libwhisperjni.dylib";
            } else if(osArch.contains("aarch64") || osArch.contains("arm64")) {
                // bundleLibraryPath ="/macos-arm64/libwhisperjni.dylib";
            }
        }
        if (bundleLibraryPath == null) {
            throw new java.io.IOException("WhisperJNI: Unsupported platform " + osName + " - " + osArch);
        }
        NativeUtils.loadLibraryFromJar(bundleLibraryPath);
    }
}
