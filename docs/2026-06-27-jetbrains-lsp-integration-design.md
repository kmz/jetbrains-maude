# jetbrains-maude ネイティブLSP統合（診断）設計

日付: 2026-06-27

## 目的

既存の `jetbrains-maude` プラグイン（JFlex字句ハイライト・折りたたみ・括弧対応など）に、
JetBrains ネイティブ LSP API（`com.intellij.platform.lsp`）経由で `maude-lsp` サーバーを
起動する統合を追加し、**診断（エラー検出）** を提供する。

JetBrains では折りたたみ・構造ビューは既に native に動くため、LSP 統合で新たに増える主な価値は
**診断** である（補完・定義ジャンプ等はサーバー未実装のため対象外）。

## スコープ

含む（MVP）:
- `.maude` ファイルを開いたとき `maude-lsp` サーバーを stdio で起動し、診断を表示。
- `maude-lsp` の `server/dist/server.js`（バンドル済み）をプラグインに同梱し、更新スクリプトを用意。
- LSP を optional 依存にし、ハイライト等は Community 含む全 IDE で従来どおり動作させる。

含まない（YAGNI / 将来）:
- 補完・定義ジャンプ・参照・hover（サーバー未実装）。
- LSP 設定 UI。Marketplace 公開。

## 主要な設計判断

1. **ビルド対象は IDEA Ultimate、LSP は optional 依存。**
   - LSP API は Ultimate 系にあるため、コンパイルは `intellijIdeaUltimate("2024.2")` に切替。
   - `plugin.xml` で LSP を optional 依存にし、LSP 拡張だけを別 config ファイルに分離する。
     これによりハイライト・折りたたみ等は Community を含む全 IDE で動作し、診断（LSP）は
     有料 IDE でのみ有効化される。
2. **ビルド済み `server.js` を同梱コミット。**
   - `maude-lsp` の `server/dist/server.js`（esbuild バンドル・vscode 非依存の完結サーバー）を
     `src/main/resources/lsp/server.js` に同梱。CI は同梱物を使うため `maude-lsp` を要しない。
   - 更新は `tools/bundle-lsp-server.sh` で行う（`maude-lsp` を `MAUDE_LSP` で指定）。
3. **node / maude の発見は `EnvironmentUtil` で行う。**
   - macOS の GUI アプリはログインシェルの PATH を継承しないため、`node` や（node の子である）
     `maude` が見つからない。IntelliJ の `EnvironmentUtil.getEnvironmentMap()` がログインシェル
     環境を取得するので、それを `GeneralCommandLine` に渡して両者を解決する。

## コンポーネント

### ビルド / パッケージング
- `build.gradle.kts`: `intellijIdeaCommunity("2024.2")` → `intellijIdeaUltimate("2024.2")`。
  既存の JFlex 生成（`generateMaudeLexer`）はそのまま。
- `tools/bundle-lsp-server.sh`: `MAUDE_LSP`（既定 `../maude-lsp`）で `npm run build:server` を
  実行し、`server/dist/server.js` を `src/main/resources/lsp/server.js` へコピー。
- `src/main/resources/lsp/server.js`: 同梱されたバンドル済みサーバー（コミット対象）。

### plugin.xml
- 既存の拡張はそのまま。
- 追加: `<depends optional="true" config-file="maude-lsp.xml">com.intellij.modules.ultimate</depends>`
  （LSP を有効化する正確なモジュール ID は実装時に Ultimate ビルド＋プラグインロードで確定する）。
- `META-INF/maude-lsp.xml`（optional config）: `<platform.lsp.serverSupportProvider>` を登録。

### Kotlin（`org.maude.intellij.lsp`）
- `MaudeLspServerSupportProvider`（`LspServerSupportProvider` 実装）:
  `fileOpened` で `.maude` のとき `serverStarter.ensureServerStarted(MaudeLspServerDescriptor(project))`。
- `MaudeLspServerDescriptor`（`ProjectWideLspServerDescriptor(project, "Maude")` 継承）:
  - `isSupportedFile(file)` = 拡張子 `maude`。
  - `createCommandLine()` = 同梱 `lsp/server.js` をプラグインのクラスパスから一時ファイルに展開し、
    `GeneralCommandLine("node", <展開先>, "--stdio")` を返す。`withEnvironment(EnvironmentUtil
    .getEnvironmentMap())` でシェル PATH を付与（node / maude 解決）。
- サーバー JS の展開ヘルパー（リソース → 一時ファイル）はこの descriptor 内に閉じる。

## データフロー

```
.maude を開く
  → MaudeLspServerSupportProvider.fileOpened
  → ensureServerStarted(MaudeLspServerDescriptor)
  → node <unpacked server.js> --stdio  （env = ログインシェル）
  → JetBrains LSP クライアント ⇄ maude-lsp（initialize / didOpen / didChange）
  → サーバーが maude を実行し publishDiagnostics
  → エディタに波線で診断表示
```

## 設定 / 既定

- 当面、専用設定は設けない（PATH 解決は `EnvironmentUtil` で吸収、`maude` は既定で PATH 上）。
- 将来必要なら `maude.lsp.nodePath` 等を追加できる。

## エラー処理

- node 不在 / 起動失敗: `GeneralCommandLine` 起動失敗は JetBrains LSP のサーバーログに出る。
  サーバー側は既に「maude 起動失敗を Warning 診断で可視化」する（maude-lsp 側で対応済み）。
- LSP モジュール非搭載 IDE（Community）: optional 依存により LSP 拡張はロードされず、
  ハイライト等のみ動作。

## テスト / 検証

- 既存の Lexer/Highlighter/Editor テストは維持（`./gradlew test` 緑のまま）。
- LSP 連携部はユニットテスト困難。検証は:
  - `./gradlew buildPlugin`（Ultimate に対しコンパイル）成功と zip 生成。
  - `bundle-lsp-server.sh` が `src/main/resources/lsp/server.js` を生成することを確認。
  - **ランタイム診断の確認はライセンス済み Ultimate 系 IDE が必要なため、ユーザーが
    ビルドした zip を導入して目視**（エラー入り `.maude` で波線）。`runIde` での自動確認は
    ライセンス制約があるため必須としない。
- CI（既存 `.github/workflows`）: Ultimate ビルドへ切替、`buildPlugin` で zip。server.js は
  同梱済みのため追加 clone 不要。

## 非目標（再掲）

補完・定義ジャンプ・hover・LSP 設定 UI・Marketplace 公開は将来フェーズ。

## 既知の確定要素（実装時に検証）

- LSP を有効化する plugin.xml の依存モジュール ID（`com.intellij.modules.ultimate` か別 ID か）。
- `ProjectWideLspServerDescriptor` / `LspServerSupportProvider` の 2024.2 でのシグネチャ詳細。
  これらは Ultimate ビルドでのコンパイルと、ユーザー IDE でのロード確認で確定する。
