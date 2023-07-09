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
  int nThreads = (jint)env->GetIntField(jParams, env->GetFieldID(paramsJClass, "nThreads", "I"));
  if (nThreads > 0)
  {
    params.n_threads = nThreads;
  }
  params.audio_ctx = (jint)env->GetIntField(jParams, env->GetFieldID(paramsJClass, "audioCtx", "I"));
  params.n_max_text_ctx = (jint)env->GetIntField(jParams, env->GetFieldID(paramsJClass, "nMaxTextCtx", "I"));
  params.offset_ms = (jint)env->GetIntField(jParams, env->GetFieldID(paramsJClass, "offsetMs", "I"));
  params.duration_ms = (jint)env->GetIntField(jParams, env->GetFieldID(paramsJClass, "durationMs", "I"));

  jstring language = (jstring)env->GetObjectField(jParams, env->GetFieldID(paramsJClass, "language", "Ljava/lang/String;"));
  params.language = language == NULL ? nullptr : env->GetStringUTFChars(language, NULL);
  jstring initialPrompt = (jstring)env->GetObjectField(jParams, env->GetFieldID(paramsJClass, "initialPrompt", "Ljava/lang/String;"));
  params.initial_prompt = initialPrompt == NULL ? nullptr : env->GetStringUTFChars(initialPrompt, NULL);

  params.translate = (jboolean)env->GetBooleanField(jParams, env->GetFieldID(paramsJClass, "translate", "Z"));
  params.no_context = (jboolean)env->GetBooleanField(jParams, env->GetFieldID(paramsJClass, "noContext", "Z"));
  params.single_segment = (jboolean)env->GetBooleanField(jParams, env->GetFieldID(paramsJClass, "singleSegment", "Z"));
  params.print_special = (jboolean)env->GetBooleanField(jParams, env->GetFieldID(paramsJClass, "printSpecial", "Z"));
  params.print_progress = (jboolean)env->GetBooleanField(jParams, env->GetFieldID(paramsJClass, "printProgress", "Z"));
  params.print_realtime = (jboolean)env->GetBooleanField(jParams, env->GetFieldID(paramsJClass, "printRealtime", "Z"));
  params.print_timestamps = (jboolean)env->GetBooleanField(jParams, env->GetFieldID(paramsJClass, "printTimestamps", "Z"));
  params.detect_language = (jboolean)env->GetBooleanField(jParams, env->GetFieldID(paramsJClass, "detectLanguage", "Z"));
  params.suppress_blank = (jboolean)env->GetBooleanField(jParams, env->GetFieldID(paramsJClass, "suppressBlank", "Z"));
  params.suppress_non_speech_tokens = (jboolean)env->GetBooleanField(jParams, env->GetFieldID(paramsJClass, "suppressNonSpeechTokens", "Z"));
  params.speed_up = (jboolean)env->GetBooleanField(jParams, env->GetFieldID(paramsJClass, "speedUp", "Z"));

  params.temperature = (jfloat)env->GetFloatField(jParams, env->GetFieldID(paramsJClass, "temperature", "F"));
  params.max_initial_ts = (jfloat)env->GetFloatField(jParams, env->GetFieldID(paramsJClass, "maxInitialTs", "F"));
  params.length_penalty = (jfloat)env->GetFloatField(jParams, env->GetFieldID(paramsJClass, "lengthPenalty", "F"));
  params.temperature_inc = (jfloat)env->GetFloatField(jParams, env->GetFieldID(paramsJClass, "temperatureInc", "F"));
  params.entropy_thold = (jfloat)env->GetFloatField(jParams, env->GetFieldID(paramsJClass, "entropyThold", "F"));
  params.logprob_thold = (jfloat)env->GetFloatField(jParams, env->GetFieldID(paramsJClass, "logprobThold", "F"));
  params.no_speech_thold = (jfloat)env->GetFloatField(jParams, env->GetFieldID(paramsJClass, "noSpeechThold", "F"));

  switch (params.strategy)
  {
  case WHISPER_SAMPLING_GREEDY:
  {
    params.greedy.best_of = (jint)env->GetIntField(jParams, env->GetFieldID(paramsJClass, "greedyBestOf", "I"));
  }
  break;
  case WHISPER_SAMPLING_BEAM_SEARCH:
  {
    params.beam_search.beam_size = (jint)env->GetIntField(jParams, env->GetFieldID(paramsJClass, "beamSearchBeamSize", "I"));
    params.beam_search.patience = (jfloat)env->GetFloatField(jParams, env->GetFieldID(paramsJClass, "beamSearchPatience", "F"));
  }
  break;
  }

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

JNIEXPORT jboolean JNICALL Java_io_github_givimad_whisperjni_WhisperJNI_isMultilingual(JNIEnv *env, jobject thisObject, jint ctxRef)
{
  return whisper_is_multilingual(contextMap.at(ctxRef));
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

JNIEXPORT jlong JNICALL Java_io_github_givimad_whisperjni_WhisperJNI_fullGetSegmentTimestamp0(JNIEnv *env, jobject thisObject, jint ctxRef, jint index)
{
  whisper_context *whisper_ctx = contextMap.at(ctxRef);
  int nSegments = whisper_full_n_segments(whisper_ctx);
  if (nSegments < index + 1)
  {
    jclass exClass = env->FindClass("java/lang/IndexOutOfBoundsException");
    env->ThrowNew(exClass, "Index out of range");
    return 0L;
  }
  return whisper_full_get_segment_t0(whisper_ctx, index);
}

JNIEXPORT jlong JNICALL Java_io_github_givimad_whisperjni_WhisperJNI_fullGetSegmentTimestamp1(JNIEnv *env, jobject thisObject, jint ctxRef, jint index)
{
  whisper_context *whisper_ctx = contextMap.at(ctxRef);
  int nSegments = whisper_full_n_segments(whisper_ctx);
  if (nSegments < index + 1)
  {
    jclass exClass = env->FindClass("java/lang/IndexOutOfBoundsException");
    env->ThrowNew(exClass, "Index out of range");
    return 0L;
  }
  return whisper_full_get_segment_t1(whisper_ctx, index);
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



JNIEXPORT jlong JNICALL Java_io_github_givimad_whisperjni_WhisperJNI_fullGetSegmentTimestamp0FromState(JNIEnv *env, jobject thisObject, jint stateRef, jint index)
{
  whisper_state *state = stateMap.at(stateRef);
  int nSegments = whisper_full_n_segments_from_state(state);
  if (nSegments < index + 1)
  {
    jclass exClass = env->FindClass("java/lang/IndexOutOfBoundsException");
    env->ThrowNew(exClass, "Index out of range");
    return 0L;
  }
  return whisper_full_get_segment_t0_from_state(state, index);
}



JNIEXPORT jlong JNICALL Java_io_github_givimad_whisperjni_WhisperJNI_fullGetSegmentTimestamp1FromState(JNIEnv *env, jobject thisObject, jint stateRef, jint index)
{
  whisper_state *state = stateMap.at(stateRef);
  int nSegments = whisper_full_n_segments_from_state(state);
  if (nSegments < index + 1)
  {
    jclass exClass = env->FindClass("java/lang/IndexOutOfBoundsException");
    env->ThrowNew(exClass, "Index out of range");
    return 0L;
  }
  return whisper_full_get_segment_t1_from_state(state, index);
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
  whisper_free(contextMap.at(ctxRef));
  contextMap.erase(ctxRef);
}

JNIEXPORT void JNICALL Java_io_github_givimad_whisperjni_WhisperJNI_freeState(JNIEnv *env, jobject thisObject, jint stateRef)
{
  whisper_free_state(stateMap.at(stateRef));
  stateMap.erase(stateRef);
}