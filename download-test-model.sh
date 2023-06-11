MODEL_NAME=tiny
./src/main/native/whisper/models/download-ggml-model.sh $MODEL_NAME
mv ./src/main/native/whisper/models/ggml-$MODEL_NAME.bin ./