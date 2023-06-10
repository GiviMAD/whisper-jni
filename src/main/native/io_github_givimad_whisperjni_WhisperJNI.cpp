#include<iostream>
#include<jni.h>
#include "io_github_givimad_whisperjni_WhisperJNI.h"
#include "./whisper/whisper.cpp"
#include <map>

std::map<int, whisper_context *> context_map;
std::map<int, whisper_state *> state_map;

int insertModel(whisper_context * ctx) {
    int ref = rand();
    context_map.insert({ref, ctx});
    return ref;
}

JNIEXPORT jint JNICALL Java_io_github_givimad_whisperjni_WhisperJNI_init
  (JNIEnv * env, jobject thisObject, jstring modelPath) {
    const char* path = env->GetStringUTFChars(modelPath, NULL);
    return insertModel(whisper_init_from_file(path));

  }

JNIEXPORT jint JNICALL Java_io_github_givimad_whisperjni_WhisperJNI_initNoState
  (JNIEnv * env, jobject thisObject, jstring modelPath) {
    const char* path = env->GetStringUTFChars(modelPath, NULL);
    return insertModel(whisper_init_from_file_no_state(path));
  }

  JNIEXPORT void JNICALL Java_io_github_givimad_whisperjni_WhisperJNI_freeContext
    (JNIEnv * env, jobject thisObject, jint ctxRef){
        whisper_context * whisper_ctx = context_map.at(ctxRef);
        if(whisper_ctx != nullptr) {
            context_map.erase(ctxRef);
            whisper_free(whisper_ctx);
            whisper_ctx = nullptr;
        }
  }


 JNIEXPORT jint JNICALL Java_io_github_givimad_whisperjni_WhisperJNI_initState
   (JNIEnv * env, jobject thisObject, jint ctxRef){
    whisper_context * whisper_ctx = context_map.at(ctxRef);
    if (!whisper_ctx) {
        return -1;
    }
    int stateRef = rand();
    whisper_state * state = whisper_init_state(whisper_ctx);
    state_map.insert({stateRef, state});
    return stateRef;
 }

 JNIEXPORT void JNICALL Java_io_github_givimad_whisperjni_WhisperJNI_freeState
   (JNIEnv * env, jobject thisObject, jint stateIndex) {
    whisper_state * state = state_map.at(stateIndex);
    state_map.erase(stateIndex);
    whisper_free_state(state);
 }

JNIEXPORT jint JNICALL Java_io_github_givimad_whisperjni_WhisperJNI_full
  (JNIEnv * env, jobject thisObject, jint ctxRef, jfloatArray samples, jint numSamples) {
    const float* samplesPointer = env->GetFloatArrayElements(samples, NULL);
    struct whisper_full_params params = whisper_full_default_params(whisper_sampling_strategy::WHISPER_SAMPLING_GREEDY);
    return whisper_full(context_map.at(ctxRef), params, samplesPointer, numSamples);
}

JNIEXPORT jint JNICALL Java_io_github_givimad_whisperjni_WhisperJNI_fullWithState
  (JNIEnv * env, jobject thisObject, jint ctxRef, jint stateRef, jfloatArray samples, jint numSamples) {
    const float* samplesPointer = env->GetFloatArrayElements(samples, NULL);
    struct whisper_full_params params = whisper_full_default_params(whisper_sampling_strategy::WHISPER_SAMPLING_GREEDY);
    return whisper_full_with_state(context_map.at(ctxRef), state_map.at(stateRef), params, samplesPointer, numSamples);
}
JNIEXPORT jint JNICALL Java_io_github_givimad_whisperjni_WhisperJNI_fullNSegmentsFromState
  (JNIEnv * env, jobject thisObject, jint stateRef){
    return whisper_full_n_segments_from_state(state_map.at(stateRef));
}

JNIEXPORT jint JNICALL Java_io_github_givimad_whisperjni_WhisperJNI_fullNSegments
  (JNIEnv * env, jobject thisObject, jint ctxRef) {
    return whisper_full_n_segments(context_map.at(ctxRef));
}
JNIEXPORT jstring JNICALL Java_io_github_givimad_whisperjni_WhisperJNI_fullGetSegmentText
  (JNIEnv * env, jobject thisObject, jint ctxRef, jint index){
    const char* text  = whisper_full_get_segment_text(context_map.at(ctxRef), index);
    return env->NewStringUTF(text);
}

JNIEXPORT jstring JNICALL Java_io_github_givimad_whisperjni_WhisperJNI_fullGetSegmentTextFromState
  (JNIEnv * env, jobject thisObject, jint stateRef, jint index) {
    const char* text  = whisper_full_get_segment_text_from_state(state_map.at(stateRef), index);
    return env->NewStringUTF(text);
}
