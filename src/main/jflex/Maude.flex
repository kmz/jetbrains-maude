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
LINE_COMMENT  = ("***"|"---")([ \t\f]*([^ \t\f(\r\n][^\r\n]*)?)
STRING        = \"([^\"\\]|\\.)*\"?
FLOAT         = [0-9]+"."[0-9]+([eE][+-]?[0-9]+)?
RATIONAL      = [0-9]+"/"[0-9]+
INT           = [0-9]+
MAUDE_ID      = [A-Za-z_][A-Za-z0-9_'\-]*

%%

<YYINITIAL> {
  ("***"|"---")[ \t\f]*"("   { commentDepth = 1; yybegin(BLOCK_COMMENT); }

  {WHITE_SPACE}          { return com.intellij.psi.TokenType.WHITE_SPACE; }
  {LINE_COMMENT}         { return MaudeTokenTypes.COMMENT_LINE; }
  {STRING}               { return MaudeTokenTypes.STRING; }
  {FLOAT}                { return MaudeTokenTypes.NUMBER; }
  {RATIONAL}             { return MaudeTokenTypes.NUMBER; }
  {INT}                  { return MaudeTokenTypes.NUMBER; }

  // BEGIN GENERATED KEYWORDS
  ("fmod" | "omod" | "smod" | "view" | "fth" | "mod" | "obj" | "oth" | "sth" | "th") { return MaudeTokenTypes.KW_MODULE; }
  ("endfth" | "endoth" | "endsth" | "endfm" | "endom" | "endsm" | "endth" | "endm" | "endo" | "endv" | "jbo") { return MaudeTokenTypes.KW_END; }
  ("subclasses" | "strategy" | "subclass" | "subsorts" | "message" | "subsort" | "strats" | "class" | "rules" | "sorts" | "strat" | "msgs" | "rule" | "sort" | "vars" | "ceq" | "cmb" | "crl" | "csd" | "eqs" | "mbs" | "msg" | "ops" | "rls" | "sds" | "var" | "cq" | "eq" | "mb" | "op" | "rl" | "sd") { return MaudeTokenTypes.KW_DECL; }
  ("generated-by" | "protecting" | "extending" | "including" | "using" | "inc" | "ex" | "gb" | "pr" | "us") { return MaudeTokenTypes.KW_IMPORT; }
  ("configuration" | "associative" | "commutative" | "constructor" | "polymorphic" | "idempotent" | "precedence" | "identity:" | "narrowing" | "otherwise" | "iterated" | "metadata" | "nonexec" | "special" | "variant" | "config" | "format" | "frozen" | "gather" | "ground" | "labels" | "object" | "pconst" | "portal" | "assoc" | "ditto" | "label" | "owise" | "print" | "right" | "comm" | "ctor" | "idem" | "iter" | "left" | "memo" | "poly" | "prec" | "id:") { return MaudeTokenTypes.KW_ATTRIBUTE; }
  ("else" | "from" | "such" | "that" | "then" | "with" | "by" | "fi" | "if" | "is" | "to") { return MaudeTokenTypes.KW_CONTROL; }
  ("dsrewrite" | "normalize" | "continue" | "deselect" | "erewrite" | "frewrite" | "srewrite" | "variants" | "creduce" | "rewrite" | "sreduce" | "narrow" | "reduce" | "search" | "select" | "xmatch" | "clear" | "debug" | "dsrew" | "match" | "parse" | "sload" | "trace" | "unify" | "cont" | "cred" | "erew" | "frew" | "load" | "loop" | "norm" | "quit" | "show" | "sred" | "srew" | "eof" | "get" | "nar" | "red" | "rew" | "set" | "do" | "in" | "q") { return MaudeTokenTypes.KW_COMMAND; }
  ("nilQidListSymbol" | "qidListSymbol" | "irreducible" | "amatchrew" | "extension" | "qidSymbol" | "xmatchrew" | "matchrew" | "Exclude" | "or-else" | "Bubble" | "amatch" | "fail" | "idle" | "test" | "all" | "dnt" | "not" | "one" | "top" | "try") { return MaudeTokenTypes.KW_OTHER; }
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
