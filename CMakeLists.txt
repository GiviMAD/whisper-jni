cmake_minimum_required(VERSION 3.5)

project(whisper-jni C CXX)

# ---- Set options ----

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

option(GGML_AVX                 "ggml: enable AVX"                ON )
option(GGML_AVX2                "ggml: enable AVX2"               ON )
option(GGML_FMA                 "ggml: enable FMA"                ON )
option(GGML_F16C                "ggml: enable F16c"               ON )
option(GGML_STATIC              "ggml: static link libraries"     OFF)
option(GGML_NATIVE              "ggml: enable -march=native flag" OFF)
option(BUILD_SHARED_LIBS        "whisper: build shared libs"      ON )

set(JAVA_AWT_LIBRARY NotNeeded)
set(JAVA_JVM_LIBRARY NotNeeded)
find_package(JNI REQUIRED)
if (JNI_FOUND)
    message (STATUS "JNI_INCLUDE_DIRS=${JNI_INCLUDE_DIRS}")
endif()

if(MSVC)
  # Force compiler to use UTF-8 for IPA constants
  add_compile_options("$<$<C_COMPILER_ID:MSVC>:/utf-8>")
  add_compile_options("$<$<CXX_COMPILER_ID:MSVC>:/utf-8>")
elseif(NOT APPLE)
  string(APPEND CMAKE_CXX_FLAGS " -Wall -Wextra -Wl,-rpath,'$ORIGIN'")
  string(APPEND CMAKE_C_FLAGS " -Wall -Wextra")
  set(CMAKE_INSTALL_RPATH '$ORIGIN')
elseif(APPLE)
  set(CMAKE_INSTALL_RPATH "@loader_path")
endif()

# ---- Declare libraries ----

add_library(whisper-jni SHARED src/main/native/io_github_givimad_whisperjni_WhisperJNI.cpp src/main/native/whisper/examples/grammar-parser.cpp)

if(NOT DEFINED WHISPER_DIR)
set(WHISPER_DIR "${CMAKE_CURRENT_BINARY_DIR}/w.cpp")
set(WHISPER_INSTALL_DIR "${CMAKE_CURRENT_BINARY_DIR}/w")
include(ExternalProject)
  ExternalProject_Add(
    whisper-cpp
    PREFIX "${WHISPER_INSTALL_DIR}"
    URL "${CMAKE_CURRENT_LIST_DIR}/src/main/native/whisper"
    CMAKE_ARGS -DCMAKE_INSTALL_RPATH:STRING=${CMAKE_INSTALL_RPATH} -DBUILD_SHARED_LIBS=${BUILD_SHARED_LIBS} -DCMAKE_C_FLAGS:STRING=${CMAKE_C_FLAGS} -DGGML_STATIC=${GGML_STATIC} -DGGML_NATIVE=${GGML_NATIVE} -DGGML_AVX=${GGML_AVX} -DGGML_AVX2=${GGML_AVX2} -DGGML_FMA=${GGML_FMA} -DGGML_F16C=${GGML_F16C} -DWHISPER_STANDALONE=1 -DCMAKE_INSTALL_PREFIX:PATH=${WHISPER_DIR} -DCMAKE_OSX_ARCHITECTURES:STRING=${CMAKE_OSX_ARCHITECTURES} -DCMAKE_OSX_DEPLOYMENT_TARGET:STRING=${CMAKE_OSX_DEPLOYMENT_TARGET}
  )
  add_dependencies(whisper-jni whisper-cpp)
endif()

# ---- Declare executable ----

if((NOT MSVC) AND (NOT APPLE))
  # Linux flags
  string(APPEND CMAKE_CXX_FLAGS " -Wall -Wextra -Wl,-rpath,'$ORIGIN'")
  string(APPEND CMAKE_C_FLAGS " -Wall -Wextra")
  # target_link_libraries(whisper-jni -static-libgcc -static-libstdc++)
endif()

target_link_libraries(whisper-jni
  whisper
  ggml
)

target_link_directories(whisper-jni PUBLIC
  ${WHISPER_DIR}/lib
  ${WHISPER_DIR}/lib/static
)

target_include_directories(whisper-jni PUBLIC
  ${JNI_INCLUDE_DIRS}
  src/main/native
  ${WHISPER_DIR}/include
  ${WHISPER_INSTALL_DIR}/src/whisper-cpp/examples
)

# ---- Declare install targets ----

install(
  TARGETS whisper-jni
  DESTINATION ${CMAKE_INSTALL_PREFIX}
)

# Dependencies

install(
  DIRECTORY ${WHISPER_DIR}/lib/
  DESTINATION ${CMAKE_INSTALL_PREFIX}
  USE_SOURCE_PERMISSIONS
  FILES_MATCHING
  PATTERN "*.so.*"
  PATTERN "*.so"
  PATTERN "*.dylib"
)

install(
  DIRECTORY ${WHISPER_DIR}/bin/
  DESTINATION ${CMAKE_INSTALL_PREFIX}
  USE_SOURCE_PERMISSIONS
  FILES_MATCHING
  PATTERN "*.dll"
)

