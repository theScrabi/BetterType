cmake_minimum_required(VERSION 3.22)
project(hunspell-jni CXX)

file(GLOB HUNSPELL_SOURCES hunspell/*.cxx)
add_library(hunspell-jni SHARED hunspell_jni.cpp ${HUNSPELL_SOURCES})
target_compile_definitions(hunspell-jni PRIVATE HUNSPELL_STATIC)
target_include_directories(hunspell-jni PRIVATE hunspell)
