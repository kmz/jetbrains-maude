# jetbrains-maude

Custom-language plugin giving JetBrains IDEs real lexer-based support for the
[Maude](https://maude.cs.illinois.edu/) language (`.maude`): syntax
highlighting, brace matching, comment toggling, and code folding. It uses a
JFlex lexer and integrates with the IDE's color scheme and editor actions.

The keyword tables are generated from Maude's own lexer source
([`maude-lang/Maude`](https://github.com/maude-lang/Maude):
`src/Mixfix/lexer.ll` and `src/Mixfix/specialTokens.cc`), so they track the
language definition. The generated block is committed, so the plugin builds
without a Maude checkout; CI regenerates it and fails if the committed copy is
stale (see below).

## Build

Requires JDK 17 (see `.tool-versions`, e.g. `asdf install`). The Gradle wrapper
is included — no system Gradle needed.

```bash
./gradlew buildPlugin     # -> build/distributions/jetbrains-maude-0.1.0.zip
./gradlew runIde          # launch a sandbox IDE with the plugin
./gradlew test            # lexer/highlighter/editor tests
```

Install the zip via `Settings → Plugins → ⚙ → Install Plugin from Disk…`.

## Regenerating keywords from Maude sources

The keyword rules in `src/main/jflex/Maude.flex` (between the
`BEGIN/END GENERATED KEYWORDS` markers) are generated from a Maude checkout.
You only need to do this when Maude adds/removes keywords.

```bash
# Point at a maude-lang/Maude checkout via env var ...
MAUDE_SRC=/path/to/Maude python3 tools/gen-maude-keywords.py

# ... or the --maude-src flag ...
python3 tools/gen-maude-keywords.py --maude-src /path/to/Maude

# ... or clone it into ./maude-src (gitignored) and run with no args:
git clone --depth 1 https://github.com/maude-lang/Maude maude-src
python3 tools/gen-maude-keywords.py
```

Commit the resulting `Maude.flex` change.

## Continuous integration

`.github/workflows/build.yml` runs on push, pull request, and manual dispatch.
It checks out this repo and `maude-lang/Maude`, regenerates the keyword block,
fails if it differs from the committed `Maude.flex` (drift detection), then runs
the tests, builds the plugin, and uploads the zip as a build artifact.

## Manual verification (`runIde` checklist)

After `./gradlew runIde` opens the sandbox IDE, open a `.maude` file (e.g.
`tests/Misc/rot13.maude` from the Maude repo) and verify:

- [ ] Keywords (`fmod`, `eq`, `rl`, `endm`) are colored distinctly.
- [ ] `*** line comment` text is colored as a comment.
- [ ] `***( block comment )` is colored as a comment, including when a `(`
      appears nested inside the block, and when a space separates the marker
      from the `(` (`*** ( … )`).
- [ ] Placing the caret on `(` highlights the matching `)` (and vice versa).
- [ ] `Code → Comment with Line Comment` (or the default toggle-comment
      shortcut) inserts `*** ` at the start of the line.
- [ ] The `fmod … endm` module block shows a fold gutter arrow and collapses
      when clicked.
- [ ] `Settings → Editor → Color Scheme → Maude` opens, shows the demo preview
      text, and lists ten color attribute entries: Keyword, Attribute, Comment,
      String, Number, Operator, Identifier, Parentheses, Brackets, Braces.

## Scope (MVP)

Lexer-level features only. Not included: BNF parsing, error detection,
completion, go-to-definition. Attribute words (`assoc`, `comm`, …) are
highlighted wherever they appear, not only inside `[ … ]`.

## Layout

```
.
├── build.gradle.kts / settings.gradle.kts / gradle.properties
├── gradlew(.bat) / gradle/wrapper/
├── src/main/jflex/Maude.flex            # lexer (generated keyword block committed)
├── src/main/kotlin/org/maude/intellij/  # language, lexer, highlighter, editor features
├── src/main/resources/META-INF/plugin.xml
├── src/test/kotlin/org/maude/intellij/  # lexer/highlighter/editor tests
├── tools/gen-maude-keywords.py          # keyword generator (reads a Maude checkout)
├── docs/                                # design + implementation plan
└── .github/workflows/build.yml
```
