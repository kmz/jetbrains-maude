# Maude IntelliJ Plugin (Lexer-level MVP) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a JetBrains Custom-Language plugin that gives Maude (`.maude`) real lexer-based syntax highlighting, brace matching, comment toggling, and code folding.

**Architecture:** A JFlex lexer tokenizes Maude source; a minimal flat `ParserDefinition` turns the token stream into a flat PSI tree so the platform can drive a `SyntaxHighlighter`, `PairedBraceMatcher`, `Commenter`, and `FoldingBuilder`. Keyword tables are generated from Maude's own `src/Mixfix/lexer.ll` + `specialTokens.cc` so they track the language version.

**Tech Stack:** Kotlin, IntelliJ Platform Gradle Plugin 2.x, Gradle (wrapper), JFlex via the Grammar-Kit Gradle plugin, JDK 17 (asdf corretto-17), Python 3 (keyword generator).

## Global Constraints

- Project root: `editor-support/intellij-maude/` (inside the Maude repo).
- Kotlin source package: `org.maude.intellij`.
- Target IDE: IntelliJ IDEA Community 2024.2; `sinceBuild = 242`.
- JDK 17 (`.tool-versions` → `java corretto-17.0.14.7.1`).
- Plugin Gradle dependency versions (pin exactly; bump only if a version fails to resolve):
  - `org.jetbrains.intellij.platform` = `2.1.0`
  - `org.jetbrains.kotlin.jvm` = `2.0.21`
  - `org.jetbrains.grammarkit` = `2022.3.2.2`
  - platform: `intellijIdeaCommunity("2024.2")`
- Generated JFlex class: `org.maude.intellij._MaudeLexer`.
- Keyword sources are RELATIVE to the repo root: `src/Mixfix/lexer.ll`, `src/Mixfix/specialTokens.cc` (i.e. `../../src/Mixfix/...` from the plugin root).
- Do NOT run `git commit`/`push` etc. unless the user explicitly asks. The "Commit" steps below are written for completeness; when executing under this repo's rules, stage only and skip the commit unless told otherwise.
- Known limitation to document, not fix: attribute words (`assoc`, `comm`, …) are highlighted wherever they appear, not only inside `[ … ]` (lexer is context-free for MVP).

---

## File Structure

```
editor-support/intellij-maude/
├── .tool-versions
├── settings.gradle.kts
├── build.gradle.kts
├── gradle.properties
├── gradlew / gradlew.bat / gradle/wrapper/gradle-wrapper.{jar,properties}
├── tools/gen-maude-keywords.py            # Task 2
├── src/main/jflex/Maude.flex              # Task 2 (markers) + populated by script
├── src/main/kotlin/org/maude/intellij/
│   ├── MaudeLanguage.kt                   # Task 1
│   ├── MaudeFileType.kt                   # Task 1
│   ├── MaudeIcons.kt                      # Task 1
│   ├── MaudeTokenTypes.kt                 # Task 3
│   ├── MaudeTokenSets.kt                  # Task 3
│   ├── MaudeLexerAdapter.kt               # Task 3
│   ├── MaudeFile.kt                       # Task 3
│   ├── MaudeParserDefinition.kt           # Task 3
│   ├── highlight/MaudeSyntaxHighlighter.kt        # Task 4
│   ├── highlight/MaudeSyntaxHighlighterFactory.kt # Task 4
│   ├── highlight/MaudeColorSettingsPage.kt        # Task 5
│   ├── editor/MaudeBraceMatcher.kt        # Task 5
│   ├── editor/MaudeCommenter.kt           # Task 5
│   └── editor/MaudeFoldingBuilder.kt      # Task 5
├── src/main/resources/META-INF/plugin.xml # Task 1, extended each task
├── src/main/resources/icons/maude.svg     # Task 1
└── src/test/kotlin/org/maude/intellij/MaudeLexerTest.kt  # Task 3
```

---

## Task 1: Gradle/Kotlin skeleton + language registration

**Files:**
- Create: `editor-support/intellij-maude/.tool-versions`
- Create: `editor-support/intellij-maude/settings.gradle.kts`
- Create: `editor-support/intellij-maude/build.gradle.kts`
- Create: `editor-support/intellij-maude/gradle.properties`
- Create: `editor-support/intellij-maude/src/main/kotlin/org/maude/intellij/MaudeLanguage.kt`
- Create: `editor-support/intellij-maude/src/main/kotlin/org/maude/intellij/MaudeIcons.kt`
- Create: `editor-support/intellij-maude/src/main/kotlin/org/maude/intellij/MaudeFileType.kt`
- Create: `editor-support/intellij-maude/src/main/resources/icons/maude.svg`
- Create: `editor-support/intellij-maude/src/main/resources/META-INF/plugin.xml`

**Interfaces:**
- Produces: `MaudeLanguage` (object, `com.intellij.lang.Language("Maude")`), `MaudeFileType` (object, `LanguageFileType`), `MaudeIcons.FILE` (`javax.swing.Icon`). Consumed by every later task.

- [ ] **Step 1: Create `.tool-versions`**

```
java corretto-17.0.14.7.1
```

- [ ] **Step 2: Create `settings.gradle.kts`**

```kotlin
rootProject.name = "intellij-maude"
```

- [ ] **Step 3: Create `gradle.properties`**

```properties
org.gradle.jvmargs=-Xmx2g
org.gradle.caching=true
kotlin.stdlib.default.dependency=false
```

- [ ] **Step 4: Create `build.gradle.kts`**

```kotlin
import org.jetbrains.grammarkit.tasks.GenerateLexerTask
import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.0.21"
    id("org.jetbrains.intellij.platform") version "2.1.0"
    id("org.jetbrains.grammarkit") version "2022.3.2.2"
}

group = "org.maude"
version = "0.1.0"

repositories {
    mavenCentral()
    intellijPlatform { defaultRepositories() }
}

dependencies {
    intellijPlatform {
        intellijIdeaCommunity("2024.2")
        testFramework(TestFrameworkType.Platform)
    }
    testImplementation("junit:junit:4.13.2")
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "242"
            untilBuild = provider { null }
        }
    }
}

kotlin {
    jvmToolchain(17)
}

val generateMaudeLexer = tasks.register<GenerateLexerTask>("generateMaudeLexer") {
    sourceFile.set(file("src/main/jflex/Maude.flex"))
    targetOutputDir.set(file("build/generated/jflex/org/maude/intellij"))
    purgeOldFiles.set(true)
}

tasks.named("compileKotlin") {
    dependsOn(generateMaudeLexer)
}

sourceSets["main"].java.srcDir("build/generated/jflex")
```

- [ ] **Step 5: Create `src/main/kotlin/org/maude/intellij/MaudeLanguage.kt`**

```kotlin
package org.maude.intellij

import com.intellij.lang.Language

object MaudeLanguage : Language("Maude")
```

- [ ] **Step 6: Create `src/main/kotlin/org/maude/intellij/MaudeIcons.kt`**

```kotlin
package org.maude.intellij

import com.intellij.openapi.util.IconLoader
import javax.swing.Icon

object MaudeIcons {
    @JvmField
    val FILE: Icon = IconLoader.getIcon("/icons/maude.svg", MaudeIcons::class.java)
}
```

- [ ] **Step 7: Create `src/main/resources/icons/maude.svg`**

```xml
<svg width="16" height="16" viewBox="0 0 16 16" xmlns="http://www.w3.org/2000/svg">
  <rect x="1" y="1" width="14" height="14" rx="3" fill="#5C6BC0"/>
  <text x="8" y="12" font-family="monospace" font-size="10" font-weight="bold"
        text-anchor="middle" fill="#FFFFFF">M</text>
</svg>
```

- [ ] **Step 8: Create `src/main/kotlin/org/maude/intellij/MaudeFileType.kt`**

```kotlin
package org.maude.intellij

import com.intellij.openapi.fileTypes.LanguageFileType
import javax.swing.Icon

object MaudeFileType : LanguageFileType(MaudeLanguage) {
    override fun getName(): String = "Maude"
    override fun getDescription(): String = "Maude source file"
    override fun getDefaultExtension(): String = "maude"
    override fun getIcon(): Icon = MaudeIcons.FILE
}
```

- [ ] **Step 9: Create `src/main/resources/META-INF/plugin.xml`**

```xml
<idea-plugin>
    <id>org.maude.intellij</id>
    <name>Maude</name>
    <vendor>Maude</vendor>
    <description><![CDATA[
        Syntax highlighting and editor support for the Maude language.
    ]]></description>

    <depends>com.intellij.modules.platform</depends>

    <extensions defaultExtensionNs="com.intellij">
        <fileType
            name="Maude"
            implementationClass="org.maude.intellij.MaudeFileType"
            fieldName="INSTANCE"
            language="Maude"
            extensions="maude"/>
    </extensions>
</idea-plugin>
```

Note: `fieldName="INSTANCE"` works because a Kotlin `object` exposes its singleton as the static field `INSTANCE`.

- [ ] **Step 10: Generate the Gradle wrapper**

Run (requires a one-time Gradle; if `gradle` is unavailable, generate the wrapper from any machine that has it, or use `brew install gradle` first):
```bash
cd editor-support/intellij-maude
gradle wrapper --gradle-version 8.10.2
```
Expected: creates `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar`, `gradle/wrapper/gradle-wrapper.properties`.

- [ ] **Step 11: Verify the skeleton builds**

Run:
```bash
cd editor-support/intellij-maude
. ~/.asdf/asdf.sh 2>/dev/null; ./gradlew clean buildPlugin -x test
```
Expected: `BUILD SUCCESSFUL`, and `build/distributions/intellij-maude-0.1.0.zip` exists. (No highlighting yet; this only proves the project + plugin.xml load.)

- [ ] **Step 12: Commit** (stage only under repo rules)

```bash
git add editor-support/intellij-maude
git commit -m "feat(intellij): Maude plugin skeleton + file type registration"
```

---

## Task 2: Keyword generator + Maude.flex

**Files:**
- Create: `editor-support/intellij-maude/tools/gen-maude-keywords.py`
- Create: `editor-support/intellij-maude/src/main/jflex/Maude.flex`
- Test: manual assertion commands below (this task's deliverable is a populated `.flex`; it is verified by grepping its generated block)

**Interfaces:**
- Produces: a `Maude.flex` whose region between `// BEGIN GENERATED KEYWORDS` and `// END GENERATED KEYWORDS` contains JFlex rules returning the token symbols `MaudeTokenTypes.KW_MODULE`, `KW_END`, `KW_DECL`, `KW_IMPORT`, `KW_COMMAND`, `KW_CONTROL`, `KW_ATTRIBUTE`, `KW_OTHER`. These symbols are defined in Task 3. (Generating the rules before the symbols exist is fine — JFlex only runs in Task 3's build.)

- [ ] **Step 1: Create `tools/gen-maude-keywords.py`**

```python
#!/usr/bin/env python3
"""Generate JFlex keyword rules for the Maude IntelliJ plugin.

Reads Maude's own lexer source (src/Mixfix/lexer.ll) and special-token table
(src/Mixfix/specialTokens.cc), classifies each reserved word into a category,
and rewrites the region between the BEGIN/END GENERATED KEYWORDS markers in
src/main/jflex/Maude.flex.

Run from the plugin root:  python3 tools/gen-maude-keywords.py
"""
import os
import re
import sys

PLUGIN_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
REPO_ROOT = os.path.abspath(os.path.join(PLUGIN_ROOT, "..", ".."))
LEXER_LL = os.path.join(REPO_ROOT, "src", "Mixfix", "lexer.ll")
SPECIAL = os.path.join(REPO_ROOT, "src", "Mixfix", "specialTokens.cc")
FLEX = os.path.join(PLUGIN_ROOT, "src", "main", "jflex", "Maude.flex")

BEGIN = "// BEGIN GENERATED KEYWORDS"
END = "// END GENERATED KEYWORDS"

# Curated category -> set of canonical Maude keyword strings.
# Synonyms (e.g. "associative") are pulled from source automatically when the
# corresponding short form is present on the same lexer rule line.
CATEGORY = {
    "KW_MODULE": {"fmod", "mod", "smod", "omod", "obj", "fth", "th", "sth", "oth", "view"},
    "KW_END": {"endfm", "endm", "endsm", "endom", "endo", "endfth", "endth",
               "endsth", "endoth", "endv", "jbo"},
    "KW_DECL": {"sort", "sorts", "subsort", "subsorts", "op", "ops", "msg",
                "msgs", "var", "vars", "class", "subclass", "subclasses",
                "mb", "cmb", "eq", "ceq", "cq", "rl", "crl", "sd", "csd",
                "strat", "strats", "strategy"},
    "KW_IMPORT": {"pr", "protecting", "ex", "extending", "inc", "including",
                  "us", "using", "gb", "generated-by"},
    "KW_ATTRIBUTE": {"assoc", "comm", "id:", "idem", "iter", "prec", "gather",
                     "frozen", "ctor", "config", "object", "memo", "format",
                     "special", "poly", "ditto", "metadata", "label", "owise",
                     "otherwise", "nonexec", "variant", "narrowing", "print",
                     "left", "right", "pconst", "portal", "ground"},
    "KW_CONTROL": {"if", "then", "else", "fi", "is", "from", "to", "such",
                   "that", "with", "by"},
    "KW_COMMAND": {"load", "sload", "in", "reduce", "red", "creduce", "cred",
                   "sreduce", "sred", "rewrite", "rew", "erewrite", "erew",
                   "frewrite", "frew", "srewrite", "srew", "dsrewrite",
                   "dsrewrite", "search", "narrow", "match", "xmatch", "unify",
                   "variants", "get", "set", "show", "select", "deselect",
                   "parse", "trace", "debug", "continue", "cont", "loop",
                   "quit", "q", "eof", "do", "clear", "norm", "normalize"},
}

WORD = re.compile(r"^[A-Za-z][A-Za-z0-9_+\-]*:?$")


def words_from_alternation(pattern):
    """Split a flex rule LHS like 'assoc|associative' into literal words."""
    out = []
    for part in pattern.split("|"):
        part = part.strip().strip('"')
        if WORD.match(part):
            out.append(part)
    return out


def collect_lexer_ll():
    """Map every literal keyword on a 'pattern RETURN/return KW_X' line to its
    sibling literals, so synonyms travel together."""
    groups = []  # list of (literals) per rule
    with open(LEXER_LL, encoding="utf-8", errors="replace") as f:
        for line in f:
            m = re.match(r"\s*([^\s{}]+)\s+(?:RETURN\(|return\s+)KW_\w+", line)
            if not m:
                continue
            words = words_from_alternation(m.group(1))
            if words:
                groups.append(words)
    return groups


def collect_special():
    words = []
    with open(SPECIAL, encoding="utf-8", errors="replace") as f:
        for m in re.finditer(r'MACRO\(\s*\w+\s*,\s*"([^"]*)"', f.read()):
            s = m.group(1)
            if WORD.match(s):
                words.append(s)
    return words


def classify():
    """Return ordered dict category -> sorted unique escaped-keyword list."""
    # Seed result with curated canonical words.
    result = {cat: set(words) for cat, words in CATEGORY.items()}
    result["KW_OTHER"] = set()

    # Build a lookup: canonical word -> category.
    lookup = {}
    for cat, words in CATEGORY.items():
        for w in words:
            lookup[w] = cat

    # Pull synonyms from lexer.ll: if any literal on a rule is already
    # classified, classify the whole rule's literals the same way.
    for words in collect_lexer_ll():
        cat = next((lookup[w] for w in words if w in lookup), None)
        if cat is None:
            continue
        for w in words:
            result[cat].add(w)
            lookup[w] = cat

    # Any other reserved word from specialTokens.cc that we have not seen
    # becomes a generic keyword so it still gets highlighted.
    for w in collect_special():
        if w not in lookup:
            result["KW_OTHER"].add(w)
            lookup[w] = "KW_OTHER"

    return result


def jflex_escape(word):
    # Quote the literal so JFlex treats it verbatim (handles ':', '-', '+').
    return '"' + word.replace("\\", "\\\\").replace('"', '\\"') + '"'


def render(result):
    order = ["KW_MODULE", "KW_END", "KW_DECL", "KW_IMPORT", "KW_ATTRIBUTE",
             "KW_CONTROL", "KW_COMMAND", "KW_OTHER"]
    lines = []
    for cat in order:
        words = sorted(result[cat], key=lambda w: (-len(w), w))
        if not words:
            continue
        alt = " | ".join(jflex_escape(w) for w in words)
        lines.append(f"  ({alt}) {{ return MaudeTokenTypes.{cat}; }}")
    return "\n".join(lines)


def main():
    result = classify()
    block = render(result)

    with open(FLEX, encoding="utf-8") as f:
        text = f.read()
    if BEGIN not in text or END not in text:
        sys.exit(f"markers not found in {FLEX}")
    pre = text[: text.index(BEGIN) + len(BEGIN)]
    post = text[text.index(END):]
    new = pre + "\n" + block + "\n  " + post
    with open(FLEX, "w", encoding="utf-8") as f:
        f.write(new)

    total = sum(len(v) for v in result.values())
    print(f"wrote {total} keywords to {FLEX}")
    for cat in result:
        print(f"  {cat}: {len(result[cat])}")


if __name__ == "__main__":
    main()
```

- [ ] **Step 2: Create `src/main/jflex/Maude.flex` (skeleton with markers)**

```
package org.maude.intellij;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;

%%

%class _MaudeLexer
%implements FlexLexer
%unicode
%function advance
%type IElementType

%{
  private int commentDepth = 0;
%}

%state BLOCK_COMMENT

WHITE_SPACE   = [ \t\f\r\n]+
LINE_COMMENT  = ("***"|"---")([^(\r\n][^\r\n]*)?
STRING        = \"([^\"\\]|\\.)*\"?
FLOAT         = [0-9]+"."[0-9]+([eE][+-]?[0-9]+)?
RATIONAL      = [0-9]+"/"[0-9]+
INT           = [0-9]+
MAUDE_ID      = [A-Za-z_][A-Za-z0-9_'\-]*

%%

<YYINITIAL> {
  ("***("|"---(")        { commentDepth = 1; yybegin(BLOCK_COMMENT); }

  {WHITE_SPACE}          { return com.intellij.psi.TokenType.WHITE_SPACE; }
  {LINE_COMMENT}         { return MaudeTokenTypes.COMMENT_LINE; }
  {STRING}               { return MaudeTokenTypes.STRING; }
  {FLOAT}                { return MaudeTokenTypes.NUMBER; }
  {RATIONAL}             { return MaudeTokenTypes.NUMBER; }
  {INT}                  { return MaudeTokenTypes.NUMBER; }

  // BEGIN GENERATED KEYWORDS
  // END GENERATED KEYWORDS

  "("                    { return MaudeTokenTypes.LPAREN; }
  ")"                    { return MaudeTokenTypes.RPAREN; }
  "["                    { return MaudeTokenTypes.LBRACKET; }
  "]"                    { return MaudeTokenTypes.RBRACKET; }
  "{"                    { return MaudeTokenTypes.LBRACE; }
  "}"                    { return MaudeTokenTypes.RBRACE; }

  ("=>"|"~>"|"->"|"=/="|":="|"::"|"=?"|"<=?"|"<-"|"=>1"|"=>+"|"=>*"|"=>!") {
                           return MaudeTokenTypes.OPERATOR; }
  ("="|":"|"<"|">"|"+"|"*"|"|"|"@"|"/\\"|"\\/"|";") {
                           return MaudeTokenTypes.OPERATOR; }

  {MAUDE_ID}             { return MaudeTokenTypes.IDENTIFIER; }
  "."                    { return MaudeTokenTypes.OPERATOR; }
  ","                    { return MaudeTokenTypes.OPERATOR; }
}

<BLOCK_COMMENT> {
  "("                    { commentDepth++; }
  ")"                    { commentDepth--;
                           if (commentDepth == 0) {
                             yybegin(YYINITIAL);
                             return MaudeTokenTypes.COMMENT_BLOCK;
                           } }
  [^()]+                 { }
  <<EOF>>                { yybegin(YYINITIAL); return MaudeTokenTypes.COMMENT_BLOCK; }
}

[^]                      { return com.intellij.psi.TokenType.BAD_CHARACTER; }
```

- [ ] **Step 3: Run the generator**

Run:
```bash
cd editor-support/intellij-maude
python3 tools/gen-maude-keywords.py
```
Expected: prints `wrote N keywords ...` with per-category counts; `KW_MODULE` ≈ 10, `KW_END` ≥ 9, `KW_ATTRIBUTE` ≥ 25, `KW_COMMAND` ≥ 30.

- [ ] **Step 4: Verify the generated block**

Run:
```bash
cd editor-support/intellij-maude
sed -n '/BEGIN GENERATED KEYWORDS/,/END GENERATED KEYWORDS/p' src/main/jflex/Maude.flex | grep -E '"fmod"|"endfm"|"assoc"|"associative"|"protecting"|"reduce"'
```
Expected: lines containing `"fmod"` (KW_MODULE), `"endfm"` (KW_END), `"assoc"`/`"associative"` (KW_ATTRIBUTE), `"protecting"` (KW_IMPORT), `"reduce"` (KW_COMMAND).

- [ ] **Step 5: Commit** (stage only under repo rules)

```bash
git add editor-support/intellij-maude/tools editor-support/intellij-maude/src/main/jflex
git commit -m "feat(intellij): keyword generator + Maude.flex lexer definition"
```

---

## Task 3: Token types, lexer adapter, parser definition + lexer test

**Files:**
- Create: `editor-support/intellij-maude/src/main/kotlin/org/maude/intellij/MaudeTokenTypes.kt`
- Create: `editor-support/intellij-maude/src/main/kotlin/org/maude/intellij/MaudeTokenSets.kt`
- Create: `editor-support/intellij-maude/src/main/kotlin/org/maude/intellij/MaudeLexerAdapter.kt`
- Create: `editor-support/intellij-maude/src/main/kotlin/org/maude/intellij/MaudeFile.kt`
- Create: `editor-support/intellij-maude/src/main/kotlin/org/maude/intellij/MaudeParserDefinition.kt`
- Modify: `editor-support/intellij-maude/src/main/resources/META-INF/plugin.xml`
- Test: `editor-support/intellij-maude/src/test/kotlin/org/maude/intellij/MaudeLexerTest.kt`

**Interfaces:**
- Consumes: `MaudeLanguage` (Task 1), the JFlex-generated `_MaudeLexer` (Task 2 flex).
- Produces: `MaudeTokenTypes` with public `IElementType` fields `KW_MODULE, KW_END, KW_DECL, KW_IMPORT, KW_ATTRIBUTE, KW_CONTROL, KW_COMMAND, KW_OTHER, COMMENT_LINE, COMMENT_BLOCK, STRING, NUMBER, OPERATOR, IDENTIFIER, LPAREN, RPAREN, LBRACKET, RBRACKET, LBRACE, RBRACE`; `MaudeTokenSets.COMMENTS/STRINGS`; `MaudeParserDefinition`; `MaudeFile.FILE` (`IFileElementType`). Consumed by Tasks 4 and 5.

- [ ] **Step 1: Write the failing lexer test**

```kotlin
package org.maude.intellij

import com.intellij.testFramework.LexerTestCase
import com.intellij.lexer.Lexer

class MaudeLexerTest : LexerTestCase() {
    override fun createLexer(): Lexer = MaudeLexerAdapter()
    override fun getDirPath(): String = ""

    fun testModuleAndKeywords() {
        doTest(
            "fmod FOO is endfm",
            """
            KW_MODULE ('fmod')
            WHITE_SPACE (' ')
            IDENTIFIER ('FOO')
            WHITE_SPACE (' ')
            KW_CONTROL ('is')
            WHITE_SPACE (' ')
            KW_END ('endfm')
            """.trimIndent()
        )
    }

    fun testLineComment() {
        doTest(
            "*** hi\n1",
            """
            COMMENT_LINE ('*** hi')
            WHITE_SPACE ('\n')
            NUMBER ('1')
            """.trimIndent()
        )
    }

    fun testNestedBlockComment() {
        doTest(
            "***( a (b) c )X",
            """
            COMMENT_BLOCK ('***( a (b) c )')
            IDENTIFIER ('X')
            """.trimIndent()
        )
    }

    fun testString() {
        doTest(
            """"a\nb"""",
            """STRING ('"a\nb"')"""
        )
    }
}
```

Note: `LexerTestCase.doTest` prints `TYPE ('text')` lines using each `IElementType`'s `toString()`. The token types in Step 3 are constructed with exactly these debug names.

- [ ] **Step 2: Run the test to verify it fails**

Run:
```bash
cd editor-support/intellij-maude
. ~/.asdf/asdf.sh 2>/dev/null; ./gradlew test --tests "org.maude.intellij.MaudeLexerTest"
```
Expected: FAIL — compilation error, `MaudeTokenTypes` / `MaudeLexerAdapter` unresolved.

- [ ] **Step 3: Create `MaudeTokenTypes.kt`**

```kotlin
package org.maude.intellij

import com.intellij.psi.tree.IElementType

class MaudeTokenType(debugName: String) : IElementType(debugName, MaudeLanguage)

object MaudeTokenTypes {
    @JvmField val KW_MODULE = MaudeTokenType("KW_MODULE")
    @JvmField val KW_END = MaudeTokenType("KW_END")
    @JvmField val KW_DECL = MaudeTokenType("KW_DECL")
    @JvmField val KW_IMPORT = MaudeTokenType("KW_IMPORT")
    @JvmField val KW_ATTRIBUTE = MaudeTokenType("KW_ATTRIBUTE")
    @JvmField val KW_CONTROL = MaudeTokenType("KW_CONTROL")
    @JvmField val KW_COMMAND = MaudeTokenType("KW_COMMAND")
    @JvmField val KW_OTHER = MaudeTokenType("KW_OTHER")

    @JvmField val COMMENT_LINE = MaudeTokenType("COMMENT_LINE")
    @JvmField val COMMENT_BLOCK = MaudeTokenType("COMMENT_BLOCK")
    @JvmField val STRING = MaudeTokenType("STRING")
    @JvmField val NUMBER = MaudeTokenType("NUMBER")
    @JvmField val OPERATOR = MaudeTokenType("OPERATOR")
    @JvmField val IDENTIFIER = MaudeTokenType("IDENTIFIER")

    @JvmField val LPAREN = MaudeTokenType("LPAREN")
    @JvmField val RPAREN = MaudeTokenType("RPAREN")
    @JvmField val LBRACKET = MaudeTokenType("LBRACKET")
    @JvmField val RBRACKET = MaudeTokenType("RBRACKET")
    @JvmField val LBRACE = MaudeTokenType("LBRACE")
    @JvmField val RBRACE = MaudeTokenType("RBRACE")
}
```

- [ ] **Step 4: Create `MaudeTokenSets.kt`**

```kotlin
package org.maude.intellij

import com.intellij.psi.tree.TokenSet

object MaudeTokenSets {
    val COMMENTS: TokenSet = TokenSet.create(
        MaudeTokenTypes.COMMENT_LINE,
        MaudeTokenTypes.COMMENT_BLOCK
    )
    val STRINGS: TokenSet = TokenSet.create(MaudeTokenTypes.STRING)

    val KEYWORDS: TokenSet = TokenSet.create(
        MaudeTokenTypes.KW_MODULE, MaudeTokenTypes.KW_END,
        MaudeTokenTypes.KW_DECL, MaudeTokenTypes.KW_IMPORT,
        MaudeTokenTypes.KW_CONTROL, MaudeTokenTypes.KW_COMMAND,
        MaudeTokenTypes.KW_OTHER
    )
}
```

- [ ] **Step 5: Create `MaudeLexerAdapter.kt`**

```kotlin
package org.maude.intellij

import com.intellij.lexer.FlexAdapter

class MaudeLexerAdapter : FlexAdapter(_MaudeLexer(null))
```

- [ ] **Step 6: Create `MaudeFile.kt`**

```kotlin
package org.maude.intellij

import com.intellij.extapi.psi.PsiFileBase
import com.intellij.psi.FileViewProvider
import com.intellij.psi.tree.IFileElementType

class MaudeFile(viewProvider: FileViewProvider) :
    PsiFileBase(viewProvider, MaudeLanguage) {
    override fun getFileType() = MaudeFileType
    override fun toString() = "Maude File"

    companion object {
        @JvmField val FILE = IFileElementType(MaudeLanguage)
    }
}
```

- [ ] **Step 7: Create `MaudeParserDefinition.kt`** (minimal flat parser)

```kotlin
package org.maude.intellij

import com.intellij.lang.ASTNode
import com.intellij.lang.ParserDefinition
import com.intellij.lang.PsiParser
import com.intellij.lexer.Lexer
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.TokenSet
import com.intellij.extapi.psi.ASTWrapperPsiElement

class MaudeParserDefinition : ParserDefinition {
    override fun createLexer(project: Project?): Lexer = MaudeLexerAdapter()

    override fun createParser(project: Project?): PsiParser =
        PsiParser { root, builder ->
            val mark = builder.mark()
            while (!builder.eof()) {
                builder.advanceLexer()
            }
            mark.done(root)
            builder.treeBuilt
        }

    override fun getFileNodeType(): IFileElementType = MaudeFile.FILE
    override fun getCommentTokens(): TokenSet = MaudeTokenSets.COMMENTS
    override fun getStringLiteralElements(): TokenSet = MaudeTokenSets.STRINGS

    override fun createElement(node: ASTNode): PsiElement = ASTWrapperPsiElement(node)
    override fun createFile(viewProvider: FileViewProvider): PsiFile = MaudeFile(viewProvider)
}
```

- [ ] **Step 8: Register the parser definition in `plugin.xml`**

Add inside `<extensions defaultExtensionNs="com.intellij">`, after the `<fileType>` element:
```xml
        <lang.parserDefinition
            language="Maude"
            implementationClass="org.maude.intellij.MaudeParserDefinition"/>
```

- [ ] **Step 9: Run the lexer test to verify it passes**

Run:
```bash
cd editor-support/intellij-maude
. ~/.asdf/asdf.sh 2>/dev/null; ./gradlew test --tests "org.maude.intellij.MaudeLexerTest"
```
Expected: PASS (all four test methods). If `testNestedBlockComment` fails on the closing offset, confirm `commentDepth=1` is set by the `***(` opener and that `[^()]+` consumes inner text.

- [ ] **Step 10: Commit** (stage only under repo rules)

```bash
git add editor-support/intellij-maude/src
git commit -m "feat(intellij): token types, flat parser definition, lexer tests"
```

---

## Task 4: Syntax highlighter

**Files:**
- Create: `editor-support/intellij-maude/src/main/kotlin/org/maude/intellij/highlight/MaudeSyntaxHighlighter.kt`
- Create: `editor-support/intellij-maude/src/main/kotlin/org/maude/intellij/highlight/MaudeSyntaxHighlighterFactory.kt`
- Modify: `editor-support/intellij-maude/src/main/resources/META-INF/plugin.xml`
- Test: `editor-support/intellij-maude/src/test/kotlin/org/maude/intellij/MaudeHighlighterTest.kt`

**Interfaces:**
- Consumes: `MaudeTokenTypes` (Task 3), `MaudeLexerAdapter` (Task 3).
- Produces: `MaudeColors` (object exposing `TextAttributesKey` fields `KEYWORD, ATTRIBUTE, COMMENT, STRING, NUMBER, OPERATOR, IDENTIFIER, PARENS, BRACKETS, BRACES`), `MaudeSyntaxHighlighter`. Consumed by Task 5's ColorSettingsPage.

- [ ] **Step 1: Write the failing highlighter test**

```kotlin
package org.maude.intellij

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.maude.intellij.highlight.MaudeColors
import org.maude.intellij.highlight.MaudeSyntaxHighlighter

class MaudeHighlighterTest : BasePlatformTestCase() {
    fun testKeywordHighlight() {
        val h = MaudeSyntaxHighlighter()
        val keys = h.getTokenHighlights(MaudeTokenTypes.KW_MODULE)
        assertEquals(1, keys.size)
        assertEquals(MaudeColors.KEYWORD, keys[0])
    }

    fun testCommentHighlight() {
        val h = MaudeSyntaxHighlighter()
        assertEquals(MaudeColors.COMMENT, h.getTokenHighlights(MaudeTokenTypes.COMMENT_LINE)[0])
        assertEquals(MaudeColors.COMMENT, h.getTokenHighlights(MaudeTokenTypes.COMMENT_BLOCK)[0])
    }

    fun testAttributeHighlight() {
        val h = MaudeSyntaxHighlighter()
        assertEquals(MaudeColors.ATTRIBUTE, h.getTokenHighlights(MaudeTokenTypes.KW_ATTRIBUTE)[0])
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run:
```bash
cd editor-support/intellij-maude
. ~/.asdf/asdf.sh 2>/dev/null; ./gradlew test --tests "org.maude.intellij.MaudeHighlighterTest"
```
Expected: FAIL — `MaudeColors` / `MaudeSyntaxHighlighter` unresolved.

- [ ] **Step 3: Create `highlight/MaudeSyntaxHighlighter.kt`**

```kotlin
package org.maude.intellij.highlight

import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors as D
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.psi.tree.IElementType
import org.maude.intellij.MaudeLexerAdapter
import org.maude.intellij.MaudeTokenTypes as T

object MaudeColors {
    val KEYWORD = createTextAttributesKey("MAUDE_KEYWORD", D.KEYWORD)
    val ATTRIBUTE = createTextAttributesKey("MAUDE_ATTRIBUTE", D.METADATA)
    val COMMENT = createTextAttributesKey("MAUDE_COMMENT", D.LINE_COMMENT)
    val STRING = createTextAttributesKey("MAUDE_STRING", D.STRING)
    val NUMBER = createTextAttributesKey("MAUDE_NUMBER", D.NUMBER)
    val OPERATOR = createTextAttributesKey("MAUDE_OPERATOR", D.OPERATION_SIGN)
    val IDENTIFIER = createTextAttributesKey("MAUDE_IDENTIFIER", D.IDENTIFIER)
    val PARENS = createTextAttributesKey("MAUDE_PARENS", D.PARENTHESES)
    val BRACKETS = createTextAttributesKey("MAUDE_BRACKETS", D.BRACKETS)
    val BRACES = createTextAttributesKey("MAUDE_BRACES", D.BRACES)
}

class MaudeSyntaxHighlighter : SyntaxHighlighterBase() {
    override fun getHighlightingLexer(): Lexer = MaudeLexerAdapter()

    override fun getTokenHighlights(tokenType: IElementType): Array<TextAttributesKey> =
        when (tokenType) {
            T.KW_MODULE, T.KW_END, T.KW_DECL, T.KW_IMPORT, T.KW_CONTROL,
            T.KW_COMMAND, T.KW_OTHER -> arr(MaudeColors.KEYWORD)
            T.KW_ATTRIBUTE -> arr(MaudeColors.ATTRIBUTE)
            T.COMMENT_LINE, T.COMMENT_BLOCK -> arr(MaudeColors.COMMENT)
            T.STRING -> arr(MaudeColors.STRING)
            T.NUMBER -> arr(MaudeColors.NUMBER)
            T.OPERATOR -> arr(MaudeColors.OPERATOR)
            T.IDENTIFIER -> arr(MaudeColors.IDENTIFIER)
            T.LPAREN, T.RPAREN -> arr(MaudeColors.PARENS)
            T.LBRACKET, T.RBRACKET -> arr(MaudeColors.BRACKETS)
            T.LBRACE, T.RBRACE -> arr(MaudeColors.BRACES)
            else -> TextAttributesKey.EMPTY_ARRAY
        }

    private fun arr(key: TextAttributesKey) = arrayOf(key)
}
```

- [ ] **Step 4: Create `highlight/MaudeSyntaxHighlighterFactory.kt`**

```kotlin
package org.maude.intellij.highlight

import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

class MaudeSyntaxHighlighterFactory : SyntaxHighlighterFactory() {
    override fun getSyntaxHighlighter(project: Project?, virtualFile: VirtualFile?): SyntaxHighlighter =
        MaudeSyntaxHighlighter()
}
```

- [ ] **Step 5: Register the factory in `plugin.xml`**

Add inside `<extensions defaultExtensionNs="com.intellij">`:
```xml
        <lang.syntaxHighlighterFactory
            language="Maude"
            implementationClass="org.maude.intellij.highlight.MaudeSyntaxHighlighterFactory"/>
```

- [ ] **Step 6: Run the test to verify it passes**

Run:
```bash
cd editor-support/intellij-maude
. ~/.asdf/asdf.sh 2>/dev/null; ./gradlew test --tests "org.maude.intellij.MaudeHighlighterTest"
```
Expected: PASS (three methods).

- [ ] **Step 7: Commit** (stage only under repo rules)

```bash
git add editor-support/intellij-maude/src
git commit -m "feat(intellij): syntax highlighter + color keys"
```

---

## Task 5: Color settings page, brace matcher, commenter, folding

**Files:**
- Create: `editor-support/intellij-maude/src/main/kotlin/org/maude/intellij/highlight/MaudeColorSettingsPage.kt`
- Create: `editor-support/intellij-maude/src/main/kotlin/org/maude/intellij/editor/MaudeBraceMatcher.kt`
- Create: `editor-support/intellij-maude/src/main/kotlin/org/maude/intellij/editor/MaudeCommenter.kt`
- Create: `editor-support/intellij-maude/src/main/kotlin/org/maude/intellij/editor/MaudeFoldingBuilder.kt`
- Modify: `editor-support/intellij-maude/src/main/resources/META-INF/plugin.xml`
- Test: `editor-support/intellij-maude/src/test/kotlin/org/maude/intellij/MaudeEditorTest.kt`

**Interfaces:**
- Consumes: `MaudeColors`, `MaudeSyntaxHighlighter` (Task 4); `MaudeTokenTypes` (Task 3); `MaudeFileType` (Task 1).
- Produces: editor-feature extensions; no symbols consumed downstream.

- [ ] **Step 1: Write the failing editor test**

```kotlin
package org.maude.intellij

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class MaudeEditorTest : BasePlatformTestCase() {
    fun testBraceMatchingFolding() {
        myFixture.configureByText(
            "a.maude",
            "fmod FOO is op f : -> Nat [ctor] . endfm\n"
        )
        // Folding regions are built from the flat token stream.
        val regions = myFixture.editor.foldingModel.allFoldRegions
        // At least the module fmod..endfm should be foldable.
        assertTrue(regions.any { it.startOffset == 0 })
    }

    fun testCommenter() {
        val c = MaudeFileType.language.let {
            com.intellij.lang.LanguageCommenters.INSTANCE.forLanguage(it)
        }
        assertEquals("*** ", c.lineCommentPrefix)
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run:
```bash
cd editor-support/intellij-maude
. ~/.asdf/asdf.sh 2>/dev/null; ./gradlew test --tests "org.maude.intellij.MaudeEditorTest"
```
Expected: FAIL — no commenter registered (`c` is null) / no folding regions.

- [ ] **Step 3: Create `editor/MaudeCommenter.kt`**

```kotlin
package org.maude.intellij.editor

import com.intellij.lang.Commenter

class MaudeCommenter : Commenter {
    override fun getLineCommentPrefix(): String = "*** "
    override fun getBlockCommentPrefix(): String = "***("
    override fun getBlockCommentSuffix(): String = ")"
    override fun getCommentedBlockCommentPrefix(): String? = null
    override fun getCommentedBlockCommentSuffix(): String? = null
}
```

- [ ] **Step 4: Create `editor/MaudeBraceMatcher.kt`**

```kotlin
package org.maude.intellij.editor

import com.intellij.lang.BracePair
import com.intellij.lang.PairedBraceMatcher
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IElementType
import org.maude.intellij.MaudeTokenTypes as T

class MaudeBraceMatcher : PairedBraceMatcher {
    private val pairs = arrayOf(
        BracePair(T.LPAREN, T.RPAREN, false),
        BracePair(T.LBRACKET, T.RBRACKET, false),
        BracePair(T.LBRACE, T.RBRACE, true)
    )

    override fun getPairs(): Array<BracePair> = pairs
    override fun isPairedBracesAllowedBeforeType(lbraceType: IElementType, next: IElementType?): Boolean = true
    override fun getCodeConstructStart(file: PsiFile?, openingBraceOffset: Int): Int = openingBraceOffset
}
```

- [ ] **Step 5: Create `editor/MaudeFoldingBuilder.kt`**

```kotlin
package org.maude.intellij.editor

import com.intellij.lang.ASTNode
import com.intellij.lang.folding.FoldingBuilderEx
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.maude.intellij.MaudeTokenTypes as T

class MaudeFoldingBuilder : FoldingBuilderEx(), DumbAware {

    override fun buildFoldRegions(
        root: PsiElement,
        document: Document,
        quick: Boolean
    ): Array<FoldingDescriptor> {
        val descriptors = mutableListOf<FoldingDescriptor>()
        val node = root.node ?: return emptyArray()

        // Collect leaf tokens in order.
        val leaves = mutableListOf<ASTNode>()
        fun walk(n: ASTNode) {
            if (n.firstChildNode == null) leaves.add(n)
            var c = n.firstChildNode
            while (c != null) { walk(c); c = c.treeNext }
        }
        walk(node)

        // Pair module-start keywords with the next end keyword.
        val starts = ArrayDeque<ASTNode>()
        for (leaf in leaves) {
            when (leaf.elementType) {
                T.KW_MODULE -> starts.addLast(leaf)
                T.KW_END -> {
                    val start = starts.removeLastOrNull() ?: continue
                    val range = TextRange(start.startOffset, leaf.startOffset + leaf.textLength)
                    if (range.length > 0) {
                        descriptors.add(FoldingDescriptor(start, range))
                    }
                }
                else -> {}
            }
        }

        // Block comments fold on their own node range.
        for (leaf in leaves) {
            if (leaf.elementType == T.COMMENT_BLOCK && leaf.textLength > 0) {
                descriptors.add(FoldingDescriptor(leaf, leaf.textRange))
            }
        }
        return descriptors.toTypedArray()
    }

    override fun getPlaceholderText(node: ASTNode): String =
        when (node.elementType) {
            T.COMMENT_BLOCK -> "***(...)"
            else -> "..."
        }

    override fun isCollapsedByDefault(node: ASTNode): Boolean = false
}
```

- [ ] **Step 6: Create `highlight/MaudeColorSettingsPage.kt`**

```kotlin
package org.maude.intellij.highlight

import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.options.colors.AttributesDescriptor
import com.intellij.openapi.options.colors.ColorDescriptor
import com.intellij.openapi.options.colors.ColorSettingsPage
import org.maude.intellij.MaudeIcons
import javax.swing.Icon

class MaudeColorSettingsPage : ColorSettingsPage {
    private val descriptors = arrayOf(
        AttributesDescriptor("Keyword", MaudeColors.KEYWORD),
        AttributesDescriptor("Attribute", MaudeColors.ATTRIBUTE),
        AttributesDescriptor("Comment", MaudeColors.COMMENT),
        AttributesDescriptor("String", MaudeColors.STRING),
        AttributesDescriptor("Number", MaudeColors.NUMBER),
        AttributesDescriptor("Operator", MaudeColors.OPERATOR),
        AttributesDescriptor("Identifier", MaudeColors.IDENTIFIER),
        AttributesDescriptor("Parentheses", MaudeColors.PARENS),
        AttributesDescriptor("Brackets", MaudeColors.BRACKETS),
        AttributesDescriptor("Braces", MaudeColors.BRACES)
    )

    override fun getIcon(): Icon = MaudeIcons.FILE
    override fun getHighlighter(): SyntaxHighlighter = MaudeSyntaxHighlighter()
    override fun getDemoText(): String = """
        *** ROT13 example
        fmod FOO is
          pr INT .
          sort Foo .
          op f : Int -> Foo [ctor] .
          var X : Int .
          eq f(X) = f(X + 1) [owise] .
        endfm
        reduce f(3) .
    """.trimIndent()

    override fun getAdditionalHighlightingTagToDescriptorMap(): MutableMap<String, TextAttributesKey>? = null
    override fun getAttributeDescriptors(): Array<AttributesDescriptor> = descriptors
    override fun getColorDescriptors(): Array<ColorDescriptor> = ColorDescriptor.EMPTY_ARRAY
    override fun getDisplayName(): String = "Maude"
}
```

- [ ] **Step 7: Register the three editor extensions + color page in `plugin.xml`**

Add inside `<extensions defaultExtensionNs="com.intellij">`:
```xml
        <lang.braceMatcher
            language="Maude"
            implementationClass="org.maude.intellij.editor.MaudeBraceMatcher"/>
        <lang.commenter
            language="Maude"
            implementationClass="org.maude.intellij.editor.MaudeCommenter"/>
        <lang.foldingBuilder
            language="Maude"
            implementationClass="org.maude.intellij.editor.MaudeFoldingBuilder"/>
        <colorSettingsPage
            implementation="org.maude.intellij.highlight.MaudeColorSettingsPage"/>
```

- [ ] **Step 8: Run the editor test to verify it passes**

Run:
```bash
cd editor-support/intellij-maude
. ~/.asdf/asdf.sh 2>/dev/null; ./gradlew test --tests "org.maude.intellij.MaudeEditorTest"
```
Expected: PASS (both methods). If folding returns no region, confirm `MaudeFoldingBuilder` walks the flat tree and that `fmod`/`endfm` map to `KW_MODULE`/`KW_END` (Task 2 categories).

- [ ] **Step 9: Commit** (stage only under repo rules)

```bash
git add editor-support/intellij-maude/src
git commit -m "feat(intellij): color settings page, brace matcher, commenter, folding"
```

---

## Task 6: Full build, sandbox verification, README

**Files:**
- Create: `editor-support/intellij-maude/README.md`
- Test: full test suite + `buildPlugin` + manual `runIde` checklist

**Interfaces:**
- Consumes: everything from Tasks 1–5.
- Produces: distributable plugin zip + docs.

- [ ] **Step 1: Run the full test suite**

Run:
```bash
cd editor-support/intellij-maude
. ~/.asdf/asdf.sh 2>/dev/null; ./gradlew clean test
```
Expected: `BUILD SUCCESSFUL`, all of `MaudeLexerTest`, `MaudeHighlighterTest`, `MaudeEditorTest` green.

- [ ] **Step 2: Build the plugin zip**

Run:
```bash
cd editor-support/intellij-maude
. ~/.asdf/asdf.sh 2>/dev/null; ./gradlew buildPlugin
```
Expected: `build/distributions/intellij-maude-0.1.0.zip` exists.

- [ ] **Step 3: Verify with a real sample in the sandbox IDE**

Run:
```bash
cd editor-support/intellij-maude
. ~/.asdf/asdf.sh 2>/dev/null; ./gradlew runIde
```
Manual checklist in the sandbox IDE (open `tests/Misc/rot13.maude` from the Maude repo):
- Keywords (`fmod`, `eq`, `rl`, `endm`) are colored.
- `***( ... )` block and `*** ...` line comments are colored, including a nested `(` inside a block comment.
- Placing the caret on `(` highlights the matching `)`.
- `Code → Comment with Line Comment` inserts `*** `.
- The `fmod … endm` module shows a fold gutter arrow and collapses.
- `Settings → Editor → Color Scheme → Maude` shows the demo text and ten attribute entries.

- [ ] **Step 4: Create `README.md`**

```markdown
# Maude IntelliJ Plugin

Custom-language plugin giving JetBrains IDEs real lexer-based support for Maude
(`.maude`): syntax highlighting, brace matching, comment toggling, and code
folding. Unlike the TextMate bundle in `../maude-syntax/`, this uses a JFlex
lexer and integrates with the IDE's color scheme and editor actions.

## Build

Requires JDK 17 (see `.tool-versions`, e.g. `asdf install`).

```bash
./gradlew buildPlugin     # -> build/distributions/intellij-maude-0.1.0.zip
./gradlew runIde          # launch a sandbox IDE with the plugin
./gradlew test            # lexer/highlighter/editor tests
```

Install the zip via `Settings → Plugins → ⚙ → Install Plugin from Disk…`.

## Regenerating keywords from Maude sources

Keyword tables come from Maude's own lexer. After updating the Maude sources,
re-run:

```bash
python3 tools/gen-maude-keywords.py
```

This rewrites the `BEGIN/END GENERATED KEYWORDS` block in
`src/main/jflex/Maude.flex` from `../../src/Mixfix/lexer.ll` and
`../../src/Mixfix/specialTokens.cc`.

## Scope (MVP)

Lexer-level features only. Not included: BNF parsing, error detection,
completion, go-to-definition. Attribute words (`assoc`, `comm`, …) are
highlighted wherever they appear, not only inside `[ … ]`.
```

- [ ] **Step 5: Commit** (stage only under repo rules)

```bash
git add editor-support/intellij-maude/README.md
git commit -m "docs(intellij): plugin README + build instructions"
```

---

## Self-Review notes

- **Spec coverage:** highlighting (T4), brace matching (T5), comment toggle (T5),
  folding (T5), color settings page (T5), keyword generation from sources (T2),
  Gradle/Kotlin/JFlex/JDK17 env (T1), tests + buildPlugin + runIde (T3–T6) — all covered.
- **Known version risk:** `org.jetbrains.intellij.platform` 2.x DSL and
  `grammarkit` task property names (`sourceFile`/`targetOutputDir`) are the most
  likely to need a minor bump; the Global Constraints note allows it.
- **Flat-parser folding:** folding walks the flat PSI leaf stream rather than a
  real AST — adequate for MVP module/comment folding; revisit when Task-B parser lands.
```
