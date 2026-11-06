cmake_minimum_required(VERSION 3.22)
project(hunspell-jni CXX)

file(GLOB HUNSPELL_SOURCES hunspell/*.cxx)
add_library(hunspell-jni SHARED hunspell_jni.cpp ${HUNSPELL_SOURCES})
target_compile_definitions(hunspell-jni PRIVATE HUNSPELL_STATIC)
target_include_directories(hunspell-jni PRIVATE hunspell)

# Reproducible builds (F-Droid): keep the checkout path out of the binary and its build ID.
target_compile_options(hunspell-jni PRIVATE "-ffile-prefix-map=${CMAKE_SOURCE_DIR}=.")
target_link_options(hunspell-jni PRIVATE "LINKER:--build-id=none")
