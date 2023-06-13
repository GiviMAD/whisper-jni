#include <iostream>
#include <jni.h>
#include "io_github_givimad_whisperjni_WhisperJNI.h"
#include "./whisper/whisper.cpp"
#include <map>

std::map<int, whisper_context *> contextMap;
std::map<int, whisper_state *> stateMap;

int insertModel(whisper_context *ctx)
{
  int ref = rand();
  contextMap.insert({ref, ctx});
  return ref;
}
struct whisper_full_params parseJParams(JNIEnv *env, jobject jParams)
{
  jclass paramsJClass = env->GetObjectClass(jParams);

  struct whisper_full_params params = whisper_full_default_params(
      (whisper_sampling_strategy)env->GetIntField(jParams, env->GetFieldID(paramsJClass, "strategy", "I")));
  // int params
  params.audio_ctx = (jint)env->GetIntField(jParams, env->GetFieldID(paramsJClass, "audioCtx", "I"));
  params.n_max_text_ctx = (jint)env->GetIntField(jParams, env->GetFieldID(paramsJClass, "nMaxTextCtx", "I"));
  params.offset_ms = (jint)env->GetIntField(jParams, env->GetFieldID(paramsJClass, "offsetMs", "I"));
  params.duration_ms = (jint)env->GetIntField(jParams, env->GetFieldID(paramsJClass, "durationMs", "I"));
  // bool params
  params.translate = (jboolean)env->GetBooleanField(jParams, env->GetFieldID(paramsJClass, "translate", "Z"));
  params.no_context = (jboolean)env->GetBooleanField(jParams, env->GetFieldID(paramsJClass, "noContext", "Z"));
  params.single_segment = (jboolean)env->GetBooleanField(jParams, env->GetFieldID(paramsJClass, "singleSegment", "Z"));
  params.print_special = (jboolean)env->GetBooleanField(jParams, env->GetFieldID(paramsJClass, "printSpecial", "Z"));
  params.print_progress = (jboolean)env->GetBooleanField(jParams, env->GetFieldID(paramsJClass, "printProgress", "Z"));
  params.print_realtime = (jboolean)env->GetBooleanField(jParams, env->GetFieldID(paramsJClass, "printRealtime", "Z"));
  params.print_timestamps = (jboolean)env->GetBooleanField(jParams, env->GetFieldID(paramsJClass, "printTimestamps", "Z"));

  return params;
}

JNIEXPORT jint JNICALL Java_io_github_givimad_whisperjni_WhisperJNI_init(JNIEnv *env, jobject thisObject, jstring modelPath)
{
  const char *path = env->GetStringUTFChars(modelPath, NULL);
  return insertModel(whisper_init_from_file(path));
}

JNIEXPORT jint JNICALL Java_io_github_givimad_whisperjni_WhisperJNI_initNoState(JNIEnv *env, jobject thisObject, jstring modelPath)
{
  const char *path = env->GetStringUTFChars(modelPath, NULL);
  return insertModel(whisper_init_from_file_no_state(path));
}

JNIEXPORT jint JNICALL Java_io_github_givimad_whisperjni_WhisperJNI_initState(JNIEnv *env, jobject thisObject, jint ctxRef)
{
  int stateRef = rand();
  whisper_state *state = whisper_init_state(contextMap.at(ctxRef));
  stateMap.insert({stateRef, state});
  return stateRef;
}

JNIEXPORT jint JNICALL Java_io_github_givimad_whisperjni_WhisperJNI_full(JNIEnv *env, jobject thisObject, jint ctxRef, jobject jParams, jfloatArray samples, jint numSamples)
{
  const float *samplesPointer = env->GetFloatArrayElements(samples, NULL);
  return whisper_full(contextMap.at(ctxRef), parseJParams(env, jParams), samplesPointer, numSamples);
}

JNIEXPORT jint JNICALL Java_io_github_givimad_whisperjni_WhisperJNI_fullWithState(JNIEnv *env, jobject thisObject, jint ctxRef, jint stateRef, jobject jParams, jfloatArray samples, jint numSamples)
{
  const float *samplesPointer = env->GetFloatArrayElements(samples, NULL);
  return whisper_full_with_state(contextMap.at(ctxRef), stateMap.at(stateRef), parseJParams(env, jParams), samplesPointer, numSamples);
}

JNIEXPORT jint JNICALL Java_io_github_givimad_whisperjni_WhisperJNI_fullNSegments(JNIEnv *env, jobject thisObject, jint ctxRef)
{
  return whisper_full_n_segments(contextMap.at(ctxRef));
}

JNIEXPORT jint JNICALL Java_io_github_givimad_whisperjni_WhisperJNI_fullNSegmentsFromState(JNIEnv *env, jobject thisObject, jint stateRef)
{
  return whisper_full_n_segments_from_state(stateMap.at(stateRef));
}

JNIEXPORT jstring JNICALL Java_io_github_givimad_whisperjni_WhisperJNI_fullGetSegmentText(JNIEnv *env, jobject thisObject, jint ctxRef, jint index)
{
  whisper_context *whisper_ctx = contextMap.at(ctxRef);
  int nSegments = whisper_full_n_segments(whisper_ctx);
  if (nSegments < index + 1)
  {
    jclass exClass = env->FindClass("java/lang/IndexOutOfBoundsException");
    env->ThrowNew(exClass, "Index out of range");
    return NULL;
  }
  const char *text = whisper_full_get_segment_text(whisper_ctx, index);
  return env->NewStringUTF(text);
}

JNIEXPORT jstring JNICALL Java_io_github_givimad_whisperjni_WhisperJNI_fullGetSegmentTextFromState(JNIEnv *env, jobject thisObject, jint stateRef, jint index)
{
  whisper_state *state = stateMap.at(stateRef);
  int nSegments = whisper_full_n_segments_from_state(state);
  if (nSegments < index + 1)
  {
    jclass exClass = env->FindClass("java/lang/IndexOutOfBoundsException");
    env->ThrowNew(exClass, "Index out of range");
    return NULL;
  }
  const char *text = whisper_full_get_segment_text_from_state(state, index);
  return env->NewStringUTF(text);
}

JNIEXPORT void JNICALL Java_io_github_givimad_whisperjni_WhisperJNI_freeContext(JNIEnv *env, jobject thisObject, jint ctxRef)
{
  whisper_context *whisper_ctx = contextMap.at(ctxRef);
  contextMap.erase(ctxRef);
  whisper_free(whisper_ctx);
}

JNIEXPORT void JNICALL Java_io_github_givimad_whisperjni_WhisperJNI_freeState(JNIEnv *env, jobject thisObject, jint stateRef)
{
  whisper_state *state = stateMap.at(stateRef);
  stateMap.erase(stateRef);
  whisper_free_state(state);
}