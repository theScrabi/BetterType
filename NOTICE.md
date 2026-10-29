# Better Type — third-party components

Better Type is licensed under the GNU General Public License v3.0 or later (see `LICENSE`).
It bundles the following components; their own license terms continue to apply.

| Component | Where | License | Source |
|---|---|---|---|
| LanguageTool 6.8 (core, en, de, fr, es), modified for Android | `lt/core`, `lt/lang-*` | LGPL 2.1 or later (`lt/COPYING.txt`) | https://github.com/languagetool-org/languagetool (tag v6.8) |
| Hunspell 1.7.2 | `lt/hunspell-android/src/main/cpp/hunspell` | MPL 1.1 / GPL 2.0 / LGPL 2.1 | https://github.com/hunspell/hunspell |
| morfologik-speller 2.1.9, modified for Android | `lt/morfologik-speller` | BSD | https://github.com/morfologiklib/morfologik-stemming |
| German Hunspell/Morfologik spelling dictionaries (igerman98) | inside `lt/lang-de` | GPL v2 / GPL v3 | https://www.j3e.de/ispell/igerman98/ |
| german-pos-dict 1.2.4 | Maven `de.danielnaber:german-pos-dict` | CC BY-SA 4.0 | https://github.com/languagetool-org/german-pos-dict |
| french-pos-dict 0.7 | Maven `org.languagetool:french-pos-dict` | CC BY-SA 4.0 | https://github.com/languagetool-org/french-pos-dict |
| english-pos-dict 0.6 | Maven `org.languagetool:english-pos-dict` | LGPL 2.1 | https://github.com/languagetool-org/english-pos-dict |
| spanish-pos-dict 2.5 | Maven `org.softcatala:spanish-pos-dict` | LGPL 2.1 | https://github.com/Softcatala/spanish-dict-tools |
| Apache OpenNLP + English models 1.5 | Maven `org.apache.opennlp`, `edu.washington.cs.knowitall` | Apache License 2.0 | https://opennlp.apache.org |
| Other Java libraries (Guava, Jackson, commons-*, …) | Maven dependencies of LanguageTool | Apache 2.0 / BSD / MIT | see each artifact's POM |

CC BY-SA 4.0 and Apache 2.0 are compatible with GPL v3 (not v2), which is why the app is GPL-3.0-or-later.
