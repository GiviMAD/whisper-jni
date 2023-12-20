cmake_minimum_required(VERSION 3.13)

project(whisper-jni C CXX)

# ---- Set options ----

set(CMAKE_CXX_STANDARD 11)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
option(WHISPER_NO_AVX                 "whisper: disable AVX"  OFF)
option(WHISPER_NO_AVX2                "whisper: disable AVX2" OFF)
option(WHISPER_NO_FMA                 "whisper: disable FMA"  OFF)
option(WHISPER_NO_F16C                "whisper: disable F16c" OFF)
option(BUILD_SHARED_LIBS              "whisper: build shared libs" ON)

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
    CMAKE_ARGS -DBUILD_SHARED_LIBS=${BUILD_SHARED_LIBS} -DCMAKE_C_FLAGS:STRING=${CMAKE_C_FLAGS} -DWHISPER_NO_AVX=${WHISPER_NO_AVX} -DWHISPER_NO_AVX2=${WHISPER_NO_AVX2} -DWHISPER_NO_FMA=${WHISPER_NO_FMA} -DWHISPER_NO_F16C=${WHISPER_NO_F16C} -DWHISPER_STANDALONE=1 -DWHISPER_METAL=0 -DCMAKE_INSTALL_PREFIX:PATH=${WHISPER_DIR} -DCMAKE_OSX_ARCHITECTURES:STRING=${CMAKE_OSX_ARCHITECTURES} -DCMAKE_OSX_DEPLOYMENT_TARGET:STRING=${CMAKE_OSX_DEPLOYMENT_TARGET}  )
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
)

target_link_directories(whisper-jni PUBLIC
  ${WHISPER_DIR}/lib
  ${WHISPER_DIR}/lib/static
)

target_include_directories(whisper-jni PUBLIC
  ${JNI_INCLUDE_DIRS}
  src/main/native
  ${WHISPER_INSTALL_DIR}/src/whisper-cpp
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

