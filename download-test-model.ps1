$env:MODEL_NAME = 'tiny'
.\src\main\native\whisper\models\download-ggml-model.cmd $env:MODEL_NAME
mv .\src\main\native\whisper\models\ggml-$env:MODEL_NAME.bin .\