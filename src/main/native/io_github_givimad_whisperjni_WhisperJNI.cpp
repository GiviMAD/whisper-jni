#include <iostream>
#include <map>
#include <jni.h>
#include "io_github_givimad_whisperjni_WhisperJNI.h"
#include "whisper.h"

std::map<int, whisper_context *> contextMap;
std::map<int, whisper_state *> stateMap;

static JavaVM *jvmRef = nullptr;
static void whisper_log_proxy(const char * text) {
    if(jvmRef) {
        JNIEnv *env;
        if (jvmRef->AttachCurrentThread((void**)&env, NULL) != JNI_OK) {
          return;
        }
        jclass whisperJNIClass = env->FindClass("io/github/givimad/whisperjni/WhisperJNI");
        jmethodID logMethodId = env->GetStaticMethodID(whisperJNIClass, "log", "(Ljava/lang/String;)V");
        jstring jstr = env->NewStringUTF(text);
        env->CallStaticVoidMethod(whisperJNIClass, logMethodId, jstr);
        jvmRef->DetachCurrentThread();
    }
}
int getContextId() {
    int i = 0;
    while (i++ < 1000) {
        int id = rand();
        if(!contextMap.count(id)) {
            return id;
        }
    }
    throw std::runtime_error("Wrapper error: Unable to get config id");
}
int getStateId() {
    int i = 0;
    while (i++ < 1000) {
        int id = rand();
        if(!stateMap.count(id)) {
            return id;
        }
    }
    throw std::runtime_error("Wrapper error: Unable to get state id");
}
int insertModel(whisper_context *ctx)
{
  int ref = getContextId();
  contextMap.insert({ref, ctx});
  return ref;
}

struct whisper_context_params newWhisperContextParams(JNIEnv *env, jobject jParams)
{
  jclass paramsJClass = env->GetObjectClass(jParams);
  struct whisper_context_params params = whisper_context_default_params();
  params.use_gpu = (jboolean)env->GetBooleanField(jParams, env->GetFieldID(paramsJClass, "useGPU", "Z"));
  return params;
}

void freeWhisperFullParams(JNIEnv *env, jobject jParams, whisper_full_params params)
{
  jclass paramsJClass = env->GetObjectClass(jParams);
  jstring language = (jstring)env->GetObjectField(jParams, env->GetFieldID(paramsJClass, "language", "Ljava/lang/String;"));
  if(language) {
      env->ReleaseStringUTFChars(language, params.language);
  }
  jstring initialPrompt = (jstring)env->GetObjectField(jParams, env->GetFieldID(paramsJClass, "initialPrompt", "Ljava/lang/String;"));
  if(initialPrompt) {
      env->ReleaseStringUTFChars(initialPrompt, params.initial_prompt);
  }
}

struct whisper_full_params newWhisperFullParams(JNIEnv *env, jobject jParams)
{
  jclass paramsJClass = env->GetObjectClass(jParams);

  whisper_sampling_strategy samplingStrategy = (whisper_sampling_strategy)env->GetIntField(jParams, env->GetFieldID(paramsJClass, "strategy", "I"));
  whisper_full_params params = whisper_full_default_params(samplingStrategy);

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

JNIEXPORT jint JNICALL Java_io_github_givimad_whisperjni_WhisperJNI_init(JNIEnv *env, jobject thisObject, jstring modelPath, jobject jParams)
{
  const char *path = env->GetStringUTFChars(modelPath, NULL);
  struct whisper_context *context = whisper_init_from_file_with_params(path, newWhisperContextParams(env, jParams));
  env->ReleaseStringUTFChars(modelPath, path);
  if(!context) {
    return -1;
  }
  return insertModel(context);
}

JNIEXPORT jint JNICALL Java_io_github_givimad_whisperjni_WhisperJNI_initNoState(JNIEnv *env, jobject thisObject, jstring modelPath, jobject jParams)
{
  const char *path = env->GetStringUTFChars(modelPath, NULL);
  struct whisper_context *context = whisper_init_from_file_with_params_no_state(path, newWhisperContextParams(env, jParams));
  env->ReleaseStringUTFChars(modelPath, path);
  if(!context) {
    return -1;
  }
  return insertModel(context);
}

JNIEXPORT jint JNICALL Java_io_github_givimad_whisperjni_WhisperJNI_initState(JNIEnv *env, jobject thisObject, jint ctxRef)
{
  int stateRef = getStateId();
  whisper_state *state = whisper_init_state(contextMap.at(ctxRef));
  if(!state) {
    return -1;
  }
  stateMap.insert({stateRef, state});
  return stateRef;
}

JNIEXPORT void JNICALL Java_io_github_givimad_whisperjni_WhisperJNI_initOpenVINOEncoder(JNIEnv *env, jobject thisObject, jint ctxRef, jstring deviceString) {
  const char* device = env->GetStringUTFChars(deviceString, NULL);
  whisper_ctx_init_openvino_encoder(contextMap.at(ctxRef), nullptr, device, nullptr);
  env->ReleaseStringUTFChars(deviceString, device);
}

JNIEXPORT jboolean JNICALL Java_io_github_givimad_whisperjni_WhisperJNI_isMultilingual(JNIEnv *env, jobject thisObject, jint ctxRef)
{
  return whisper_is_multilingual(contextMap.at(ctxRef));
}

JNIEXPORT jint JNICALL Java_io_github_givimad_whisperjni_WhisperJNI_full(JNIEnv *env, jobject thisObject, jint ctxRef, jobject jParams, jfloatArray samples, jint numSamples)
{
  whisper_full_params params = newWhisperFullParams(env, jParams);
  jfloat *samplesPointer = env->GetFloatArrayElements(samples, NULL);
  int result = whisper_full(contextMap.at(ctxRef), params, samplesPointer, numSamples);
  freeWhisperFullParams(env, jParams, params);
  env->ReleaseFloatArrayElements(samples, samplesPointer, 0);
  return result;
}

JNIEXPORT jint JNICALL Java_io_github_givimad_whisperjni_WhisperJNI_fullWithState(JNIEnv *env, jobject thisObject, jint ctxRef, jint stateRef, jobject jParams, jfloatArray samples, jint numSamples)
{
  whisper_full_params params = newWhisperFullParams(env, jParams);
  jfloat *samplesPointer = env->GetFloatArrayElements(samples, NULL);
  int result = whisper_full_with_state(contextMap.at(ctxRef), stateMap.at(stateRef), params, samplesPointer, numSamples);
  freeWhisperFullParams(env, jParams, params);
  env->ReleaseFloatArrayElements(samples, samplesPointer, 0);
  return result;
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
JNIEXPORT jstring JNICALL Java_io_github_givimad_whisperjni_WhisperJNI_printSystemInfo(JNIEnv *env, jobject thisObject)
{
  const char *text = whisper_print_system_info();
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
JNIEXPORT void JNICALL Java_io_github_givimad_whisperjni_WhisperJNI_setLogger(JNIEnv *env, jclass thisClass, jboolean enabled) {
    if (enabled) {
        if (!jvmRef && env->GetJavaVM(&jvmRef) != JNI_OK) {
            jclass exClass = env->FindClass("java/lang/RuntimeException");
            env->ThrowNew(exClass, "Failed getting reference to Java VM");
            return;
        }
        whisper_set_log_callback(whisper_log_proxy);
    } else {
        whisper_set_log_callback(NULL);
    }
}