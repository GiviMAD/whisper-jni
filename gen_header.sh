set -xe

LIB_SRC=src/main/java/io/github/givimad/whisperjni
javac -h src/main/native \
$LIB_SRC/internal/LibraryUtils.java \
$LIB_SRC/WhisperContextParams.java \
$LIB_SRC/WhisperContext.java \
$LIB_SRC/WhisperGrammar.java \
$LIB_SRC/WhisperSamplingStrategy.java \
$LIB_SRC/WhisperFullParams.java \
$LIB_SRC/WhisperState.java \
$LIB_SRC/WhisperJNI.java

rm -rf $LIB_SRC/*.class $LIB_SRC/internal/*.class
