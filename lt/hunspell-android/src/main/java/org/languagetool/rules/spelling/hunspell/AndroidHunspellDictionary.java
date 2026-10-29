/* LanguageTool, a natural language style checker
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 */
package org.languagetool.rules.spelling.hunspell;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * {@link HunspellDictionary} backed by the native Hunspell library, built with the Android NDK.
 * Replaces the desktop JNA binding ({@link DumontsHunspellDictionary}) on Android.
 */
public final class AndroidHunspellDictionary implements HunspellDictionary {

  static {
    System.loadLibrary("hunspell-jni");
  }

  private long handle;

  private AndroidHunspellDictionary(Path dictionary, Path affix) {
    handle = open(affix.toString(), dictionary.toString());
    if (handle == 0) {
      throw new RuntimeException("Only UTF-8 Hunspell dictionaries are supported: " + affix);
    }
  }

  /** Makes LanguageTool create its Hunspell dictionaries with this class. Call before the first check. */
  public static void install() {
    Hunspell.setHunspellStreamFactory(new Hunspell.Factory() {
      @Override
      public HunspellDictionary createFromLocalFiles(String languageCode, Path dictionary, Path affix) {
        return new AndroidHunspellDictionary(dictionary, affix);
      }

      @Override
      public HunspellDictionary createFromStreams(String languageCode, InputStream dictionary, InputStream affix) throws IOException {
        // shortcut: the temp files are never deleted; fine while dictionaries are cached for the process lifetime
        Path dic = Files.createTempFile(languageCode, ".dic");
        Path aff = Files.createTempFile(languageCode, ".aff");
        Files.copy(dictionary, dic, StandardCopyOption.REPLACE_EXISTING);
        Files.copy(affix, aff, StandardCopyOption.REPLACE_EXISTING);
        return createFromLocalFiles(languageCode, dic, aff);
      }
    });
  }

  @Override
  public synchronized boolean spell(String word) {
    ensureOpen();
    return spell(handle, word.getBytes(StandardCharsets.UTF_8));
  }

  @Override
  public synchronized void add(String word) {
    ensureOpen();
    add(handle, word.getBytes(StandardCharsets.UTF_8));
  }

  @Override
  public synchronized List<String> suggest(String word) {
    ensureOpen();
    List<String> result = new ArrayList<>();
    for (byte[] suggestion : suggest(handle, word.getBytes(StandardCharsets.UTF_8))) {
      result.add(new String(suggestion, StandardCharsets.UTF_8));
    }
    return result;
  }

  @Override
  public synchronized boolean isClosed() {
    return handle == 0;
  }

  @Override
  public synchronized void close() {
    if (handle != 0) {
      close(handle);
      handle = 0;
    }
  }

  private void ensureOpen() {
    if (handle == 0) {
      throw new IllegalStateException("Hunspell dictionary is closed");
    }
  }

  private static native long open(String affixPath, String dictionaryPath);
  private static native boolean spell(long handle, byte[] word);
  private static native byte[][] suggest(long handle, byte[] word);
  private static native void add(long handle, byte[] word);
  private static native void close(long handle);
}
