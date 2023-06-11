set -x
set -e
javac -h src/main/native src/main/java/io/github/givimad/whisperjni/NativeUtils.java src/main/java/io/github/givimad/whisperjni/WhisperContext.java src/main/java/io/github/givimad/whisperjni/WhisperState.java src/main/java/io/github/givimad/whisperjni/WhisperJNI.java
