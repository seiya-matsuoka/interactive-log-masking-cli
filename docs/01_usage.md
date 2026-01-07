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
