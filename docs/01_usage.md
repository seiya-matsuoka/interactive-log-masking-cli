# 使い方

> このドキュメントは、**interactive-log-masking-cli** の使い方をまとめたもの。

## 1. このツールでできること

ログファイル（またはログディレクトリ）に含まれる情報を、ルールに従ってマスキング（置換）し、`out/` 配下へ出力します。  
dryRun（件数集計のみ）にも対応し、実行結果レポート（`out/report-*.json`）を出力します。

---

## 2. 事前準備

### 2.1 必要なもの

- Java: 21
- 配布物（zip）を使う場合: 追加で Gradle は不要（同梱スクリプトで実行できます）
- リポジトリを直接実行する場合: Gradle

---

## 3. ルールとサンプルの場所

### 3.1 ルール（JSON）

- `rules/` 配下にサンプルルールがあります。
  - `rules/mask-rules.json`（デフォルト想定）
  - `rules/mask-rules-basic.json`
  - `rules/mask-rules-strict.json`
  - `rules/mask-rules-projectA.json`

### 3.2 サンプルログ

- `input/sample/` 配下にサンプルログがあります。
  - `input/sample/api/...`
  - `input/sample/app/...`
  - `input/sample/infra/...`
  - `input/sample/app.log`
  - `input/sample/quick.log`

### 3.3 出力先

- 出力は `out/` 配下です（入力ファイルは変更しません）
- report も `out/` 配下に `report-*.json` として出力されます

---

## 4. 実行方法（リポジトリから実行）

### 4.1 対話型 CLI で起動

表示崩れを避けるため、`--console=plain` を付けて実行します。

```bash
# macOS/Linux (bash/zsh)
./gradlew --console=plain run

# Windows (PowerShell)
.\gradlew --console=plain run

# Windows (cmd)
gradlew --console=plain run
```

実行すると対話が始まり、次を順に入力します（空入力の場合はデフォルト値が採用されます）。

- 入力パス（ファイル or ディレクトリ）
- 出力先ディレクトリ（out 配下）
- dryRun にするか
- サフィックス付与の有無とサフィックス文字列
- ルール指定方法（JSON / 対話作成）

---

## 5. 実行方法（配布物 zip から実行）

### 5.1 zip を展開

`interactive-log-masking-cli-<version>.zip` を任意の場所に展開します。

展開後の中身（イメージ）:

```txt
.
├─ bin/
├─ lib/
├─ rules/
├─ input/sample/
└─ out/
```

### 5.2 起動（bin 配下のスクリプト）

- macOS/Linux:

```bash
./bin/interactive-log-masking-cli
```

- Windows:

```powershell
.\bin\interactive-log-masking-cli.bat
```

※配布物にはアプリ本体 jar と依存 jar が同梱されているため、基本的には Java があれば実行できます。

---

## 6. 使い方（対話の例）

以下は **例** です（デフォルト値は環境や設定で変わる場合があります）。

- 入力パス（ファイル or ディレクトリ）:
  - 例: `input`
- 出力先ディレクトリ（out 配下）:
  - 例: `out`
- dryRun（件数集計のみ。ファイルは生成しない）:
  - `y` なら dryRun（ファイルは出さず、件数だけ集計）
  - `n` なら通常実行（ファイル生成あり）
- 出力ファイル名にサフィックスを付与:
  - `y` ならサフィックスを付けて出力
- サフィックス:
  - 例: `_masked`
- ルール指定方法:
  - 1. JSON 設定ファイル
  - 2. 対話で作成

最後に実行計画が表示されるので、確認して実行します。

---

## 7. dryRun について

dryRun は「変換後ファイルを生成せず、置換件数のみ集計する」モードです。

- 出力ファイルは作りません
- report（`out/report-*.json`）は出力します
- 件数集計は通常実行と同じロジックで行います

---

## 8. 生成されるもの

### 8.1 マスク済みファイル（通常実行のみ）

- `out/` 配下に出力されます
- 入力がディレクトリの場合、相対パス構造を維持して出力します
- サフィックス付与が有効なら、拡張子の直前に付与します

例:

- 入力: `input/sample/app/app-2026-01-05.log`
- 出力: `out/sample/app/app-2026-01-05_masked.log`

### 8.2 実行結果レポート（通常/dryRun 共通）

- `out/report-YYYYMMDD-HHMMSS-SSS.json`
- 主に以下の情報を含みます
  - 対象ファイル数
  - 合計置換件数
  - ルール別件数
  - ファイル別件数（入出力パス・件数）
  - dryRun の有無、suffix、ルールソースなど

---

## 9. 確認ポイント

### 9.1 出力が見当たらない

- dryRun が `true` になっていないか確認してください（dryRun はファイルを生成しません）

### 9.2 ルール JSON がエラーになる

- `pattern` が壊れた正規表現でないか
- `id` が重複していないか
- `flags` に未対応の値が入っていないか

---

## 10. 注意事項

- 本ツールは入力ファイル自体を変更しませんが、運用上は **コピーしたログ** を使うことを推奨します
- `out/` 配下には生成物が溜まっていきます。必要に応じて整理してください
