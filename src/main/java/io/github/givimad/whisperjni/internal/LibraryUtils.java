package io.github.givimad.whisperjni.internal;

import io.github.givimad.whisperjni.WhisperJNI;

import java.io.*;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.ProviderNotFoundException;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.function.Consumer;


public class LibraryUtils {
    private static final String TEMP_FOLDER_PREFIX = "whisper-jni-";
    private static Path libraryDir = null;
    private LibraryUtils() {
    }
    private static void createLibraryFromInputStream(String filename, InputStream is) throws IOException {
        Path libraryPath = libraryDir.resolve(filename);
        try (is) {
            Files.copy(is, libraryPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            try {
                Files.delete(libraryPath);
            } catch (IOException ignored) {}
            throw e;
        } catch (NullPointerException e) {
            try {
                Files.delete(libraryPath);
            } catch (IOException ignored) {}
            throw new FileNotFoundException("File" + libraryPath + "not found.");
        }
        libraryPath.toFile().deleteOnExit();
    }
    /**
     * Loads library from current JAR archive
     *
     * The file from JAR is copied into system temporary directory and then loaded. The temporary file is deleted after
     * exiting.
     * Method uses String as filename because the pathname is "abstract", not system-dependent.
     *
     * @param path The path of file inside JAR as absolute path (beginning with '/'), e.g. /package/File.ext
     * @throws IOException If temporary file creation or read/write operation fails
     * @throws IllegalArgumentException If source file (param path) does not exist
     * @throws IllegalArgumentException If the path is not absolute or if the filename is shorter than three characters
     * (restriction of {@link File#createTempFile(java.lang.String, java.lang.String)}).
     * @throws FileNotFoundException If the file could not be found inside the JAR.
     */
    public static void copyFromSystem(Path path, String filename, Consumer<String> logger) throws IOException {
        if(libraryDir == null) {
            libraryDir = createTempDirectory(TEMP_FOLDER_PREFIX);
        }
        if (null == path) {
            throw new IllegalArgumentException("Missing path.");
        }
        if(logger != null) {
            logger.accept("Extracting "+ path + " into " + libraryDir.resolve(filename));
        }
        try (var is = Files.newInputStream(path)) {
            createLibraryFromInputStream(filename, is);
        }
    }
    /**
     * Loads library from current JAR archive
     *
     * The file from JAR is copied into system temporary directory and then loaded. The temporary file is deleted after
     * exiting.
     * Method uses String as filename because the pathname is "abstract", not system-dependent.
     *
     * @param path The path of file inside JAR as absolute path (beginning with '/'), e.g. /package/File.ext
     * @throws IOException If temporary file creation or read/write operation fails
     * @throws IllegalArgumentException If source file (param path) does not exist
     * @throws IllegalArgumentException If the path is not absolute or if the filename is shorter than three characters
     * (restriction of {@link File#createTempFile(java.lang.String, java.lang.String)}).
     * @throws FileNotFoundException If the file could not be found inside the JAR.
     */
    public static void extractLibraryFromJar(String path, String filename, Consumer<String> logger) throws IOException {
        if(libraryDir == null) {
            libraryDir = createTempDirectory(TEMP_FOLDER_PREFIX);
        }
        if (null == path || !path.startsWith("/")) {
            throw new IllegalArgumentException("The path has to be absolute (start with '/').");
        }
        if(logger != null) {
            logger.accept("Extracting "+ path + " into " + libraryDir.resolve(filename));
        }
        createLibraryFromInputStream(filename, LibraryUtils.class.getResourceAsStream(path));
    }
    public static void load(String filename) {
        System.load(libraryDir.resolve(filename).toAbsolutePath().toString());
    }
    private static Path createTempDirectory(String prefix) throws IOException {
        String tempDir = System.getProperty("java.io.tmpdir");
        File generatedDir = new File(tempDir, prefix + System.nanoTime());
        if (!generatedDir.mkdir())
            throw new IOException("Failed to create temp directory " + generatedDir.getName());
        return Paths.get(generatedDir.getAbsolutePath());
    }

    /**
     * Register the native library, should be called at first.
     * @throws IOException when unable to load the native library
     */
    public static void loadLibrary(WhisperJNI.LoadOptions options) throws IOException {
        String wrapperLibName;
        String osName = System.getProperty("os.name").toLowerCase();
        String osArch = System.getProperty("os.arch").toLowerCase();
        if (osName.contains("win")) {
            wrapperLibName = "whisperjni.dll";
            if(osArch.contains("amd64") || osArch.contains("x86_64")) {
                if(options.whisperJNILib == null){
                    options.logger.accept("Looking for whisper.dll in $env:PATH.");
                    if(isWhisperDLLInstalled()) {
                        LibraryUtils.extractLibraryFromJar("/win-amd64/whisperjni.dll", "whisperjni.dll", options.logger);
                    } else {
                        LibraryUtils.extractLibraryFromJar("/win-amd64/whisperjni_full.dll", "whisperjni.dll", options.logger);
                    }
                } else {
                    LibraryUtils.copyFromSystem(options.whisperJNILib, "whisperjni.dll", options.logger);
                }
            }
        } else if (osName.contains("nix") || osName.contains("nux")
                || osName.contains("aix")) {
            wrapperLibName = "libwhisperjni.so";
            String cpuInfo;
            try {
                cpuInfo = Files.readString(Path.of("/proc/cpuinfo"));
            } catch (IOException ignored) {
                cpuInfo = "";
            }
            if(osArch.contains("amd64") || osArch.contains("x86_64")) {
                if(options.whisperLib == null) {
                    if(cpuInfo.contains("avx2") && cpuInfo.contains("fma") && cpuInfo.contains("f16c") && cpuInfo.contains("avx")) {
                        LibraryUtils.extractLibraryFromJar("/debian-amd64/libwhisper+mf16c+mfma+mavx+mavx2.so", "libwhisper.so", options.logger);
                    } else {
                        LibraryUtils.extractLibraryFromJar("/debian-amd64/libwhisper.so", "libwhisper.so", options.logger);
                    }
                } else {
                    LibraryUtils.copyFromSystem(options.whisperLib, "libwhisper.so", options.logger);
                }
                if(options.whisperJNILib == null){
                    LibraryUtils.extractLibraryFromJar("/debian-amd64/libwhisperjni.so", "libwhisperjni.so", options.logger);
                } else {
                    LibraryUtils.copyFromSystem(options.whisperJNILib, "libwhisperjni.so", options.logger);
                }
            } else if(osArch.contains("aarch64") || osArch.contains("arm64")) {
                if(options.whisperLib == null){
                    if(cpuInfo.contains("fphp")) {
                        LibraryUtils.extractLibraryFromJar("/debian-arm64/libwhisper+fp16.so", "libwhisper.so", options.logger);
                    } else if(cpuInfo.contains("crc32")) {
                        LibraryUtils.extractLibraryFromJar( "/debian-arm64/libwhisper+crc.so", "libwhisper.so", options.logger);
                    }
                } else {
                    LibraryUtils.copyFromSystem(options.whisperLib, "libwhisper.so", options.logger);
                }
                if(options.whisperJNILib == null){
                    LibraryUtils.extractLibraryFromJar("/debian-arm64/libwhisperjni.so", "libwhisperjni.so", options.logger);
                } else {
                    LibraryUtils.copyFromSystem(options.whisperJNILib, "libwhisperjni.so", options.logger);
                }
            } else if(osArch.contains("armv7") || osArch.contains("arm")) {
                if(options.whisperLib == null){
                    if(cpuInfo.contains("crc32")) {
                        LibraryUtils.extractLibraryFromJar("/debian-armv7l/libwhisper+crc.so", "libwhisper.so", options.logger);
                    } else {
                        LibraryUtils.extractLibraryFromJar("/debian-armv7l/libwhisper.so", "libwhisper.so", options.logger);
                    }
                } else {
                    LibraryUtils.copyFromSystem(options.whisperLib, "libwhisper.so", options.logger);
                }
                if(options.whisperJNILib == null){
                    LibraryUtils.extractLibraryFromJar("/debian-armv7l/libwhisperjni.so", "libwhisperjni.so", options.logger);
                } else {
                    LibraryUtils.copyFromSystem(options.whisperJNILib, "libwhisperjni.so", options.logger);
                }
            }
        } else if (osName.contains("mac") || osName.contains("darwin")) {
            wrapperLibName = "libwhisperjni.dylib";
            if(osArch.contains("amd64") || osArch.contains("x86_64")) {
                if(options.whisperLib == null){
                    LibraryUtils.extractLibraryFromJar( "/macos-amd64/libwhisper.dylib", "libwhisper.dylib", options.logger);
                } else {
                    LibraryUtils.copyFromSystem(options.whisperLib, "libwhisper.dylib", options.logger);
                }
                if(options.whisperJNILib == null){
                    LibraryUtils.extractLibraryFromJar("/macos-amd64/libwhisperjni.dylib", "libwhisperjni.dylib", options.logger);
                } else {
                    LibraryUtils.copyFromSystem(options.whisperJNILib, "libwhisperjni.dylib", options.logger);
                }
            } else if(osArch.contains("aarch64") || osArch.contains("arm64")) {
                if(options.whisperLib == null){
                    LibraryUtils.extractLibraryFromJar( "/macos-arm64/libwhisper.dylib", "libwhisper.dylib", options.logger);
                } else {
                    LibraryUtils.copyFromSystem(options.whisperLib, "libwhisper.dylib", options.logger);
                }
                if(options.whisperJNILib == null){
                    LibraryUtils.extractLibraryFromJar("/macos-arm64/libwhisperjni.dylib", "libwhisperjni.dylib", options.logger);
                } else {
                    LibraryUtils.copyFromSystem(options.whisperJNILib, "libwhisperjni.dylib", options.logger);
                }
            }
        } else {
            throw new IOException("Unknown OS");
        }
        LibraryUtils.load(wrapperLibName);
    }
    private static boolean isWhisperDLLInstalled() {
        return Arrays
                .stream(System.getenv("PATH").split(";"))
                .map(Paths::get)
                .map(p -> p.resolve("whisper.dll"))
                .anyMatch(Files::exists);
    }
}