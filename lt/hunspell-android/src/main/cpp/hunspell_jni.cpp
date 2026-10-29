// JNI bridge for org.languagetool.rules.spelling.hunspell.AndroidHunspellDictionary.
// Words cross the boundary as UTF-8 byte arrays: JNI's own string functions use "modified UTF-8",
// which differs from Hunspell's UTF-8 for characters outside the BMP.
#include <jni.h>
#include <string>
#include <vector>
#include "hunspell.hxx"

namespace {

std::string toString(JNIEnv* env, jbyteArray bytes) {
  jsize len = env->GetArrayLength(bytes);
  std::string s(len, '\0');
  env->GetByteArrayRegion(bytes, 0, len, reinterpret_cast<jbyte*>(s.data()));
  return s;
}

Hunspell* hunspell(jlong handle) { return reinterpret_cast<Hunspell*>(handle); }

}  // namespace

extern "C" {

JNIEXPORT jlong JNICALL
Java_org_languagetool_rules_spelling_hunspell_AndroidHunspellDictionary_open(JNIEnv* env, jclass, jstring aff, jstring dic) {
  const char* affPath = env->GetStringUTFChars(aff, nullptr);
  const char* dicPath = env->GetStringUTFChars(dic, nullptr);
  auto* h = new Hunspell(affPath, dicPath);
  env->ReleaseStringUTFChars(aff, affPath);
  env->ReleaseStringUTFChars(dic, dicPath);
  if (h->get_dict_encoding() != "UTF-8") {  // the byte-array bridge above assumes UTF-8 dictionaries
    delete h;
    return 0;
  }
  return reinterpret_cast<jlong>(h);
}

JNIEXPORT jboolean JNICALL
Java_org_languagetool_rules_spelling_hunspell_AndroidHunspellDictionary_spell(JNIEnv* env, jclass, jlong handle, jbyteArray word) {
  return hunspell(handle)->spell(toString(env, word)) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jobjectArray JNICALL
Java_org_languagetool_rules_spelling_hunspell_AndroidHunspellDictionary_suggest(JNIEnv* env, jclass, jlong handle, jbyteArray word) {
  std::vector<std::string> suggestions = hunspell(handle)->suggest(toString(env, word));
  jobjectArray result = env->NewObjectArray(suggestions.size(), env->FindClass("[B"), nullptr);
  for (size_t i = 0; i < suggestions.size(); i++) {
    jbyteArray bytes = env->NewByteArray(suggestions[i].size());
    env->SetByteArrayRegion(bytes, 0, suggestions[i].size(), reinterpret_cast<const jbyte*>(suggestions[i].data()));
    env->SetObjectArrayElement(result, i, bytes);
    env->DeleteLocalRef(bytes);
  }
  return result;
}

JNIEXPORT void JNICALL
Java_org_languagetool_rules_spelling_hunspell_AndroidHunspellDictionary_add(JNIEnv* env, jclass, jlong handle, jbyteArray word) {
  hunspell(handle)->add(toString(env, word));
}

JNIEXPORT void JNICALL
Java_org_languagetool_rules_spelling_hunspell_AndroidHunspellDictionary_close(JNIEnv*, jclass, jlong handle) {
  delete hunspell(handle);
}

}  // extern "C"
