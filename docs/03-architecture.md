# 設計

> このドキュメントは、**interactive-log-masking-cli** の設計をまとめたもの。

## 1. 全体アーキテクチャ方針

「対話 UI」「設定」「コア処理」「入出力」「実処理統合（service）」「レポート」「ユーティリティ」を分離する。

- 対話（CLI）は **実行計画（RunPlan）を作る** までに責務を限定する
- 実際の処理は Service が統合して実行する（CLI から直接 I/O やコアを呼ばない）
- ルール（config）→ コア（core）への変換（compile）を明示し、型を分ける

---

## 2. ベースパッケージ

- `io.github.seiya_matsuoka.interactivelogmaskingcli`

---

## 3. パッケージ分割（責務）

※**役割で分類**し、用途不明なパッケージは作らない方針

### 3.1 `cli`

対話フロー（質問・入力・確認）を担当し、実行計画（RunPlan）を確定する。

- `InteractiveCli`
  - 対話の全体フロー
  - デフォルト値の提示
  - 実行内容のサマリ表示と最終確認
- `ConsolePrompter`
  - 標準入力/出力を使った質問・入力ユーティリティ
  - `askPath` / `askString` / `askYesNo` / `askChoice` など
- `RuleWizard`
  - 対話でルールを作成する
  - 入力結果を `config` の設定モデルに落とす

### 3.2 `config`

ルール JSON（読み込み/保存）と、その検証を担当する。

- `MaskRulesConfig` / `MaskRuleConfig`
  - JSON スキーマ（設定モデル）
  - 外部入出力に近い形 を維持する
- `RulesRepository`
  - JSON ファイルの読み込み/保存（Jackson）
- `RuleValidator`
  - 設定モデルの検証（必須項目、重複 ID、pattern 妥当性、flags 妥当性、version 等）
- `RuleValidationException`
  - 検証 NG の例外（検証エラーを 1 つの例外型に集約）
- `RuleFlag`
  - flags の定義（例：CASE_INSENSITIVE）
- `MaskRuleCompiler`
  - `config`（MaskRuleConfig）→ `core`（MaskRule）へ変換
  - 正規表現コンパイル（Pattern 化）など 実行可能な形 へ落とす

### 3.3 `core`

マスキングのコアロジック（テキストを受けて置換し、件数を返す）。

- `MaskRule`
  - 実行用ルール（コンパイル済み Pattern を保持）
- `MaskEngine`
  - `maskText(text, rules)` の本体
  - ルールを順に適用し、置換後テキストと件数を返す
- `MaskEngineResult`
  - 置換後テキスト + 件数集計（総件数 / ルール別 など）

### 3.4 `io`

ファイル列挙、パス解決、ファイル入出力、単一ファイル処理など **ファイルレベルの処理** を担当する。

- `InputFileCollector`
  - 入力がファイル/ディレクトリのどちらでも受けられるようにし、対象ファイルを列挙する
- `InputFileRef`
  - 入力ファイルを表す値オブジェクト（入力ルート + 個々のファイルパスなど）
  - 構造維持出力 のために相対パスを扱いやすくする
- `FileMaskingProcessor`
  - 単一ファイルの処理（読み込み → core へ → 書き込み or dryRun）
  - dryRun の場合も **出力予定パス** を計算できる

### 3.5 `service`

**アプリとしての実処理** を統合する層（CLI がここを呼ぶ）。

- `RunPlan`
  - 実行計画（入力/出力/dryRun/サフィックス/ルールソース等）を 1 つにまとめた値
  - CLI で確定し、Service に渡す
- `FileMaskingService`
  - RunPlan を受け取り、次を統合実行する
    - ルール読み込み/検証/compile
    - 入力ファイル列挙
    - ファイル処理（dryRun/通常）
    - 結果集計
    - レポート出力

### 3.6 `report`

実行結果をファイル出力する。

- `ReportWriter`
  - `out/report-YYYYMMDD-HHMMSS-SSS.json` を作成し JSON 出力
  - dryRun の場合も report は出す
- `RunReportJson`
  - report JSON の出力用 DTO（schemaVersion、実行情報、ファイル別・ルール別件数 等）
- `MaskRunReport` / `MaskedFileReport`
  - 実行中に集計した情報を保持する内部モデル
  - 最終的に `RunReportJson` へ変換して出力する

### 3.7 `util`

小さな共通処理。

- `RegexFlagUtil`
  - `RuleFlag` → `Pattern` のフラグ（int）へ変換する共通関数
  - validator/compile の双方で利用し、重複ロジックを避ける
- `PathUtil`
  - suffix 付与、拡張子の扱い、パス生成などを集約

---

## 4. 依存の向き

依存が複雑にならないよう、次の向きを守る。

- `cli` → `service`
- `service` → `config`, `io`, `core`, `report`, `util`
- `config` → `util`（flag 変換など）
- `io` → `core`, `util`
- `report` → `util`（必要なら）
- `core` →（原則）依存しない

ポイント:

- `core` はテキスト処理に閉じ、I/O・対話・JSON に依存しない
- **設定モデル（config）** と **実行モデル（core）** は分け、compiler で変換する

---

## 5. ユースケースの処理フロー

### 5.1 通常実行（ファイル出力あり）

1. `App` 起動
2. `InteractiveCli` が対話を進め、`RunPlan` を作る
3. `FileMaskingService` に `RunPlan` を渡して実行
4. Service 内で以下を順に行う
   - `RulesRepository` でルール JSON 読み込み（または対話で作成された設定を受け取る）
   - `RuleValidator` で検証
   - `MaskRuleCompiler` で core 用ルールへ変換
   - `InputFileCollector` で対象ファイルを列挙（InputFileRef）
   - 各ファイルを `FileMaskingProcessor` で処理
     - 読み込み → `MaskEngine.maskText` → 書き込み
   - 合計件数/ルール別件数/ファイル別件数を集計
   - `ReportWriter` で `out/report-*.json` を出力

### 5.2 dryRun（件数集計のみ）

- 5.1 と同じフローだが、`FileMaskingProcessor` は書き込みを行わない
- **出力予定パス** は計算し、report にも出力する

---

## 6. 例外・エラーの設計方針

- ルール不正は `RuleValidationException` に集約し、早期に失敗させる
- ファイル I/O エラーなどは基本的に例外として上位へ伝播（CLI 側で必要なら最小限の表示）
- report にはログ本文や置換サンプルなど **内容が分かる情報** を入れない

---

## 7. ディレクトリ構成

```txt
.
├─ build.gradle
├─ settings.gradle
├─ gradlew / gradlew.bat
├─ gradle.properties
├─ gradle/wrapper/
│
├─ scripts/
│  ├─ run.sh
│  └─ run.bat
│
├─ rules/
│  ├─ mask-rules.json
│  ├─ mask-rules-basic.json
│  ├─ mask-rules-strict.json
│  └─ mask-rules-projectA.json
│
├─ input/
│  └─ sample/
│     ├─ api/
│     ├─ app/
│     ├─ infra/
│     ├─ app.log
│     └─ quick.log
│
├─ out/
│  └─ .gitkeep
│
└─ src/
   ├─ main/
   │  └─ java/io/github/seiya_matsuoka/interactivelogmaskingcli/
   │     ├─ App.java
   │     ├─ cli/
   │     ├─ config/
   │     ├─ core/
   │     ├─ io/
   │     ├─ report/
   │     ├─ service/
   │     └─ util/
   │
   └─ test/
      ├─ java/io/github/seiya_matsuoka/interactivelogmaskingcli/
      │  ├─ config/
      │  ├─ core/
      │  ├─ io/
      │  ├─ report/
      │  ├─ service/
      │  ├─ util/
      │  └─ SmokeTest.java
      │
      └─ resources/fixtures/rules/
         ├─ valid-mask-rules.json
         └─ invalid-pattern-mask-rules.json
```

---

## 8. 配布物（distZip）の構成（概要）

ユーザーが zip を展開した直後に、サンプルで動作確認できる状態にする

- `bin/`
  - 実行用スクリプト（Unix/Windows）
- `lib/`
  - アプリ本体 jar と依存 jar
- 同梱（本プロジェクト方針）
  - `rules/`（サンプルルール）
  - `input/sample/`（サンプルログ）
  - `out/.gitkeep`（出力先ディレクトリの目印）

---

## 9. テスト方針

- `config`：JSON 読み込み/保存、検証 NG、flag 変換の妥当性
- `core`：ルール順適用、件数集計
- `io`：ファイル列挙、構造維持、suffix 付与、dryRun 時の動作
- `service`：統合（通常/dryRun）
- `report`：report-\*.json の出力内容（最低限）
- `SmokeTest`：テストが通ることの確認用
