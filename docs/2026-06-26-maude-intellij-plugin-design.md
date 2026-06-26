# Maude IntelliJ プラグイン（レベル2・字句MVP）設計

日付: 2026-06-26

## 目的

JetBrains 系 IDE 向けに、Maude（`.maude`）の **Custom Language API ベース**プラグインを作る。
TextMate バンドル（正規表現ベース）とは異なり、JFlex 製 Lexer を用いた本物の字句解析で
構文ハイライト・括弧マッチング・コメントトグル・コード折りたたみを提供する。

本仕様は最初のバージョン（MVP = 字句レベル基盤、スコープ A）を対象とする。BNF 構文解析・
構文エラー検出・補完・定義ジャンプなどはレベル B / C として将来対応（非対象）。

## スコープ

含む（MVP）:
- JFlex 製 Lexer による文脈依存の構文ハイライト
- 括弧マッチング（`()` `[]` `{}`）
- コメントトグル（行 `***`、ブロック `***( )`）
- コード折りたたみ（`fmod`〜`endfm` 等のモジュール、ブロックコメント）
- 配色カスタマイズ用 Color Settings Page
- キーワードを Maude 本体ソースから生成するスクリプト

含まない（YAGNI / 将来）:
- BNF 構文解析・構文エラー検出
- 補完・定義ジャンプ・参照検索・リネーム

## アプローチ選定

- **言語: Kotlin**。現行 IntelliJ プラグインの標準で記述が簡潔。
- **字句解析: JFlex**（IntelliJ 標準の Lexer 生成器）。
- **最小 ParserDefinition を入れる**: IntelliJ は Lexer 単体では折りたたみ・括弧マッチを
  組めない。全トークンをフラットに並べるだけの最小パーサを置くことで、ハイライト・括弧・
  コメント・折りたたみが成立し、将来レベル B（本物の BNF パーサ）へ自然に拡張できる。

## アーキテクチャ

```
ファイル → MaudeLexer(JFlex) → トークン列 → 最小ParserDefinition(フラットAST)
            ├─ SyntaxHighlighter      … トークン→色
            ├─ BraceMatcher           … () [] {} 対応
            ├─ Commenter              … *** / ***( )
            └─ FoldingBuilder         … fmod〜endfm, ブロックコメント を畳む
```

## コンポーネント（責務単位）

| ファイル | 役割 | 依存 |
|---|---|---|
| `MaudeLanguage` / `MaudeFileType` | 言語登録・`.maude` 関連付け・アイコン | IntelliJ Platform |
| `src/main/jflex/Maude.flex` | 字句定義。キーワードは生成断片を取り込む | 生成スクリプト出力 |
| `MaudeTokenTypes` | トークン種別の `IElementType` 定義 | — |
| `MaudeLexerAdapter` | JFlex Lexer を `FlexAdapter` で接続 | Maude.flex 生成物 |
| `MaudeParserDefinition` | 全トークンをフラットに保持する最小パーサ | LexerAdapter, TokenTypes |
| `highlight/MaudeSyntaxHighlighter(+Factory)` | トークン → `TextAttributesKey` | TokenTypes |
| `highlight/MaudeColorSettingsPage` | 設定画面で配色カスタム＋デモ表示 | SyntaxHighlighter |
| `editor/MaudeBraceMatcher` | 括弧マッチング | TokenTypes |
| `editor/MaudeCommenter` | コメントトグル | — |
| `editor/MaudeFoldingBuilder` | `fmod`〜`endfm` 等・ブロックコメントの折りたたみ | TokenTypes |
| `tools/gen-maude-keywords.py` | `specialTokens.cc`/`lexer.ll` → カテゴリ別キーワード断片を生成 | Maude 本体ソース |

## トークン分類（色カテゴリ）

KEYWORD（module / end / declaration / command / control）、ATTRIBUTE（`[]` 内属性語）、
STRING、NUMBER、COMMENT_LINE、COMMENT_BLOCK、OPERATOR、PARENS / BRACKETS / BRACES、
IDENTIFIER、BAD_CHARACTER。各々に既定の `TextAttributesKey` を割り当て、
ColorSettingsPage で上書き可能にする。

## キーワード生成

`tools/gen-maude-keywords.py` が出典 2 ファイルをパースし、カテゴリ別キーワードを
`Maude.flex` に挿入する断片（例 `src/main/jflex/generated-keywords.flex`）として出力する。

- 出典: `src/Mixfix/specialTokens.cc`（`MACRO(name, "string")` 116 件）、
  `src/Mixfix/lexer.ll`（`... RETURN(KW_...)` のコマンド系）
- カテゴリ分類: module / end / declaration / attribute / command / control
- Maude のバージョン更新時はスクリプト再実行で追従

これが「言語仕様（本体ソース）から生成」の実体。なお Maude の演算子は各モジュールで
ユーザー定義（mixfix）されるため、静的に取得できるのは組み込み予約トークンのみ（既知の制約）。

## ビルド・環境

- IntelliJ Platform Gradle Plugin 2.x、Kotlin、JDK 17（`asdf` corretto-17、`.tool-versions` 同梱）
- Gradle wrapper（`gradlew`）同梱 → 追加インストール不要
- ターゲット: IntelliJ IDEA 2024.2+（`sinceBuild=242`）

## テスト・検証

- Lexer テスト: `tests/Misc/*.maude` の一部をトークン化し期待トークン列を検証
- `./gradlew buildPlugin` で `.zip` 生成、`./gradlew runIde` でサンドボックス IDE 起動し目視確認
- 生成スクリプトの抽出件数チェック（`specialTokens.cc` 116 件等との突き合わせ）

## 成果物配置

```
editor-support/intellij-maude/
├── build.gradle.kts / settings.gradle.kts / gradle.properties
├── gradlew(.bat) / gradle/wrapper/
├── .tool-versions
├── src/main/jflex/Maude.flex
├── src/main/jflex/generated-keywords.flex   # 生成物
├── src/main/kotlin/org/maude/intellij/
│   ├── MaudeLanguage.kt / MaudeFileType.kt / MaudeIcons.kt
│   ├── MaudeTokenTypes.kt / MaudeLexerAdapter.kt / MaudeParserDefinition.kt
│   ├── highlight/MaudeSyntaxHighlighter.kt / MaudeSyntaxHighlighterFactory.kt / MaudeColorSettingsPage.kt
│   └── editor/MaudeBraceMatcher.kt / MaudeCommenter.kt / MaudeFoldingBuilder.kt
├── src/main/resources/META-INF/plugin.xml
├── src/main/resources/icons/maude.svg
├── src/test/kotlin/.../MaudeLexerTest.kt
└── tools/gen-maude-keywords.py
```

## 実装順序（高レベル）

1. Gradle/Kotlin スケルトン（build スクリプト, plugin.xml, Language/FileType）→ `runIde` が起動するまで
2. `gen-maude-keywords.py` でキーワード断片を生成
3. `Maude.flex` 字句定義 → JFlex で Lexer 生成 → TokenTypes / LexerAdapter / ParserDefinition
4. SyntaxHighlighter + Factory（ハイライトが出るまで）
5. ColorSettingsPage / BraceMatcher / Commenter / FoldingBuilder
6. Lexer テスト、`buildPlugin` で `.zip` 確認、`runIde` 目視
