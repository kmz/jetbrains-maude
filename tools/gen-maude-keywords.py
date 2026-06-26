#!/usr/bin/env python3
"""Generate JFlex keyword rules for the Maude IntelliJ plugin.

Reads Maude's own lexer source (src/Mixfix/lexer.ll) and special-token table
(src/Mixfix/specialTokens.cc) from a Maude checkout, classifies each reserved
word into a category, and rewrites the region between the BEGIN/END GENERATED
KEYWORDS markers in src/main/jflex/Maude.flex.

The Maude checkout is located via (in priority order):
  1. the --maude-src PATH command-line argument
  2. the MAUDE_SRC environment variable
  3. the default ./maude-src directory under the repo root

Examples (run from the repo root):
  MAUDE_SRC=/path/to/Maude python3 tools/gen-maude-keywords.py
  python3 tools/gen-maude-keywords.py --maude-src /path/to/Maude
"""
import argparse
import os
import re
import sys

PLUGIN_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
FLEX = os.path.join(PLUGIN_ROOT, "src", "main", "jflex", "Maude.flex")


def resolve_maude_src():
    parser = argparse.ArgumentParser(description="Generate Maude JFlex keyword rules.")
    parser.add_argument("--maude-src", help="Path to a maude-lang/Maude checkout root.")
    opts = parser.parse_args()
    src = opts.maude_src or os.environ.get("MAUDE_SRC") \
        or os.path.join(PLUGIN_ROOT, "maude-src")
    src = os.path.abspath(src)
    lexer = os.path.join(src, "src", "Mixfix", "lexer.ll")
    special = os.path.join(src, "src", "Mixfix", "specialTokens.cc")
    for path in (lexer, special):
        if not os.path.isfile(path):
            sys.exit(
                f"Maude source not found: {path}\n"
                "Point to a maude-lang/Maude checkout via --maude-src PATH or "
                "the MAUDE_SRC environment variable (or clone it into ./maude-src)."
            )
    return lexer, special


LEXER_LL, SPECIAL = resolve_maude_src()

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
    """Split a flex rule LHS like 'assoc|associative' into literal words.

    Patterns containing '(' use flex grouping (e.g. 'end(th|fth|...)|jbo').
    We cannot reliably decompose these without a full flex parser, so we skip
    them — the curated CATEGORY dict already seeds the important forms.
    """
    if "(" in pattern:
        return []
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

    # Remember which words are explicitly curated so synonym extraction cannot
    # reassign them to a different category (e.g. 'object' stays KW_ATTRIBUTE
    # even though it shares a rule with 'obj' which is KW_MODULE).
    curated = set(lookup.keys())

    # Pull synonyms from lexer.ll: if any literal on a rule is already
    # classified, classify the whole rule's literals the same way.
    for words in collect_lexer_ll():
        cat = next((lookup[w] for w in words if w in lookup), None)
        if cat is None:
            continue
        for w in words:
            if w in curated and lookup.get(w) != cat:
                continue  # curated word already belongs to a different category
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
